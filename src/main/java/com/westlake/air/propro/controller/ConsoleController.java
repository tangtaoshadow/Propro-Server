package com.westlake.air.propro.controller;

import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.enums.TaskStatus;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.*;
import com.westlake.air.propro.service.*;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@RestController
@RequestMapping("/console")
public class ConsoleController extends BaseController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    TaskService taskService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    UserService userService;
    private Logger logger = LoggerFactory.getLogger(ConsoleController.class);

    /***
     * @UpdateTime 2019-8-27 17:07:27
     * @Archive 请求资源列表 post
     * @return 返回资源类型 数目
     */
    @RequestMapping(value = "/resourceList", method = RequestMethod.POST)
    public String resourceList() {

        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();
        // 检查参数 username
        int status = -1;

        do {
            // 验证 token
            String username = getCurrentUsername();

            if (username == null) {
                // token 不正常
                status = -2;
                break;
            }

            ExperimentQuery experimentQuery = new ExperimentQuery();
            if (!isAdmin()) {
                experimentQuery.setOwnerName(username);
            }
            experimentQuery.setType(Constants.EXP_TYPE_DIA_SWATH);
            long expSWATHCount = experimentService.count(experimentQuery);
            map.put("expSwathCount", expSWATHCount);


            experimentQuery.setType(Constants.EXP_TYPE_PRM);
            long expPRMCount = experimentService.count(experimentQuery);
            map.put("expPRMCount", expPRMCount);


            AnalyseOverviewQuery analyseOverviewQuery = new AnalyseOverviewQuery();
            if (!isAdmin()) {
                analyseOverviewQuery.setOwnerName(username);
            }
            long overviewCount = analyseOverviewService.count(analyseOverviewQuery);
            map.put("overviewCount", overviewCount);

            ProjectQuery projectQuery = new ProjectQuery();
            if (!isAdmin()) {
                projectQuery.setOwnerName(username);
            }
            long projectCount = projectService.count(projectQuery);
            map.put("projectCount", projectCount);


            TaskQuery query = new TaskQuery();
            if (!isAdmin()) {
                query.setCreator(username);
            }
            query.setStatus(TaskStatus.RUNNING.getName());
            long taskRunningCount = taskService.count(query);
            map.put("taskRunningCount", taskRunningCount);


            LibraryQuery libraryQuery = new LibraryQuery(0);
            if (!isAdmin()) {
                libraryQuery.setCreator(username);
            }
            long libCount = libraryService.count(libraryQuery);
            map.put("libCount", libCount);


            libraryQuery.setType(1);
            long iRtLibCount = libraryService.count(libraryQuery);
            map.put("iRtLibCount", iRtLibCount);


            libraryQuery = new LibraryQuery(0);
            libraryQuery.setDoPublic(true);
            libraryQuery.setCreator(null);
            long publicLibCount = libraryService.count(libraryQuery);
            map.put("publicLibCount", publicLibCount);

            libraryQuery.setType(1);
            long publicIrtCount = libraryService.count(libraryQuery);
            map.put("publicIrtCount", publicIrtCount);


            // 最后标记成功
            status = 0;
        } while (false);


        // 返回结果
        map.put("status", status);

        // 返回数据
        JSONObject json = new JSONObject(map);
        return json.toString();

    }

    /**
     * 需要有Admin权限才可以执行注册功能
     *
     * @param model
     * @return
     */
    @RequiresRoles({"admin"})
    @RequestMapping("/register")
    String register(Model model, UserDO user) {
        return "home";
    }

    @RequiresRoles({"admin"})
    @RequestMapping("/init")
    String init(Model model) {
        logger.info("Register");
        UserDO userDO = new UserDO();
        userDO.setUsername("Admin");
        userDO.setEmail("lumiaoshan@westlake.edu.cn");
        userDO.setNick("propro");
        String randomSalt = new SecureRandomNumberGenerator().nextBytes().toHex();
        String result = new Md5Hash("propro", randomSalt, 3).toString();
        userDO.setSalt(randomSalt);
        userDO.setPassword(result);
        Set<String> roles = new HashSet<>();
        roles.add("admin");
        userDO.setRoles(roles);
        userDO.setTelephone("13185022599");
        userDO.setOrganization("Westlake University");
        userService.register(userDO);

        return "home";
    }

}
