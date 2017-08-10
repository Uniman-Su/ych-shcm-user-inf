package com.ych.shcm.userinf.action;

import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.PayChannel;
import com.ych.shcm.o2o.model.PayOrder;
import com.ych.shcm.o2o.service.OrderService;
import com.ych.shcm.o2o.service.PayOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * 支付的入口Action
 */
@RequestMapping("pay")
@Controller("shcm.userinf.action")
public class PayAction {

    /**
     * 预处理中存放的订单
     */
    public static final String ORDER_ATTR_NAME = "Order";

    /**
     * 预处理中存放的支付单
     */
    public static final String PAY_ORDER_ATTR_NAME = "PayOrder";

    @Autowired
    private OrderService orderService;

    @Autowired
    private PayOrderService payOrderService;

    /**
     * 支付订单
     *
     * @param orderNo
     *         订单号
     * @param payChannel
     *         支付渠道
     * @return Forward实际的支付处理
     */
    @RequestMapping("payOrder")
    public String entrance(@RequestParam("orderNo") String orderNo, @RequestParam("payChannel") PayChannel payChannel) {
        switch (payChannel) {
            case WXPAY:
                return "forward:/wxpay/payByJSAPI";

            default:
                return "";
        }
    }

    /**
     * 退款
     *
     * @param orderNo
     *         订单号
     * @param request
     *         支付单
     * @return Forward实际的支付处理
     */
    @RequestMapping("refund")
    public String refund(@RequestParam("orderNo") String orderNo, HttpServletRequest request) {
        Order order = orderService.getByNo(orderNo);
        PayOrder payOrder = payOrderService.getPayedByOrderId(order.getId());

        request.setAttribute(ORDER_ATTR_NAME, order);
        request.setAttribute(PAY_ORDER_ATTR_NAME, payOrder);

        switch (payOrder.getPayChannel()) {
            case WXPAY:
                return "forward:/wxpay/refund";

            default:
                return "";
        }
    }

}
