package com.ych.shcm.userinf.action;

import java.text.ParseException;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import com.ych.core.model.BaseWithCommonOperationResult;
import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;
import com.ych.core.wechat.mp.Button;
import com.ych.core.wechat.mp.JsApiInitInfo;
import com.ych.core.wechat.mp.UserInfo;
import com.ych.core.wechat.mp.WXMPUtils;
import com.ych.core.wechat.mp.authorization.AuthorizationScope;
import com.ych.core.wechat.mp.pushmsg.ButtonType;
import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.service.*;
import com.ych.shcm.o2o.service.systemparamholder.*;
import com.ych.shcm.o2o.wechat.parameter.NavigateInParameter;
import com.ych.shcm.o2o.wechat.parameter.RedirectBackParameter;

/**
 * 微信登录相关的Action
 * <p>
 * Created by U on 2017/7/18.
 */
@RequestMapping("wxmp/logon")
@Controller("shcm.userinf.action.WechatLoginAction")
public class WechatLoginAction {

    @Autowired
    private WechatService wechatService;

    @Autowired
    private WXMPUtils wxmpUtils;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private OperatorServcie operatorServcie;

    @Autowired
    private ShopService shopService;

    @Autowired
    private SystemParameterService systemParameterService;

    @Resource(name = WXShopOwnerBindOpenIdUrl.NAME)
    private SystemParameterHolder shopOwnerBindOpenIdUrl;

    @Resource(name = WXShopOwnerHomepageUrl.NAME)
    private SystemParameterHolder shopOwnerHomepageUrl;

    @Resource(name = WXOperatorBindOpenIdUrl.NAME)
    private SystemParameterHolder operatorBindOpenIdUrl;

    @Resource(name = WXShopLocationInputUrl.NAME)
    private SystemParameterHolder shopLocationInputUrl;

    @Resource(name = WXErrorPageUrl.NAME)
    private SystemParameterHolder errorPageUrl;

    @Autowired
    private MessageSource messageSource;

    /**
     * 验证微信公众号导航进入的参数
     *
     * @param parameter
     *         输入参数
     * @return 如果验证成功会附带返回微信用户授权页的URL, 否则会返回错误描述
     */
    @RequestMapping("guidance")
    public ModelAndView navigateIn(NavigateInParameter parameter) {
        ModelAndView ret;

        CommonOperationResultWidthData<String> validateResult = wechatService.navigateIn(parameter);
        if (validateResult.getResult() == CommonOperationResult.Succeeded) {
            ret = new ModelAndView("thirdGuideInRedirect");
            ret.addObject("url", validateResult.getData());
        } else {
            ret = new ModelAndView("errorPageRedirect");
            ret.addObject("actionUrl", errorPageUrl.getStringValue());
            ret.addObject("errorMsg", validateResult.getDescription());
        }

        return ret;
    }

    /**
     * 执行微信的回调操作
     *
     * @param parameter
     *         回调参数
     * @return 操作结果, 成功时将附带用户的JWT
     */
    @RequestMapping("guidanceRedirectBack")
    @ResponseBody
    public CommonOperationResultWidthData<String> redirectBack(@RequestBody RedirectBackParameter parameter) {
        return wechatService.redirectBack(parameter);
    }

    /**
     * 刷新JWT
     *
     * @param token
     *         旧的JWT
     * @return 如果操作成功会返回新的JWT
     */
    @RequestMapping("refreshToken")
    @ResponseBody
    public CommonOperationResultWidthData<String> refreshToken(@RequestBody String token) {
        return jwtService.refreshToken(token);
    }

    /**
     * 店铺所有者进入的入口
     */
    @RequestMapping("shopOwnerEntrance.html")
    public String shopOwnerEntrance() {
        return "redirect:" + wechatService.getShopOwenerEntranceAuthUrl(AuthorizationScope.snsapi_userinfo);
    }

    /**
     * 店铺所有者微信回调URL
     *
     * @param code
     *         Code
     * @return 视图模型
     */
    @RequestMapping("shopOwnerRedirectBack.html")
    public ModelAndView shopOwnerRedirectBack(@RequestParam("code") String code) {
        ModelAndView ret = new ModelAndView("shopOwnerRedirectBack");

        CommonOperationResultWidthData<String> authResult = wechatService.simpleRedirectBack(code);

        UserThirdAuth userThirdAuth = userService.getThirdAuthByByThirdId(ThirdAuthType.WECHAT_OPENID, authResult.getData());

        if (userThirdAuth == null) {
            ret.addObject("openId", authResult.getData());
            ret.addObject("actionUrl", shopOwnerBindOpenIdUrl.getStringValue());
        } else {
            List<Shop> shopList = shopService.getByUserId(userThirdAuth.getUserId());

            if (CollectionUtils.isEmpty(shopList)) {
                ret.setViewName("errorPageRedirect");
                ret.addObject("actionUrl", errorPageUrl.getStringValue());
                ret.addObject("errorMsg", messageSource.getMessage("user.shop.notBinded", null, Locale.getDefault()));
                return ret;
            }

            HashMap<String, String> claims = new HashMap<>();
            claims.put(JWTService.JWT_SHOP_ID, String.valueOf(shopList.get(0).getId()));

            ret.addObject("jwt", jwtService.generateWechatJWT(userThirdAuth.getUserId(), userThirdAuth.getThirdId(), claims));
            ret.addObject("actionUrl", shopOwnerHomepageUrl.getStringValue());
        }


        return ret;
    }

    /**
     * 通过用户名和密码进行登录
     *
     * @param userName
     *         用户名
     * @param password
     *         密码
     * @param openId
     *         OpenID
     * @return 成功时附带返回JWT
     */
    @ResponseBody
    @RequestMapping("loginByUserNameAndPassword")
    public CommonOperationResultWidthData<String> loginByUserNameAndPassword(@RequestParam("userName") String userName, @RequestParam("password") String password, @RequestParam("openId") String openId) {
        return wechatService.loginByUserNameAndPassword(userName, password, openId);
    }

    /**
     * 店铺坐标更新进入的入口
     */
    @RequestMapping("shopLocationEntrance.html")
    public String shopLocationEntrance() {
        return "redirect:" + wechatService.getShopLocationEntranceAuthUrl(AuthorizationScope.snsapi_userinfo);
    }

    /**
     * 操作员登录的回调页面
     *
     * @param code
     *         Code
     * @param entranceParam
     *         跳转入口参数名称
     * @return 视图模型
     */
    @RequestMapping("operatorLoginRedirectBack.html")
    public ModelAndView operatorLoginRedirectBack(@RequestParam("code") String code, @RequestParam(name = "entranceParam", required = false) String entranceParam) {
        ModelAndView ret = new ModelAndView("operatorRedirectBack");

        CommonOperationResultWidthData<String> authResult = wechatService.simpleRedirectBack(code);

        OperatorThirdAuth operatorThirdAuth = operatorServcie.getThirdAuthByByThirdId(ThirdAuthType.WECHAT_OPENID, authResult.getData());

        if (operatorThirdAuth == null) {
            ret.addObject("openId", authResult.getData());
            ret.addObject("actionUrl", operatorBindOpenIdUrl.getStringValue());

            if (StringUtils.isNotEmpty(entranceParam)) {
                ret.addObject("targetUrl", systemParameterService.getStringValue(entranceParam));
            }
        } else {
            HashMap<String, String> claims = new HashMap<>();
            claims.put(JWTService.JWT_OPEN_ID, operatorThirdAuth.getThirdId());

            ret.addObject("jwt", jwtService.generateWebConsoleJWT(operatorThirdAuth.getOperatorId(), claims));

            if (StringUtils.isEmpty(entranceParam)) {
                ret.addObject("actionUrl", shopLocationInputUrl.getStringValue());
            } else {
                ret.addObject("actionUrl", systemParameterService.getStringValue(entranceParam));
            }
        }

        return ret;
    }

    /**
     * 管理员通过用户名和密码进行登录
     *
     * @param userName
     *         用户名
     * @param password
     *         密码
     * @param openId
     *         OpenID
     * @return 成功时附带返回JWT
     */
    @ResponseBody
    @RequestMapping("operatorLoginByUserNameAndPassword")
    public CommonOperationResultWidthData<String> operatorLoginByUserNameAndPassword(@RequestParam("userName") String userName, @RequestParam("password") String password, @RequestParam("openId") String openId) {
        return wechatService.operatorLoginByUserNameAndPassword(userName, password, openId);
    }

    /**
     * 获取JsApi的初始化参数
     *
     * @param url
     *         页面URL
     * @return 初始化信息
     */
    @ResponseBody
    @RequestMapping("getJsApiInitInfo")
    public JsApiInitInfo getJsApiInitInfo(String url, HttpServletRequest request) {
        return wxmpUtils.getJsApiInitInfo(url);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.addCustomFormatter(new DateFormatter() {
            @Override
            public Date parse(String text, Locale locale) throws ParseException {
                return new Date(Long.valueOf(text));
            }
        });
    }

}
