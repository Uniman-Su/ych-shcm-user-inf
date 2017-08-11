package com.ych.shcm.userinf.action;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.action.UserAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.interceptor.JWTAuthInterceptor;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.User;
import com.ych.shcm.o2o.model.UserCar;
import com.ych.shcm.o2o.parameter.QueryCarParameter;
import com.ych.shcm.o2o.service.CarModelService;
import com.ych.shcm.o2o.service.CarService;
import com.ych.shcm.o2o.service.JWTService;
import com.ych.shcm.o2o.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 车辆接口
 */
@Controller("shcm.userinf.action.CarAction")
public class CarAction extends UserAction {

    @Autowired
    private CarService carService;

    @Autowired
    private CarModelService carModelService;

    @Autowired
    private JWTService jwtService;
    @Autowired
    private UploadService uploadService;
    @Autowired
    private MessageSource messageSource;

    /**
     * 取得车辆列表
     *
     * @param parameter
     *         取得参数
     * @return 分页数据
     */
    @JWTAuth(issuer = Constants.WEB_CONSOLE_OPERATOR_ISSUER)
    @ResponseBody
    @RequestMapping("console/car/queryList")
    public CommonOperationResultWidthData<PagedList<Car>> queryCarList(@RequestBody QueryCarParameter parameter) {

        CommonOperationResultWidthData<PagedList<Car>> ret = new CommonOperationResultWidthData();
        ret.setData(carService.getCarList(parameter));
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 查询车辆信息
     *
     * @param id
     *         车辆Id
     * @param isComplete
     *         是否需要完整的车辆信息
     * @return 结果
     */
    @JWTAuth(issuer = {Constants.WEB_CONSOLE_OPERATOR_ISSUER, Constants.WECHAT_MP_ISSUER})
    @RequestMapping("car/queryCar")
    @ResponseBody
    public CommonOperationResultWidthData<Car> queryCarByCarId(@RequestParam BigDecimal id, boolean isComplete) {
        CommonOperationResultWidthData<Car> ret = new CommonOperationResultWidthData<>();
        ret.setData(carService.getCarById(id));
        if (isComplete) {
            ret.getData().setCarModel(carModelService.getModelById(ret.getData().getModelId()));
            ret.getData().getCarModel().setSeries(carModelService.getSeriesById(ret.getData().getCarModel().getSeriesId()));
            ret.getData().getCarModel().getSeries().setCarBrand(carModelService.getBrandById(ret.getData().getCarModel().getSeries().getBrandId()));
            ret.getData().getCarModel().getSeries().setCarFactory(carModelService.getFactoryById(ret.getData().getCarModel().getSeries().getFactoryId()));
            ret.getData().getCarModel().getSeries().getCarBrand().setLogoPath(uploadService.getFileUrl(ret.getData().getCarModel().getSeries().getCarBrand().getLogoPath()));

        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 取得用户车辆列表
     *
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/car/myCar")
    @ResponseBody
    public CommonOperationResultWidthData<List<UserCar>> queryMyCar() {
        CommonOperationResultWidthData<List<UserCar>> ret = new CommonOperationResultWidthData<>();
        User user = getUser();
        List<UserCar> list = carService.getUserCarList(user.getId());
        for (UserCar userCar : list) {
            userCar.getCar().setCarModel(carModelService.getModelById(userCar.getCar().getModelId()));
            userCar.getCar().getCarModel().setSeries(carModelService.getSeriesById(userCar.getCar().getCarModel().getSeriesId()));
            userCar.getCar().getCarModel().getSeries().setCarBrand(carModelService.getBrandById(userCar.getCar().getCarModel().getSeries().getBrandId()));
            userCar.getCar().getCarModel().getSeries().setCarFactory(carModelService.getFactoryById(userCar.getCar().getCarModel().getSeries().getFactoryId()));
            userCar.getCar().getCarModel().getSeries().getCarBrand().setLogoPath(uploadService.getFileUrl(userCar.getCar().getCarModel().getSeries().getCarBrand().getLogoPath()));
        }
        ret.setResult(CommonOperationResult.Succeeded);

        ret.setData(list);
        return ret;
    }

    /**
     * 取得用户当前的车辆信息
     *
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/car/getCurrentSelectedCar")
    @ResponseBody
    public CommonOperationResultWidthData<Car> getCurrentSelectedCar() {
        CommonOperationResultWidthData<Car> ret = new CommonOperationResultWidthData<>();
        Car car = getCar();
        if (car == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(messageSource.getMessage("order.validate.car.notExists", null, Locale.getDefault()));
            return ret;
        }
        ret.setData(car);
        ret.setResult(CommonOperationResult.Succeeded);

        return ret;
    }

    /**
     * 用户选择车辆
     *
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @RequestMapping("wxmp/car/chooseCar")
    @ResponseBody
    public CommonOperationResultWidthData<String> chooseCar(@RequestParam BigDecimal carId, @RequestAttribute(JWTAuthInterceptor.JWT_ATTR_NAME) String jwt) {

        Map<String, String> map = new HashMap<>();
        map.put(JWTService.JWT_CAR_ID, carId.toString());
        return jwtService.refreshToken(jwt, map);

    }
}
