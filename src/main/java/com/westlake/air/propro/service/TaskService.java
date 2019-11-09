package com.westlake.air.propro.service;

import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.TaskQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
public interface TaskService {

    Long count(TaskQuery query);

    List<TaskDO> getAll(TaskQuery targetQuery);

    ResultDO<List<TaskDO>> getList(TaskQuery targetQuery);

    ResultDO insert(TaskDO taskDO);

    ResultDO update(TaskDO taskDO);

    ResultDO update(TaskDO taskDO, String newLog);

    ResultDO update(TaskDO taskDO,String status, String newLog);

    ResultDO finish(TaskDO taskDO, String status, String newLog);

    ResultDO delete(String id);

    ResultDO<TaskDO> getById(String id);

    ResultDO doTask(TaskTemplate taskTemplate);

}
