package com.ych.shcm.userinf.action;

import com.ych.core.model.BaseWithCommonOperationResult;
import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.core.model.SortOrder;
import com.ych.shcm.o2o.action.CompositeAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.CarBrand;
import com.ych.shcm.o2o.model.CarFactory;
import com.ych.shcm.o2o.model.CarModel;
import com.ych.shcm.o2o.model.CarSeries;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.OrderEvaluation;
import com.ych.shcm.o2o.model.Shop;
import com.ych.shcm.o2o.model.ShopImage;
import com.ych.shcm.o2o.parameter.QueryOrderAppointmentListParameter;
import com.ych.shcm.o2o.parameter.QueryShopParameter;
import com.ych.shcm.o2o.service.CarModelService;
import com.ych.shcm.o2o.service.CarService;
import com.ych.shcm.o2o.service.OrderService;
import com.ych.shcm.o2o.service.ShopService;
import com.ych.shcm.o2o.service.UploadService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * 店铺基本信息获取相关的Action
 *
 * @author U
 */
@Controller("shcm.userinf.action.baseinfo.ShopAction")
public class ShopAction extends CompositeAction {

    @Autowired
    private ShopService shopService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CarService carService;

    @Autowired
    private CarModelService carModelService;
    @Autowired
    private UploadService uploadService;
    @Autowired
    private MessageSource messageSource;

    /**
     * 查询店铺信息
     *
     * @param id
     *         店铺id
     * @return 结果
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @RequestMapping("shop/queryShop")
    @ResponseBody
    public CommonOperationResultWidthData<Shop> queryShopByShopId(@RequestParam BigDecimal id) {
        CommonOperationResultWidthData<Shop> ret = new CommonOperationResultWidthData<>();
        ret.setData(shopService.getShopById(id));
        ret.getData().setImagePath(uploadService.getFileUrl(ret.getData().getImagePath()));
        for (ShopImage shopImage : ret.getData().getImages()) {
            shopImage.setImagePath(uploadService.getFileUrl(shopImage.getImagePath()));
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 取得订单可服务的门店
     *
     * @param parameter
     *         订单Id
     * @return 返回的列表
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/shop/queryCanServiceShopList")
    @ResponseBody
    public CommonOperationResultWidthData<PagedList<Shop>> queryCanServiceShopList(@RequestBody QueryShopParameter parameter) {
        CommonOperationResultWidthData<PagedList<Shop>> ret = new CommonOperationResultWidthData<>();
        Order order = orderService.getById(parameter.getOrderId());
        Car car = carService.getCarById(order.getCarId());
        CarModel carModel = carModelService.getModelById(car.getModelId());
        carModel.setSeries(carModelService.getSeriesById(carModel.getSeriesId()));
        parameter.setBrandId(carModel.getSeries().getBrandId());
        if (parameter.getPosition() != null) {
            parameter.setSort("distance");
            parameter.setOrder(SortOrder.asc);
        }
        ret.setData(shopService.getPagedList(parameter));
        for (Shop shop : ret.getData().getList()) {
            shop.setImagePath(uploadService.getFileUrl(shop.getImagePath()));
        }
        ret.setResult(CommonOperationResult.Succeeded);

        return ret;

    }

    /**
     * 取得门店评价列表
     *
     * @param parameter
     *         门店ID
     * @return 返回的列表
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/shop/queryEvaluationsOfShop")
    @ResponseBody
    public CommonOperationResultWidthData<PagedList<OrderEvaluation>> queryEvaluationsOfShop(@RequestBody QueryOrderAppointmentListParameter parameter) {
        CommonOperationResultWidthData<PagedList<OrderEvaluation>> ret = new CommonOperationResultWidthData<>();
        PagedList<OrderEvaluation> list = orderService.getOrderEvaluationList(parameter);
        ret.setData(list);
        for (OrderEvaluation orderEvaluation : list.getList()) {
            Car car = carService.getCarById(orderEvaluation.getOrder().getCarId());

            CarModel carModel = carModelService.getModelById(car.getModelId());
            CarSeries carSeries = carModelService.getSeriesById(carModel.getSeriesId());
            CarFactory carFactory = carModelService.getFactoryById(carSeries.getFactoryId());
            CarBrand carBrand = carModelService.getBrandById(carSeries.getBrandId());

            carModel.setSeries(carSeries);
            carSeries.setCarFactory(carFactory);
            carSeries.setCarBrand(carBrand);
            car.setCarModel(carModel);
            orderEvaluation.getOrder().setCar(car);
            carBrand.setLogoPath(uploadService.getFileUrl(carBrand.getLogoPath()));
        }

        return ret;
    }

    /**
     * 门店端确认服务
     *
     * @param orderId
     *         订单Id
     * @param mileage
     *         里程数
     * @return 结果
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("shop/confirmService")
    @ResponseBody
    public CommonOperationResultWidthData confirmService(@RequestParam BigDecimal orderId, @RequestParam Long mileage) {

        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        List<Shop> shops = shopService.getByUserId(getUser().getId());
        if (CollectionUtils.isEmpty(shops)) {
            ret.setResult(CommonOperationResult.IllegalOperation);
            ret.setDescription(messageSource.getMessage("user.shop.notBinded", null, Locale.getDefault()));
            return ret;
        }
        //QueryOrderAppointmentListParameter parameter = new QueryOrderAppointmentListParameter();
        //parameter.setOrderId(orderId);
        //parameter.setShopId(shops.get(0).getId());
        //parameter.setPageIndex(0);
        //parameter.setPageSize(Integer.MAX_VALUE);
        //List<OrderAppointment> list = orderService.getAppointmentList(parameter).getList();
        //if (CollectionUtils.isEmpty(list)) {
        //    ret.setResult(CommonOperationResult.IllegalOperation);
        //    ret.setDescription("此订单未预约");
        //    return ret;
        //}
        return orderService.serviceTheOrder(orderId, mileage, shops.get(0).getId());
    }

    /**
     * 修改店铺的坐标
     *
     * @param name
     *         店铺名称
     * @param longitude
     *         经度
     * @param latitude
     *         纬度
     * @return 操作结果
     */
    @JWTAuth(issuer = Constants.WEB_CONSOLE_OPERATOR_ISSUER)
    @RequestMapping("shop/modifyLocation")
    @ResponseBody
    public BaseWithCommonOperationResult modifyLocation(String name, double longitude, double latitude) {
        return shopService.modifyLocation(name, longitude, latitude, getOperator().getId());
    }

}
