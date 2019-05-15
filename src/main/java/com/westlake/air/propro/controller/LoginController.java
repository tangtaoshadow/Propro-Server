package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.config.VMProperties;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.message.ApplyMessage;
import com.westlake.air.propro.domain.bean.message.DingtalkMessage;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.service.impl.HttpClient;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("login")
public class LoginController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    UserService userService;

    @Autowired
    VMProperties vmProperties;

    @Autowired
    HttpClient httpClient;

    @RequestMapping(value = "/dologin")
    @ResponseBody
    public ResultDO login(UserDO user, boolean remember) {
        ResultDO result = new ResultDO();
        String username = user.getUsername();
        Subject currentUser = SecurityUtils.getSubject();
        if (!currentUser.isAuthenticated()) {
            UsernamePasswordToken token = new UsernamePasswordToken(username, user.getPassword());
            try {
                if (remember) {
                    token.setRememberMe(true);
                }
                currentUser.login(token);
                result.setSuccess(true);
            } catch (UnknownAccountException uae) {
                result.setErrorResult(ResultCode.USER_NOT_EXISTED);
            } catch (IncorrectCredentialsException ice) {
                result.setErrorResult(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            } catch (LockedAccountException lae) {
                result.setErrorResult(ResultCode.ACCOUNT_IS_LOCKED);
            } catch (ExcessiveAttemptsException eae) {
                result.setErrorResult(ResultCode.TRY_TOO_MANY_TIMES);
            } catch (AuthenticationException ae) {
                //通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,堆栈轨迹如下");
                ae.printStackTrace();
                result.setErrorResult(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            }
        }

        return result;
    }

    @RequestMapping(value = "/login")
    public String login(HttpServletRequest request) {
        logger.info("可能被重定向了");
        try {
            if ((null != SecurityUtils.getSubject() && SecurityUtils.getSubject().isAuthenticated()) || (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isRemembered())) {
                return "redirect:/";
            } else {
                logger.info("--进行登录验证..验证开始");
                return "login";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "login";
    }

    @RequestMapping("apply")
    @ResponseBody
    public ResultDO apply(Model model,
                          @RequestParam(value = "username", required = true) String username,
                          @RequestParam(value = "email", required = true) String email,
                          @RequestParam(value = "telephone", required = true) String telephone,
                          @RequestParam(value = "dingtalkId", required = false) String dingtalkId,
                          @RequestParam(value = "university", required = false) String university) {
        ResultDO resultDO = new ResultDO(true);
        String dingtalkRobot = vmProperties.getDingtalkRobot();
        ApplyMessage am = new ApplyMessage();
        am.setUsername(username);
        am.setEmail(email);
        am.setTelephone(telephone);
        am.setUniversity(university);
        am.setDingtalkId(dingtalkId);
        DingtalkMessage message = new DingtalkMessage("账号申请", am.markdown());
        String response = httpClient.client(dingtalkRobot, JSON.toJSONString(message));
        logger.info(response);
        return resultDO;
    }

    @RequestMapping(value = "/logout")
    public String logout(HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }

        return "login/login";
    }
}
