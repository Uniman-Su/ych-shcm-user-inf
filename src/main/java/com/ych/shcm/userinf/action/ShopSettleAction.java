package com.ych.shcm.userinf.action;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.action.OperatorAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.ShopSettleDate;
import com.ych.shcm.o2o.model.ShopSettleDateSummary;
import com.ych.shcm.o2o.model.ShopSettleDetail;
import com.ych.shcm.o2o.parameter.QueryShopSettleParameter;
import com.ych.shcm.o2o.service.OrderService;
import com.ych.shcm.o2o.service.ShopService;
import com.ych.shcm.o2o.service.ShopSettleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

/**
 * 店铺结算Action
 */
@Controller("shcm.userinf.action.ConsoleShopSettleAction")
public class ShopSettleAction extends OperatorAction {

    @Autowired
    private ShopSettleService shopSettleService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private OrderService orderService;

    /**
     * 查询店铺结算日汇总数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 店铺结算日汇总数据的分页数据
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/shopSettle/queryList")
    public CommonOperationResultWidthData<PagedList<ShopSettleDateSummary>> queryPagedSummaryList(@RequestBody QueryShopSettleParameter parameter) {
        CommonOperationResultWidthData<PagedList<ShopSettleDateSummary>> ret = new CommonOperationResultWidthData<>();
        ret.setData(shopSettleService.getPagedSummaryList(parameter));
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 查询店铺结算日数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 店铺结算日数据的分页数据
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/shopSettle/queryDateList")
    public CommonOperationResultWidthData<PagedList<ShopSettleDate>> queryPagedDateList(@RequestBody QueryShopSettleParameter parameter) {

        CommonOperationResultWidthData<PagedList<ShopSettleDate>> ret = new CommonOperationResultWidthData<>();
        ret.setData(shopSettleService.getPagedDateList(parameter));
        for (ShopSettleDate shopSettleDate : ret.getData().getList()) {
            shopSettleDate.setShop(shopService.getShopById(shopSettleDate.getShopId()));
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 查询店铺结算明细的分页列表
     *
     * @param parameter
     *         查询参数
     * @return 店铺结算明细的分页列表
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/shopSettle/queryDetailList")
    public CommonOperationResultWidthData<PagedList<ShopSettleDetail>> queryPagedDetailList(@RequestBody QueryShopSettleParameter parameter) {
        CommonOperationResultWidthData<PagedList<ShopSettleDetail>> ret = new CommonOperationResultWidthData<>();
        ret.setData(shopSettleService.getPagedDetailList(parameter));
        for (ShopSettleDetail shopSettleDetail : ret.getData().getList()) {
            shopSettleDetail.setOrder(orderService.getById(shopSettleDetail.getOrderId()));
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 结算日数据
     *
     * @return
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/shopSettle/settledDateData")
    public CommonOperationResultWidthData settledDateData(@RequestParam BigDecimal id) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            ret = shopSettleService.settledDate(id);
        } catch (RuntimeException e) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription("更新异常");
        }
        return ret;
    }

    /**
     * 结算汇总数据
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/shopSettle/settledSummaryData")
    public CommonOperationResultWidthData settledSummaryData(@RequestParam BigDecimal id) {

        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            shopSettleService.settledSummary(id);
        } catch (RuntimeException e) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription("更新异常");
        }
        return ret;
    }

}
