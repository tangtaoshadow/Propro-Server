package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.component.HttpClient;
import com.westlake.air.propro.config.VMProperties;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.message.ApplyMessage;
import com.westlake.air.propro.domain.bean.message.DingtalkMessage;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.utils.JWTUtil;
import com.westlake.air.propro.utils.PasswordUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
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
        } else {
            result.setSuccess(true);
        }

        return result;
    }


    /***
     * @Archive 登录
     * @param user          传入 username Password
     * @return json 附带status 和 token
     */
    @RequestMapping("/login")
    public String login(UserDO user) {
        Map<String, Object> map = new HashMap<String, Object>();
        int res = checkLogin(user.getUsername(), user.getPassword(), map);
        map.put("status", res);

        if (0 == res) {
            // 登录成功
            map.put("token", JWTUtil.createToken(user.getUsername()));
            // 添加用户需要的信息
        }
        // 返回json
        JSONObject json = new JSONObject(map);
        return json.toString();
    }


    @RequestMapping("apply")
    @ResponseBody
    public ResultDO apply(Model model,
                          @RequestParam(value = "username", required = true) String username,
                          @RequestParam(value = "email", required = true) String email,
                          @RequestParam(value = "telephone", required = true) String telephone,
                          @RequestParam(value = "dingtalkId", required = false) String dingtalkId,
                          @RequestParam(value = "organization", required = false) String organization) {
        ResultDO resultDO = new ResultDO(true);
        String dingtalkRobot = vmProperties.getDingtalkRobot();
        ApplyMessage am = new ApplyMessage();
        am.setUsername(username);
        am.setEmail(email);
        am.setTelephone(telephone);
        am.setOrganization(organization);
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

        return "redirect:/login/login";
    }


    /***
     * @Author TangTao
     * @CreateTime 2019-8-3 13:52:23
     * @Statement 为什么传入 map ？ 首先这个函数包装了所有处理登录过程
     *                  返回一个 int 供后端处理 为什么不直接把 int 写入 map 这时为了后续开发 可能会有更复杂的操作 而这里只负责验证登录
     *                  登录成功就把用户信息写入map 这样只需一次查询操作
     * @Archive 实现对账号密码进行验证 成功之后同时返回了用户信息
     * @param           username
     * @param           password
     * @param           map         如果用户登录成功 将用户信息写入 map 供前端显示
     * @return 成功 0
     */
    protected int checkLogin(String username, String password, Map<String, Object> map) {
        // 判断 username password 是否为空
        if (null == username || null == password) {
            return -1;
        }
        // 1 从数据库中根据 username 获取存在的用户
        UserDO userInfo = userService.getByUsername(username);
        System.out.println(userInfo);
        if (null == userInfo) {
            // 用户不存在
            return -2;
        }

        // 2 获取 salt
        String salt = userInfo.getSalt();
        // 3 生成 password
        String hashPassword = PasswordUtil.getHashPassword(password, salt);
        // 4 验证 password
        if (hashPassword.equals(userInfo.getPassword())) {
            // 登录成功
            System.out.println("登录成功");
            // 写入 map 一起发送给前端
            map.put("username", username);
            map.put("email", userInfo.getEmail());
            map.put("nick", userInfo.getNick());
            map.put("organization", userInfo.getOrganization());
            map.put("telephone", userInfo.getTelephone());
            // 这个角色 前端拿到用来供给不同用户显示不同界面 但是不显示出来 一是没有必要 二是泄露了隐私
            map.put("roles", userInfo.getRoles());
            return 0;
        } else {
            // 密码错误
            return -3;
        }

    }


}
