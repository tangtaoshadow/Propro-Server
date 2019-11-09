package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ProjectReportDO;
import com.westlake.air.propro.domain.db.simple.SimpleProjectReport;
import com.westlake.air.propro.domain.query.ProjectReportQuery;

import java.util.List;

public interface ProjectReportService {

    ResultDO<List<ProjectReportDO>> getList(ProjectReportQuery query);

    List<ProjectReportDO> getAll(String projectId);

    List<SimpleProjectReport> getSimpleAll(String projectId);

    ResultDO insert(ProjectReportDO projectReport);

    ResultDO update(ProjectReportDO projectReport);

    ResultDO delete(String id);

    ProjectReportDO getById(String id);

    long count(ProjectReportQuery query);
}
