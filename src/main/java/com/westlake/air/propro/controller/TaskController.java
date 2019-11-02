package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.westlake.air.propro.constants.enums.TaskStatus;
import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.TaskQuery;
import com.westlake.air.propro.service.TaskService;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:33
 */
@RestController
@RequestMapping("task")
public class TaskController extends BaseController {

    @Autowired
    TaskService taskService;


    /***
     * @UpdateTime 2019-9-9 16:29:53
     * @Archive 任务列表
     * @param currentPage
     * @param pageSize
     * @param taskTemplate
     * @param taskStatus
     * @return
     */
    @PostMapping(value = "/list")
    String taskList(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "30") Integer pageSize,
            @RequestParam(value = "taskTemplate", required = false) String taskTemplate,
            @RequestParam(value = "taskStatus", required = false) String taskStatus) {


        // 执行状态
        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        // 数据打包
        Map<String, Object> data = new HashMap<String, Object>();

        do {

            if (taskTemplate != null) {
                data.put("taskTemplate", taskTemplate);
            }

            data.put("taskTemplates", TaskTemplate.values());

            if (taskStatus != null) {
                data.put("taskStatus", taskStatus);
            }

            data.put("statusList", TaskStatus.values());

            data.put("pageSize", pageSize);
            TaskQuery query = new TaskQuery();
            if (taskTemplate != null && !taskTemplate.isEmpty() && !taskTemplate.equals("All")) {
                query.setTaskTemplate(taskTemplate);
            }

            if (taskStatus != null && !taskStatus.isEmpty() && !taskStatus.equals("All")) {
                query.setStatus(taskStatus);
            }

            if (!isAdmin()) {
                query.setCreator(getCurrentUsername());
            }

            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            query.setSortColumn("createDate");
            query.setOrderBy(Sort.Direction.DESC);
            ResultDO<List<TaskDO>> resultDO = taskService.getList(query);

            data.put("tasks", resultDO.getModel());
            data.put("totalPage", resultDO.getTotalPage());
            // 得出全部记录数
            data.put("totalNumbers", resultDO.getTotalNum());


            data.put("currentPage", currentPage);

            status = 0;

        } while (false);


        map.put("data", data);
        map.put("status", status);

        // 返回数据

        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @UpdateTime 2019-9-10 09:20:56
     * @Archive 查询任务id的详情
     * @param id 任务id
     * @return
     */
    @PostMapping(value = "/detail")
    String detail(@RequestParam("taskId") String id) {

        // 状态标记
        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        do {
            ResultDO<TaskDO> resultDO = taskService.getById(id);
            if (resultDO.isFailed()) {
                // 任务查询失败
                status = -3;
                break;
            }

            PermissionUtil.check(resultDO.getModel());
            TaskDO taskDO = resultDO.getModel();
            map.put("task", taskDO);

            TaskTemplate taskTemplate = TaskTemplate.getByName(taskDO.getTaskTemplate());
            if (taskTemplate == null) {
                // 对象不存在
                status = -4;
                break;
            }

            // 成功状态
            status = 0;
        } while (false);

        // 返回状态
        map.put("status", status);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    @PostMapping(value = "/getTaskInfo/{id}")
    String getTaskInfo(Model model, @PathVariable("id") String id) {
        ResultDO<TaskDO> resultDO = taskService.getById(id);
        if (resultDO.isSuccess() && resultDO.getModel() != null) {
            PermissionUtil.check(resultDO.getModel());
            return JSON.toJSONString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(resultDO.getModel().getLastModifiedDate()));
        } else {
            return null;
        }
    }


    @PostMapping(value = "/delete")
    String delete(@RequestParam("id") String id) {

        // 执行状态
        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        ResultDO<TaskDO> resultDO = taskService.getById(id);
        PermissionUtil.check(resultDO.getModel());

        taskService.delete(id);

        status = 0;
        map.put("status", status);

        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


}
