package com.ych.shcm.userinf.action;

import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.ych.core.model.BaseWithCommonOperationResult;
import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.SystemParameterHolder;
import com.ych.core.pay.ChannelOperationResult;
import com.ych.core.pay.CreatePayOrderParameterImpl;
import com.ych.core.wechat.pay.ErrorCode;
import com.ych.core.wechat.pay.TradeType;
import com.ych.core.wechat.pay.WXPayUtils;
import com.ych.core.wechat.pay.WXPaymentStrategy;
import com.ych.core.wechat.pay.message.PayNotify;
import com.ych.core.wechat.pay.message.ReturnResult;
import com.ych.shcm.o2o.action.UserAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.service.OrderService;
import com.ych.shcm.o2o.service.PayOrderService;
import com.ych.shcm.o2o.service.systemparamholder.WXMPAppID;
import com.ych.shcm.o2o.service.systemparamholder.WXPayMerchantName;
import com.ych.shcm.o2o.service.systemparamholder.WXPayNotifyUrl;

/**
 * 微信支付相关的Action
 */
@RequestMapping("wxpay")
@Controller("shcm.userinf.action.WXPayAction")
public class WXPayAction extends UserAction {

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private WXPayUtils wxPayUtils;

    @Autowired
    private MessageSource messageSource;

    @Resource(name = WXMPAppID.NAME)
    private SystemParameterHolder wxAppId;

    @Resource(name = WXPayMerchantName.NAME)
    private SystemParameterHolder wxPayMerchantName;

    @Resource(name = WXPayNotifyUrl.NAME)
    private SystemParameterHolder wxPayNotifyUrl;

    /**
     * 微信公众号JSAPI调用支付
     *
     * @param orderNo
     *         订单号
     * @param payChannel
     *         支付渠道
     * @return JSAPI支付时传递的参数对象
     */
    @JWTAuth(Constants.WECHAT_MP_ISSUER)
    @RequestMapping("payByJSAPI")
    @ResponseBody
    public CommonOperationResultWidthData<Map<String, Object>> createPayOrder(@RequestParam("orderNo") String orderNo, @RequestParam("payChannel") PayChannel payChannel) {
        CommonOperationResultWidthData<Map<String, Object>> ret = new CommonOperationResultWidthData<>();

        Order order = orderService.getByNo(orderNo);
        if (order == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(messageSource.getMessage("order.orderNo.notExists", null, Locale.getDefault()));
            return ret;
        }

        if (!getUser().getId().equals(order.getUserId())) {
            ret.setResult(CommonOperationResult.IllegalAccess);
            ret.setDescription(messageSource.getMessage("system.common.illegalAccess", null, Locale.getDefault()));
            return ret;
        }

        CreatePayOrderParameterImpl createParameter = new CreatePayOrderParameterImpl();
        createParameter.setChannelName(payChannel.name());
        createParameter.setFlowNo(orderNo);
        createParameter.setTotalMoney(order.getMoney());

        List<OrderServicePack> packs = orderService.getPackByOrderId(order.getId(), false);
        createParameter.setOrderSubject(wxPayMerchantName.getStringValue() + "-" + packs.get(0).getName());

        Map<String, Object> extendedParamters = new HashMap<>();
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_FEE_TYPE, "CNY");
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_TERMINAL_IP, getRemoteAddr());
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_NOTIFY_URL, wxPayNotifyUrl.getStringValue());
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_TRADE_TYPE, TradeType.JSAPI);
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_OPEN_ID, getAuthorizeResult().getOpenId());

        CommonOperationResultWidthData<ChannelOperationResult> createResult = payOrderService.createPayOrder(createParameter, extendedParamters);
        if (createResult.getResult() != CommonOperationResult.Succeeded) {
            ret.setResult(createResult.getResult());
            ret.setDescription(createResult.getDescription());
            return ret;
        }

        ret.setResult(CommonOperationResult.Succeeded);

        long signTimestamp = System.currentTimeMillis() / 1000;
        String signTimestampStr = String.valueOf(signTimestamp);
        String signNonceStr = wxPayUtils.getNonceStr();
        String packageStr = "prepay_id=" + createResult.getData().getPrepayChannelFlowNo();
        TreeMap<String, String> signData = new TreeMap<>();
        signData.put("appId", wxAppId.getStringValue());
        signData.put("nonceStr", signNonceStr);
        signData.put("package", packageStr);
        signData.put("signType", com.ych.core.wechat.pay.Constants.SIGN_TYPE_MD5);
        signData.put("timeStamp", signTimestampStr);
        String sign = wxPayUtils.getSign(signData, com.ych.core.wechat.pay.Constants.SIGN_TYPE_MD5);


        HashMap<String, Object> data = new HashMap<>();
        data.put("nonceStr", signNonceStr);
        data.put("package", packageStr);
        data.put("signType", com.ych.core.wechat.pay.Constants.SIGN_TYPE_MD5);
        data.put("timestamp", signTimestamp);
        data.put("paySign", sign);
        ret.setData(data);

        return ret;
    }

    /**
     * 对支付通知进行验签
     *
     * @param payNotify
     *         支付通知
     * @return 验签结果
     */
    @RequestMapping("payNotify")
    @ResponseBody
    public ReturnResult validatePayNotifySign(@RequestBody PayNotify payNotify) {
        ReturnResult validateResult = wxPayUtils.validatePayNotifySign(payNotify);

        if (validateResult.getReturnCode().equals(ErrorCode.SUCCESS)) {
            String flowNo = payNotify.getOutTradeNo();
            String channelFlowNo = payNotify.getTransactionId();
            boolean success;
            String errorCode;
            String errorDesc;

            if (payNotify.getReturnCode().equals(ErrorCode.SUCCESS)) {
                if (payNotify.getResultCode().equals(ErrorCode.SUCCESS)) {
                    success = true;
                    errorCode = payNotify.getResultCode();
                    errorDesc = payNotify.getReturnMsg();
                } else {
                    success = false;
                    errorCode = payNotify.getErrorCode();
                    errorDesc = payNotify.getErrorCodeDesc();
                }
            } else {
                success = false;
                errorCode = payNotify.getResultCode();
                errorDesc = payNotify.getReturnMsg();
            }


            BaseWithCommonOperationResult opResult = payOrderService.payNotified(flowNo, channelFlowNo, payNotify.getPayTime(), success, errorCode, errorDesc);

            if (opResult.getResult() != CommonOperationResult.Succeeded) {
                validateResult = new ReturnResult();
                validateResult.setReturnCode(ErrorCode.FAIL);
                validateResult.setReturnMsg("Handle notify result failed");
            }
        }

        return validateResult;
    }

    /**
     * 申请退款操作
     *
     * @param order
     *         订单
     * @param payOrder
     *         支付单
     * @return 操作结果
     */
    @JWTAuth(Constants.WECHAT_MP_ISSUER)
    @RequestMapping("refund")
    @ResponseBody
    public BaseWithCommonOperationResult refund(@RequestAttribute(PayAction.ORDER_ATTR_NAME) Order order, @RequestAttribute(PayAction.PAY_ORDER_ATTR_NAME) PayOrder payOrder) {
        BaseWithCommonOperationResult ret = new BaseWithCommonOperationResult();

        if (!getUser().getId().equals(order.getUserId())) {
            ret.setResult(CommonOperationResult.IllegalAccess);
            ret.setDescription(messageSource.getMessage("system.common.illegalAccess", null, Locale.getDefault()));
            return ret;
        }

        Map<String, Object> extendedParamters = new HashMap<>();
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_TOTAL_FEE, payOrder.getPrice());
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_REFUND_FEE, order.getMoney());
        extendedParamters.put(WXPaymentStrategy.EXTPARAM_REFUND_FEE_TYPE, "CNY");

        CommonOperationResultWidthData<ChannelOperationResult> refundResult = payOrderService.refund(order.getOrderNo(), extendedParamters);
        if (refundResult.getResult() == CommonOperationResult.Succeeded) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(refundResult.getResult());
            ret.setDescription(refundResult.getDescription());
        }

        return ret;
    }

}
