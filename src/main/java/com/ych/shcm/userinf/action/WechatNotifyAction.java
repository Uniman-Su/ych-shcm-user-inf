package com.ych.shcm.userinf.action;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.ych.core.fasterxml.jackson.MapperUtils;
import com.ych.core.wechat.mp.WXMPUtils;
import com.ych.core.wechat.mp.pushmsg.CompositeMessage;
import com.ych.core.wechat.mp.pushmsg.IPushMessageResponse;
import com.ych.core.wechat.mp.pushmsg.PushMsgHandler;

/**
 * 微信通知的处理Action
 */
@RequestMapping("wxmp/inf/")
@Controller("shcm.userinf.action.WechatNotifyAction")
public class WechatNotifyAction {

    private Logger logger = LoggerFactory.getLogger(WechatNotifyAction.class);

    @Autowired
    private WXMPUtils wxmpUtils;

    @Resource(name = "wxMPPushMsgHandler")
    private PushMsgHandler pushMsgHandler;

    /**
     * 处理微信对通知接口的校验
     *
     * @param nonce
     *         随机串
     * @param timestamp
     *         时间戳
     * @param signature
     *         签名
     * @param echostr
     *         响应字符串
     * @return 返回值
     */
    @RequestMapping(path = "notify", method = RequestMethod.GET)
    @ResponseBody
    public String validateInf(@RequestParam("nonce") String nonce, @RequestParam("timestamp") long timestamp, @RequestParam("signature") String signature, @RequestParam("echostr") String echostr) {
        return wxmpUtils.validateInf(nonce, timestamp, signature, echostr);
    }

    /**
     * 处理微信的通知
     *
     * @param message
     *         通知消息
     */
    @RequestMapping(path = "notify", method = RequestMethod.POST)
    public void notify(@RequestBody CompositeMessage message, HttpServletResponse response) {
        IPushMessageResponse<?> pushMsgResp = pushMsgHandler.handle(message);
        MediaType mediaType = pushMsgResp.getMediaType();

        response.setHeader("Content-Type", mediaType.toString());

        OutputStream os = null;

        try {
            os = response.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");

            if (mediaType.equals(MediaType.APPLICATION_XML) || mediaType.equals(MediaType.TEXT_XML)) {
                writer.write(MapperUtils.XML_MAPPER.get().writeValueAsString(pushMsgResp.getResponse()));
            } else if (mediaType.equals(MediaType.TEXT_PLAIN)) {
                writer.write(String.valueOf(pushMsgResp.getResponse()));
            } else if (mediaType.equals(MediaType.APPLICATION_JSON_UTF8) || mediaType.equals(MediaType.APPLICATION_JSON)) {
                writer.write(MapperUtils.MAPPER.get().writeValueAsString(pushMsgResp.getResponse()));
            }

            response.flushBuffer();
        } catch (IOException e) {
            logger.error("Write response failed", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

}
