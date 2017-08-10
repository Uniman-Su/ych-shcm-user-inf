package com.ych.shcm.userinf.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.shcm.o2o.service.JWTService;
import com.ych.shcm.o2o.service.OperatorServcie;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 控制台登录相关的Action
 * <p>
 * Created by U on 2017/7/18.
 */
@RequestMapping("console/logon")
@Controller("shcm.userinf.action.ConsoleLoginAction")
public class ConsoleLoginAction {

    @Autowired
    private OperatorServcie operatorServcie;

    @Autowired
    private JWTService jwtService;

    /**
     * 执行登录操作
     *
     * @param userName
     *         用户名
     * @param password
     *         密码
     * @return 操作结果
     */
    @RequestMapping("login")
    @ResponseBody
    public CommonOperationResultWidthData<String> login(@RequestParam("userName") String userName, @RequestParam("password") String password) {
        return operatorServcie.login(userName, password);
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

}
