package com.ych.shcm.userinf.action;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.SystemParameterHolder;
import com.ych.shcm.o2o.action.UserAction;
import com.ych.shcm.o2o.annotation.JWTAuth;
import com.ych.shcm.o2o.dao.ServiceItemDao;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.ServiceItem;
import com.ych.shcm.o2o.model.ServicePack;
import com.ych.shcm.o2o.service.CarService;
import com.ych.shcm.o2o.service.ServicePackService;
import com.ych.shcm.o2o.service.UploadService;
import com.ych.shcm.o2o.service.systemparamholder.SelectableSecondSPMonth;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务接口
 */
@Controller("shcm.userinf.action.ServicePackAction")
public class ServicePackAction extends UserAction {

    @Autowired
    ServicePackService servicePackService;
    @Autowired
    private CarService carService;
    @Autowired
    private ServiceItemDao serviceItemDao;
    @Autowired
    private UploadService uploadService;

    @Resource(name = SelectableSecondSPMonth.NAME)
    private SystemParameterHolder selectableSecondSPMonth;

    /**
     * 查询车辆可用服务包
     *
     * @param carId
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @ResponseBody
    @RequestMapping("wxmp/service/queryServicePacks")
    public CommonOperationResultWidthData<List<ServicePack>> queryServicePacks(@RequestParam("carId") BigDecimal carId) {
        CommonOperationResultWidthData<List<ServicePack>> ret = new CommonOperationResultWidthData<>();

        Car car = carService.getCarById(carId);
        List<ServicePack> servicePacks = carService.getServicePackOfCar(car.getModelId());
        for (ServicePack servicePack : servicePacks) {
            servicePack.setIconPath(uploadService.getFileUrl(servicePack.getIconPath()));
        }
        ret.setResult(CommonOperationResult.Succeeded);
        ret.setData(servicePacks);
        return ret;
    }

    /**
     * 查询车辆可选服务包
     *
     * @param carId
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @ResponseBody
    @RequestMapping("wxmp/service/queryCanChooseServicePacks")
    public CommonOperationResultWidthData<List<CanChooseServicePack>> queryCanChooseServicePacks(@RequestParam("carId") BigDecimal carId) {
        CommonOperationResultWidthData<List<CanChooseServicePack>> ret = new CommonOperationResultWidthData<>();

        Car car = carService.getCarById(carId);
        boolean isFirstMaintenance = false;

        //首保
        if (car.getFirstOrderId() == null) {
            isFirstMaintenance = true;
        }

        //上牌时间判断
        boolean registrationFlag = false;
        if (car.getRegistrationTime() != null) {
            Date registrationDate = DateUtils.truncate(car.getRegistrationTime(), Calendar.MONTH);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -selectableSecondSPMonth.getIneterValue());
            Date compareDate = DateUtils.truncate(calendar, Calendar.MONTH).getTime();
            if (registrationDate.compareTo(compareDate) >= 0) {
                //上牌时间超过指定月数
                registrationFlag = true;
            }
        }

        List<ServicePack> servicePacks = carService.getServicePackOfCar(car.getModelId());
        List<CanChooseServicePack> canChooseServicePacks = new ArrayList<>();
        for (ServicePack servicePack : servicePacks) {
            CanChooseServicePack canChooseServicePack = new CanChooseServicePack();
            if (isFirstMaintenance & !registrationFlag) {
                //首保把所有项目不可选
                canChooseServicePack.setCanChoose(false);
            }
            BeanUtils.copyProperties(servicePack, canChooseServicePack);
            canChooseServicePacks.add(canChooseServicePack);
        }
        //首服务始终可选
        if (CollectionUtils.isNotEmpty(canChooseServicePacks)) {
            canChooseServicePacks.get(0).setCanChoose(true);
            // 指定注册月份以内的车辆默认选择第二个服务
            if (canChooseServicePacks.size() >= 2) {
                if (registrationFlag) {
                    canChooseServicePacks.get(1).setSelected(true);
                } else {
                    canChooseServicePacks.get(0).setSelected(true);
                }
            } else {
                canChooseServicePacks.get(0).setSelected(true);
            }
        }

        ret.setResult(CommonOperationResult.Succeeded);
        ret.setData(canChooseServicePacks);
        return ret;
    }

    /**
     * 可选服务包
     */
    public static class CanChooseServicePack extends ServicePack {

        boolean canChoose = true;

        boolean selected = false;

        /**
         * Getter for property 'canChoose'.
         *
         * @return Value for property 'canChoose'.
         */
        public boolean isCanChoose() {
            return canChoose;
        }

        /**
         * Setter for property 'canChoose'.
         *
         * @param canChoose
         *         Value to set for property 'canChoose'.
         */
        public void setCanChoose(boolean canChoose) {
            this.canChoose = canChoose;
        }

        /**
         * Getter for property 'selected'.
         *
         * @return Value for property 'selected'.
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Setter for property 'selected'.
         *
         * @param selected
         *         Value to set for property 'selected'.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    /**
     * 查询服务包项目
     *
     * @param packId
     * @return
     */
    @JWTAuth(issuer = Constants.WECHAT_MP_ISSUER)
    @ResponseBody
    @RequestMapping("wxmp/service/queryServicePacksItems")
    public CommonOperationResultWidthData<Map<String, Object>> queryServicePacksItems(@RequestParam("packId") BigDecimal packId) {

        Map<String, Object> map = new HashMap<>();

        CommonOperationResultWidthData<Map<String, Object>> ret = new CommonOperationResultWidthData<>();
        List<ServiceItem> items = serviceItemDao.selectServiceItemsOfPack(packId);
        for (ServiceItem item : items) {
            item.setIconPath(uploadService.getFileUrl(item.getIconPath()));
        }
        map.put("items", items);
        map.put("price", servicePackService.getPriceOfPack(items, getCar()));
        ret.setResult(CommonOperationResult.Succeeded);
        ret.setData(map);
        return ret;
    }

}
