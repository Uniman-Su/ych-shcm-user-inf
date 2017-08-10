package com.ych.shcm.userinf.action;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.MultiSortableParameter;
import com.ych.core.model.PagedList;
import com.ych.core.model.SortOrder;
import com.ych.shcm.o2o.autohome.spider.carmodel.service.CarModelSpider;
import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.parameter.QueryCarBrandListParameter;
import com.ych.shcm.o2o.parameter.QueryCarSeriesListParameter;
import com.ych.shcm.o2o.service.CarModelService;
import com.ych.shcm.o2o.service.UploadService;

/**
 * 车型相关的Action
 *
 * @author U
 */
@Controller("shcm.userinf.action.CarModelAction")
@RequestMapping("/carModel")
public class CarModelAction {

    private static final Logger LOG = LoggerFactory.getLogger(CarModelAction.class);

    /**
     * 车型爬虫
     */
    @Autowired
    private CarModelSpider modelSpider;

    /**
     * 品牌服务
     */
    @Autowired
    private CarModelService modelSvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UploadService uploadSvc;

    /**
     * 进入车型爬虫触发器页面
     *
     * @return 视图模型
     */
    @RequestMapping("autohomeSpiderTrigger.html")
    public ModelAndView enterSpiderTrigger() {
        return new ModelAndView("carModel/autohomeSpiderTrigger");
    }

    /**
     * 抓取车系
     *
     * @return 操作结果
     */
    @RequestMapping("drawBrands")
    @ResponseBody
    public CommonOperationResult drawBrands() {

        if (modelSpider.drawCarBrands() != CommonOperationResult.Succeeded) {
            return CommonOperationResult.Failed;
        }

        return CommonOperationResult.Succeeded;
    }

    /**
     * 抓取车系
     *
     * @return 操作结果
     */
    @RequestMapping("drawSerieses")
    @ResponseBody
    public CommonOperationResult drawSerieses() {
        List<CarBrand> brandList = modelSvc.getBrandList();

        for (CarBrand brand : brandList) {
            if (modelSpider.drawCarSerieses(brand) != CommonOperationResult.Succeeded) {
                return CommonOperationResult.Failed;
            }
        }

        return CommonOperationResult.Succeeded;
    }

    /**
     * 抓取车型
     *
     * @return 操作结果
     */
    @RequestMapping("drawModels")
    @ResponseBody
    public CommonOperationResult drawModels() {
        QueryCarSeriesListParameter parameter = new QueryCarSeriesListParameter();
        parameter.setPageIndex(0);
        parameter.setPageSize(100);
        List<MultiSortableParameter.SortParameter> sortParameters = new ArrayList<>();
        sortParameters.add(new MultiSortableParameter.SortParameter("firstChar"));
        sortParameters.add(new MultiSortableParameter.SortParameter("name"));
        parameter.setSorts(sortParameters);

        for (PagedList<CarSeries> pagedList = modelSvc.querySeriesPagedList(parameter); pagedList.getList()
                .size() > 0; pagedList = modelSvc.querySeriesPagedList(parameter)) {
            for (CarSeries series : pagedList.getList()) {
                if (modelSpider.drawCarModels(series) != CommonOperationResult.Succeeded) {
                    return CommonOperationResult.Failed;
                }
            }

            if (pagedList.getList().size() < parameter.getPageSize()
                    || pagedList.getPageIndex() == pagedList.getPageCount() - 1) {
                break;
            }

            parameter.setPageIndex(parameter.getPageIndex() + 1);
            parameter.setSorts(sortParameters);
        }

        return CommonOperationResult.Succeeded;
    }

    /**
     * 抓取品牌图标
     *
     * @return 操作结果
     */
    @RequestMapping("drawBrandLogos")
    @ResponseBody
    public CommonOperationResult drawBrandLogos() {
        return modelSpider.drawBrandLogo();
    }

    /**
     * 获取所有的可用车型品牌并按首字母排序
     *
     * @return
     */
    @RequestMapping("/getBrandList")
    @ResponseBody
    public List<CarBrand> getBrandList() {
        List<CarBrand> carBrands = modelSvc.getBrandList();
        ;
        return carBrands;
    }

    /**
     * 查询品牌下所有车系
     *
     * @param brandId
     *         品牌ID
     * @return 车系
     */
    @RequestMapping("/getSeriesList")
    @ResponseBody
    public List<CarSeries> getSeriesList(@RequestParam("brandId") BigDecimal brandId, @RequestParam("factoryId") BigDecimal factoryId) {
        return modelSvc.getSeriesList(brandId, factoryId);
    }

    /**
     * 查询品牌下所有年份系
     *
     * @param seriesId
     * @return
     */
    @RequestMapping("/getSeriesYearList")
    @ResponseBody
    public List<CarSeriesYear> getSeriesYearList(@RequestParam("seriesId") BigDecimal seriesId) {
        return modelSvc.getSeriesYearList(seriesId);
    }

    /**
     * 查询品牌下所有开系
     *
     * @param seriesId
     * @return
     */
    @RequestMapping("/getModelList")
    @ResponseBody
    public List<CarModel> getModelList(@RequestParam("seriesId") BigDecimal seriesId, @RequestParam("seriesYearId") BigDecimal seriesYearId) {
        return modelSvc.getModelList(seriesId, seriesYearId);
    }

    /**
     * 查询品牌下所有工厂
     *
     * @param brandId
     *         品牌ID
     * @return 工厂列表
     */
    @RequestMapping("/getFactoryList")
    @ResponseBody
    public List<CarFactory> getFactoryList(@RequestParam("brandId") BigDecimal brandId) {
        return modelSvc.getByBrandId(brandId);
    }

    /**
     * 进入选择页面
     *
     * @return
     */
    @RequestMapping("/enterSelectPage.html")
    public ModelAndView enterSelectPage() {
        ModelAndView mav = new ModelAndView("carModel/selectpage");
        return mav;
    }

    /**
     * 进入选择页面
     *
     * @return
     */
    @RequestMapping("/enterSelectFactory.html")
    public ModelAndView enterSelectFactory() {
        ModelAndView mav = new ModelAndView("carModel/selectFactory");
        return mav;
    }

}
