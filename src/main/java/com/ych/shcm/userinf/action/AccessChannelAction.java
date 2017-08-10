package com.ych.shcm.userinf.action;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.shcm.o2o.model.AccessChannel;
import com.ych.shcm.o2o.openinf.IResponse;
import com.ych.shcm.o2o.service.AccessChannelService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;

/**
 * 渠道基本信息获取相关的Action
 *
 * @author U
 */
@Controller("shcm.userinf.action.baseinfo.AccessChannelAction")
public class AccessChannelAction {

    @Autowired
    private AccessChannelService accessChannelService;

    /**
     * 查询渠道信息
     *
     * @param id
     */
    @RequestMapping("accessChannel/queryAccessChannel")
    @ResponseBody
    public CommonOperationResultWidthData<AccessChannel> queryAccessChannelById(@RequestParam BigDecimal id) {
        CommonOperationResultWidthData<AccessChannel> ret = new CommonOperationResultWidthData<>();
        ret.setData(accessChannelService.getById(id));
        ret.getData().setNotifyUrl("******");
        ret.getData().setSecurityKey("******");
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 接入渠道的请求入口
     *
     * @param appCode
     *         AppCode
     * @param requestBody
     *         报文体
     * @param digest
     *         签名
     * @param timestamp
     *         时间戳
     * @return 响应消息
     */
    @RequestMapping("accessChannel/request")
    @ResponseBody
    public IResponse<?> accessChannelRequest(@RequestParam("appCode") String appCode, @RequestBody String requestBody, @RequestParam("digest") String digest, @RequestParam("timestamp") long timestamp) {
        return accessChannelService.dispatchRequest(appCode, requestBody, digest, timestamp);
    }


}
