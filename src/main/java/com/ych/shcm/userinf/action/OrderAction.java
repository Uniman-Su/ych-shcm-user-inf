package com.ych.shcm.userinf.action;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.action.UserAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.OrderAppointment;
import com.ych.shcm.o2o.model.OrderBill;
import com.ych.shcm.o2o.model.OrderEvaluation;
import com.ych.shcm.o2o.model.OrderServicePack;
import com.ych.shcm.o2o.model.OrderServicePackItem;
import com.ych.shcm.o2o.model.OrderStatusCount;
import com.ych.shcm.o2o.model.ServicePack;
import com.ych.shcm.o2o.model.Shop;
import com.ych.shcm.o2o.model.User;
import com.ych.shcm.o2o.parameter.QueryOrderAppointmentListParameter;
import com.ych.shcm.o2o.parameter.QueryOrderListParameter;
import com.ych.shcm.o2o.service.CarModelService;
import com.ych.shcm.o2o.service.CarService;
import com.ych.shcm.o2o.service.OrderService;
import com.ych.shcm.o2o.service.ShopService;
import com.ych.shcm.o2o.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单接口
 */
@Controller("shcm.userinf.action.OrderAction")
public class OrderAction extends UserAction {

    @Autowired
    OrderService orderService;

    @Autowired
    private CarService carService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private CarModelService carModelService;

    @Autowired
    private UploadService uploadService;

    /**
     * 用户订单数量接口
     *
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @ResponseBody
    @RequestMapping("wxmp/order/myOrderStatusCount")
    public CommonOperationResultWidthData queryOrderNumGroupByType() {

        CommonOperationResultWidthData<List<OrderStatusCount>> ret = new CommonOperationResultWidthData();
        ret.setData(orderService.getOrderCountGroupByType(getUser().getId()));
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 创建订单
     *
     * @param createOrderRequest
     * @return 结果
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @ResponseBody
    @RequestMapping("wxmp/order/createOrder")
    public CommonOperationResultWidthData<Map<String, Object>> createOrder(@RequestBody CreateOrderRequestBody createOrderRequest) {
        CommonOperationResultWidthData<Map<String, Object>> ret = new CommonOperationResultWidthData();
        User user = getUser();
        try {
            Assert.notNull(createOrderRequest.getCarId(), "车辆id不能为空");
            Assert.notEmpty(createOrderRequest.getPacks(), "订单服务包不能为空");
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        Order order = new Order();
        order.setAccessChannelId(BigDecimal.ONE);
        order.setUserId(user.getId());
        order.setCarId(createOrderRequest.getCarId());
        List<OrderServicePack> orderServicePacks = new ArrayList<>();
        for (ServicePack servicePack : createOrderRequest.getPacks()) {
            OrderServicePack orderServicePack = new OrderServicePack();
            orderServicePacks.add(orderServicePack);
            orderServicePack.setServicePackId(servicePack.getId());
        }
        order.setOrderServicePacks(orderServicePacks);
        return orderService.createOrder(order);
    }

    /**
     * 订单创建请求体
     */
    public static class CreateOrderRequestBody {

        BigDecimal carId;

        List<ServicePack> packs;

        /**
         * Getter for property 'carId'.
         *
         * @return Value for property 'carId'.
         */
        public BigDecimal getCarId() {
            return carId;
        }

        /**
         * Setter for property 'carId'.
         *
         * @param carId
         *         Value to set for property 'carId'.
         */
        public void setCarId(BigDecimal carId) {
            this.carId = carId;
        }

        /**
         * Getter for property 'packs'.
         *
         * @return Value for property 'packs'.
         */
        public List<ServicePack> getPacks() {
            return packs;
        }

        /**
         * Setter for property 'packs'.
         *
         * @param packs
         *         Value to set for property 'packs'.
         */
        public void setPacks(List<ServicePack> packs) {
            this.packs = packs;
        }
    }

    /**
     * 分页获取订单列表/wx端保养维修记录
     *
     * @param parameter
     *         查询条件
     * @return 分页列表
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @ResponseBody
    @RequestMapping({"console/order/queryList", "wxmp/order/queryList", "sone/order/queryList"})
    public CommonOperationResultWidthData<PagedList<Order>> queryOrderList(@RequestBody QueryOrderListParameter parameter, HttpServletRequest request) {

        CommonOperationResultWidthData<PagedList<Order>> ret = new CommonOperationResultWidthData();
        if (request.getServletPath().contains("wxmp/order/queryList")) {
            parameter.setNeedPacks(true);
            ret.setData(orderService.getOrderPageList(parameter));
        } else if (request.getServletPath().contains("sone/order/queryList")) {
            List<Shop> shops = shopService.getByUserId(getUser().getId());
            if (CollectionUtils.isEmpty(shops)) {
                ret.setResult(CommonOperationResult.IllegalOperation);
                ret.setDescription("用户暂无门店");
                return ret;
            }
            parameter.setShopId(shops.get(0).getId());
            ret.setData(orderService.getOrderPageList(parameter));
            for (Order order : ret.getData().getList()) {
                order.setCar(carService.getCarById(order.getCarId()));
                order.getCar().setCarModel(carModelService.getModelById(order.getCar().getModelId()));
                order.getCar().getCarModel().setSeries(carModelService.getSeriesById(order.getCar().getCarModel().getSeriesId()));
                order.getCar().getCarModel().getSeries().setCarBrand(carModelService.getBrandById(order.getCar().getCarModel().getSeries().getBrandId()));
                order.getCar().getCarModel().getSeries().setCarFactory(carModelService.getFactoryById(order.getCar().getCarModel().getSeries().getFactoryId()));
            }
        } else {
            ret.setResult(CommonOperationResult.Succeeded);
            ret.setData(orderService.getOrderPageList(parameter));
            for (Order order : ret.getData().getList()) {
                order.setCar(carService.getCarById(order.getCarId()));
                if (order.getShopId() != null) {
                    order.setShop(shopService.getShopById(order.getShopId()));
                }
                order.setOrderBill(orderService.getOrderBillByOrderId(order.getId()));
            }
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 取得订单详情
     *
     * @param orderId
     *         订单ID
     * @return 分页列表
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @ResponseBody
    @RequestMapping({"console/order/orderDetail", "wxmp/order/orderDetail", "sone/order/orderDetail"})
    public CommonOperationResultWidthData<Order> orderDetail(@RequestParam BigDecimal orderId) {
        CommonOperationResultWidthData<Order> ret = new CommonOperationResultWidthData();
        if (orderId == null) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription("订单不能ID为空");
            return ret;
        }
        Order order = orderService.getById(orderId);
        if (order == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription("订单不存在");
            return ret;
        }

        ret.setData(order);
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 查询订单发票信息
     *
     * @param orderId
     *         订单id
     * @return 结果
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @RequestMapping("order/queryOrderBill")
    @ResponseBody
    public CommonOperationResultWidthData<OrderBill> queryOrderBillByOrderId(@RequestParam BigDecimal orderId) {
        CommonOperationResultWidthData<OrderBill> ret = new CommonOperationResultWidthData();
        try {
            Assert.notNull(orderId, "订单id不能为空");
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        OrderBill orderBill = orderService.getOrderBillByOrderId(orderId);
        ret.setData(orderBill);
        if (orderBill == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription("订单开票信息不存在");
        } else {
            ret.setResult(CommonOperationResult.Succeeded);
        }
        return ret;
    }

    /**
     * 查询评价信息
     *
     * @param orderId
     *         订单id
     * @return 结果
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @RequestMapping("order/queryOrderEvaluation")
    @ResponseBody
    public CommonOperationResultWidthData<OrderEvaluation> queryOrderEvaluationByOrderId(@RequestParam("orderId") BigDecimal orderId) {
        CommonOperationResultWidthData<OrderEvaluation> ret = new CommonOperationResultWidthData<>();
        ret.setData(orderService.getOrderEvaluationByOrderId(orderId));
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 取得订单下的服务
     *
     * @param orderId
     *         订单Id
     * @param needItems
     *         是否需要填充orderServiceItem
     * @return 服务项目
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @RequestMapping("order/queryServicePack")
    @ResponseBody
    public CommonOperationResultWidthData<List<OrderServicePack>> queryServicePackByOrderId(@RequestParam BigDecimal orderId, @RequestParam boolean needItems) {
        CommonOperationResultWidthData<List<OrderServicePack>> ret = new CommonOperationResultWidthData<>();
        ret.setData(orderService.getPackByOrderId(orderId, needItems));
        for (OrderServicePack orderServicePack : ret.getData()) {
            for (OrderServicePackItem item : orderServicePack.getOrderServicePackItems()) {
                item.setIconPath(uploadService.getFileUrl(item.getIconPath()));
            }
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 取消订单
     *
     * @param orderId
     *         订单Id
     * @return 结果
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/order/cancel")
    @ResponseBody
    public CommonOperationResultWidthData cancelOrder(@RequestParam BigDecimal orderId) {
        return orderService.cancelOrder(orderId);

    }

    /**
     * 确认服务
     *
     * @param orderId
     *         订单Id
     * @return 结果
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/order/confirmService")
    @ResponseBody
    public CommonOperationResultWidthData confirmService(@RequestParam BigDecimal orderId) {
        return orderService.confirmService(orderId);
    }

    /**
     * 申请开票
     *
     * @param orderBill
     *         开票申请
     * @return 结果
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/order/applyBill")
    @ResponseBody
    public CommonOperationResultWidthData applyBill(@RequestBody OrderBill orderBill) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        try {
            Assert.notNull(orderBill, "开票申请不能为空");
            Assert.notNull(orderBill.getOrderId(), "订单id不能为空");
            Assert.hasLength(orderBill.getBank(), "开户银行不能为空");
            Assert.hasLength(orderBill.getBankAccount(), "银行账户不能为空");
            Assert.hasLength(orderBill.getCompany(), "公司名不能为空");
            Assert.hasLength(orderBill.getCompanyAddr(), "注册地址不能为空");
            //Assert.hasLength(orderBill.getCompanyPhone(), "公司电话不能为空");
            Assert.hasLength(orderBill.getDeliverAddr(), "配送地址不能为空");
            Assert.hasLength(orderBill.getTaxNo(), "纳税人识别代码不能为空");
            Assert.hasLength(orderBill.getPtc(), "联系人不能为空");
            Assert.hasLength(orderBill.getPhone(), "联系电话不能为空");
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        return orderService.addOrderBill(orderBill);
    }

    /**
     * 评价
     *
     * @param orderEvaluation
     * @return 结果
     * 订单Id
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping({"wxmp/order/evaluateOrder"})
    @ResponseBody
    public CommonOperationResultWidthData evaluateOrder(@RequestBody OrderEvaluation orderEvaluation) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        try {
            Assert.notNull(orderEvaluation, "评价不能为空");
            Assert.notNull(orderEvaluation.getOrderId(), "订单id不能为空");
            Assert.notNull(orderEvaluation.getSkill(), "技术能力评价不能为空");
            Assert.notNull(orderEvaluation.getAttitude(), "服务态度不能为空");
            Assert.notNull(orderEvaluation.getEfficiency(), "服务效率不能为空");
            Assert.notNull(orderEvaluation.getEnvironment(), "店面环境不能为空");
            Assert.notNull(orderEvaluation.getOverallEvaluation(), "总体评价不能为空");

        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
//        orderEvaluation.setAverage(orderEvaluation.getAttitude().add(orderEvaluation.getEfficiency()).add(orderEvaluation.getEnvironment()).divide(new BigDecimal(3),1,BigDecimal.ROUND_HALF_UP));
        orderEvaluation.setAverage(new BigDecimal(orderEvaluation.getAttitude().add(orderEvaluation.getEfficiency()).add(orderEvaluation.getEnvironment()).add(orderEvaluation.getSkill()).doubleValue() / 4));

        return orderService.evaluateOrder(orderEvaluation);
    }

    /**
     * 门店预约
     *
     * @param orderAppointment
     *         预约实体
     * @return 结果
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/order/appointmentOrder")
    @ResponseBody
    public CommonOperationResultWidthData appointmentOrder(@RequestBody OrderAppointment orderAppointment) {
        return orderService.addAppointment(orderAppointment);
    }

    /**
     * 查询订单预约
     *
     * @param parameter
     *         查询条件
     * @return 结果
     */
    @JWTAuth(Constants.WECHAT_MP_ISSUER)
    @RequestMapping({"wxmp/order/queryOrderAppointmentsOfShop", "wxmp/order/queryOrderAppointmentsOfUser"})
    @ResponseBody
    public CommonOperationResultWidthData<PagedList<OrderAppointment>> queryOrderAppointments(HttpServletRequest request, @RequestBody QueryOrderAppointmentListParameter parameter) {
        CommonOperationResultWidthData<PagedList<OrderAppointment>> ret = new CommonOperationResultWidthData();

        if (request.getServletPath().contains("wxmp/order/queryOrderAppointmentsOfUser")) {
            parameter.setUserId(getUser().getId());
        }
        ret.setData(orderService.getAppointmentList(parameter));
        ret.setResult(CommonOperationResult.Succeeded);
        if (parameter.isNeedOrderInfo()) {
            for (OrderAppointment orderAppointment : ret.getData().getList()) {
                orderAppointment.getOrder().setCar(carService.getCarById(orderAppointment.getOrder().getCarId()));
                orderAppointment.getOrder().getCar().setCarModel(carModelService.getModelById(orderAppointment.getOrder().getCar().getModelId()));
                orderAppointment.getOrder().getCar().getCarModel().setSeries(carModelService.getSeriesById(orderAppointment.getOrder().getCar().getCarModel().getSeriesId()));
                orderAppointment.getOrder().getCar().getCarModel().getSeries().setCarBrand(carModelService.getBrandById(orderAppointment.getOrder().getCar().getCarModel().getSeries().getBrandId()));
                orderAppointment.getOrder().getCar().getCarModel().getSeries().setCarFactory(carModelService.getFactoryById(orderAppointment.getOrder().getCar().getCarModel().getSeries().getFactoryId()));
            }
        }
        if (request.getServletPath().contains("wxmp/order/queryOrderAppointmentsOfUser")) {
            for (OrderAppointment orderAppointment : ret.getData().getList()) {
                orderAppointment.setShop(shopService.getShopById(orderAppointment.getShopId()));
            }
        }
        return ret;
    }

    /**
     * 查询门店订单预约详情
     *
     * @param id
     *         预约id
     * @return 结果
     */
    @JWTAuth(Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/order/orderAppointmentsDetail")
    @ResponseBody
    public CommonOperationResultWidthData<OrderAppointment> queryOrderAppointmentsDetail(@RequestParam BigDecimal id) {
        CommonOperationResultWidthData<OrderAppointment> ret = new CommonOperationResultWidthData();
        ret.setResult(CommonOperationResult.Succeeded);
        ret.setData(orderService.getOrderAppointmentById(id));
        return ret;
    }
}
