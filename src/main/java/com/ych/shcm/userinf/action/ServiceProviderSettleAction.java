package com.ych.shcm.userinf.action;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.action.OperatorAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.ServiceProviderSettleDate;
import com.ych.shcm.o2o.model.ServiceProviderSettleDateSummary;
import com.ych.shcm.o2o.model.ServiceProviderSettleDetail;
import com.ych.shcm.o2o.parameter.QueryServiceProviderSettleParameter;
import com.ych.shcm.o2o.service.OrderService;
import com.ych.shcm.o2o.service.ServiceProviderService;
import com.ych.shcm.o2o.service.ServiceProviderSettleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

/**
 * 服务商结算Action
 */
@Controller("shcm.userinf.action.ConsoleServiceProviderSettleAction")
public class ServiceProviderSettleAction extends OperatorAction {

    @Autowired
    private ServiceProviderSettleService serviceProviderSettleService;

    @Autowired
    private ServiceProviderService serviceProviderService;

    @Autowired
    private OrderService orderService;

    /**
     * 查询服务商结算日汇总数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 服务商结算日汇总数据的分页数据
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/serviceProviderSettle/queryList")
    public CommonOperationResultWidthData<PagedList<ServiceProviderSettleDateSummary>> queryPagedSummaryList(@RequestBody QueryServiceProviderSettleParameter parameter) {
        CommonOperationResultWidthData<PagedList<ServiceProviderSettleDateSummary>> ret = new CommonOperationResultWidthData<>();
        ret.setData(serviceProviderSettleService.getPagedSummaryList(parameter));
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 查询服务商结算日数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 服务商结算日数据的分页数据
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/serviceProviderSettle/queryDateList")
    public CommonOperationResultWidthData<PagedList<ServiceProviderSettleDate>> queryPagedDateList(@RequestBody QueryServiceProviderSettleParameter parameter) {

        CommonOperationResultWidthData<PagedList<ServiceProviderSettleDate>> ret = new CommonOperationResultWidthData<>();
        ret.setData(serviceProviderSettleService.getPagedDateList(parameter));
        for (ServiceProviderSettleDate serviceProviderSettleDate : ret.getData().getList()) {
            serviceProviderSettleDate.setServiceProvider(serviceProviderService.getById(serviceProviderSettleDate.getServiceProviderId()));
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 查询服务商结算明细的分页列表
     *
     * @param parameter
     *         查询参数
     * @return 服务商结算明细的分页列表
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER})
    @ResponseBody
    @RequestMapping("console/serviceProviderSettle/queryDetailList")
    public CommonOperationResultWidthData<PagedList<ServiceProviderSettleDetail>> queryPagedDetailList(@RequestBody QueryServiceProviderSettleParameter parameter) {
        CommonOperationResultWidthData<PagedList<ServiceProviderSettleDetail>> ret = new CommonOperationResultWidthData<>();
        ret.setData(serviceProviderSettleService.getPagedDetailList(parameter));
        for (ServiceProviderSettleDetail serviceProviderSettleDetail : ret.getData().getList()) {
            serviceProviderSettleDetail.setOrder(orderService.getById(serviceProviderSettleDetail.getOrderId()));
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
    @RequestMapping("console/serviceProviderSettle/settledDateData")
    public CommonOperationResultWidthData settledDateData(@RequestParam BigDecimal id) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            ret = serviceProviderSettleService.settledDate(id);
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
    @RequestMapping("console/serviceProviderSettle/settledSummaryData")
    public CommonOperationResultWidthData settledSummaryData(@RequestParam BigDecimal id) {

        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            serviceProviderSettleService.settledSummary(id);
        } catch (RuntimeException e) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription("更新异常");
        }
        return ret;
    }

}
