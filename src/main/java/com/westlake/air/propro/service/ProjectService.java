package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.query.ProjectQuery;

import java.util.List;

public interface ProjectService {

    ResultDO<List<ProjectDO>> getList(ProjectQuery query);

    List<ProjectDO> getAll(ProjectQuery query);

    ResultDO insert(ProjectDO project);

    ResultDO update(ProjectDO projectDO);

    ResultDO delete(String id);

    ProjectDO getById(String id);

    ProjectDO getByName(String name);

    long count(ProjectQuery query);
}
