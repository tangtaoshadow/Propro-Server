package com.westlake.air.propro.controller;

import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.ProjectService;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.utils.JWTUtil;
import com.westlake.air.propro.utils.PasswordUtil;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UserController extends BaseController {


    @Autowired
    UserService userService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ExperimentService experimentService;

    @RequestMapping(value = "/profile")
    String profile(Model model, RedirectAttributes redirectAttributes) {
        Object object = SecurityUtils.getSubject().getPrincipal();
        if (object != null) {
            UserDO userDO = userService.getByUsername(((UserDO) object).getUsername());

            model.addAttribute("user", userDO);

            if (isAdmin()) {
                model.addAttribute("projectCount", projectService.count(new ProjectQuery()));
                model.addAttribute("expCount", experimentService.count(new ExperimentQuery()));
            } else {
                model.addAttribute("projectCount", projectService.count(new ProjectQuery(userDO.getUsername())));
                model.addAttribute("expCount", experimentService.count(new ExperimentQuery(userDO.getUsername())));
            }
        }
        return "user/profile";
    }


    /***
     * @UpdateTime 2019-8-7 00:39:41
     * @param nick
     * @param email
     * @param telephone
     * @param organization
     * @return json 格式 status 0 正常 <0 异常
     */
    @RequestMapping(value = "/updateInfo", method = RequestMethod.POST)
    public String updateUserInfo(
            @RequestParam(value = "nick", required = false) String nick,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "telephone", required = false) String telephone,
            @RequestParam(value = "organization", required = false) String organization
    ) {

        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();
        String username = getCurrentUsername();
        int status = -1;
        if (username == null) {
            // token 不正常
            status = -2;
        }

        UserDO user = userService.getByUsername(username);
        if (user == null) {
            // 不存在的错误 token异常 或者 数据库错误
            status = -3;
        }
        user.setNick(nick);
        user.setEmail(email);
        user.setTelephone(telephone);
        user.setOrganization(organization);
        userService.update(user);

        status = (-1 == status) ? 0 : status;
        map.put("status", status);
        JSONObject json = new JSONObject(map);

        return json.toString();
    }


    /****
     * @CreatTime 2019-8-7 21:55:39
     * @UpdateTime 2019-8-7 22:05:10
     * @Author TangTao
     * @param oldPwd        传入的原来密码
     * @param newPwd        更新的密码 长度至少为6
     * @return json         格式 status  状态返回值切勿随意改动 否则前端会报出其他错误 前端鲁棒性较强 也有可能短时间内观察不出来
     *                      0 : success
     *                      -1 : 正常出错
     *                      <-1 : 不存在的错误 但是发生了
     *                      只返回最有用的数据 其余全交给前端处理
     */
    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    String updatePassword(@RequestParam(value = "current_password", required = true) String oldPwd,
                          @RequestParam(value = "new_password", required = true) String newPwd) {

        String username = getCurrentUsername();
        UserDO user = null;

        // 默认状态 成功 0
        int status = 0;

        // 这里巧妙运用循环结构 就不需要很多的if-else
        // 使得整个函数一个入口一个出口 出错就取消往下执行
        do {

            if (username == null) {
                // 不正常的情况 可以直接拒绝
                status = -2;
                break;
            }

            // 通过用户名 查询用户对象
            user = userService.getByUsername(username);
            String myOldPassword = user.getPassword();

            // 服务器出错 因为 getCurrentUsername 已经通过 userService 成功查询出用户了
            // 所以这里不可能出错 但是尽管可能性很小 为了安全起见 还是再校验一遍数据
            // 这里应该使用 StringUtils.isEmpty() 因为它可以处理其他异常值
            // 通过源码可以发现 str == null || "".equals(str)
            if (user == null || StringUtils.isEmpty(myOldPassword)) {
                status = -3;
                break;
            }

            // 生成 旧密码的 md5 值
            String oldMD5Pwd = PasswordUtil.getHashPassword(oldPwd, user.getSalt());
            if (!myOldPassword.equals(oldMD5Pwd)) {
                // 输入的原密码错误
                status = -1;
                break;
            }

            // 为了安全起见 这里要求长度至少大于 5
            if (6 > newPwd.length()) {
                // impossible 这是不正常的情况 除非遭受攻击 秉承前端数据不可信原则 这里再判断一次
                status = -4;
            }

        } while (false);

        // 当且仅当 status 成功时 才采取更新操作
        if (0 == status) {
            // 通过上面的校验
            // 开始生成新密码
            String randomSalt = PasswordUtil.getRandomSalt();
            String result = PasswordUtil.getHashPassword(newPwd, randomSalt);
            user.setSalt(randomSalt);
            user.setPassword(result);
            // 执行数据库更新
            userService.update(user);
        }


        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("status", status);
        JSONObject json = new JSONObject(map);
        System.out.println("password " + json.toString());
        return json.toString();
    }


    /****
     * @CreatTime 2019-8-8 13:23:03
     * @UpdateTime 2019-8-8 13:23:11
     * @Author TangTao
     * @Archive 更新 token
     * @return 成功 status 0 成功 附带新token -1 服务器错误 -2 token 验证失败
     */
    @RequestMapping(value = "/updateToken", method = RequestMethod.POST)
    String updateToken() {

        // 默认状态 失败 -1
        int status = -1;

        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();

        String username = getCurrentUsername();
        if (!StringUtils.isEmpty(username)) {
            map.put("token", JWTUtil.createToken(username));
            status = 0;
        } else {
            // 不正常的情况 可以直接拒绝
            // 比如 token 验证失败 token 过期
            status = -2;
        }


        map.put("status", status);
        JSONObject json = new JSONObject(map);
        return json.toString();
    }


}
