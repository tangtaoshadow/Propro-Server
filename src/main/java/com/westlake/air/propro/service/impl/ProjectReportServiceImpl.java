package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.dao.ProjectReportDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.db.ProjectReportDO;
import com.westlake.air.propro.domain.db.simple.SimpleProjectReport;
import com.westlake.air.propro.domain.query.ProjectReportQuery;
import com.westlake.air.propro.service.ProjectReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("projectReportService")
public class ProjectReportServiceImpl implements ProjectReportService {

    public final Logger logger = LoggerFactory.getLogger(ProjectReportServiceImpl.class);

    @Autowired
    ProjectReportDAO projectReportDAO;

    @Override
    public ResultDO<List<ProjectReportDO>> getList(ProjectReportQuery query) {
        List<ProjectReportDO> projectReports = projectReportDAO.getList(query);
        long totalCount = projectReportDAO.count(query);
        ResultDO<List<ProjectReportDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(projectReports);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public List<ProjectReportDO> getAll(String projectId) {
        ProjectReportQuery query = new ProjectReportQuery(projectId);
        return projectReportDAO.getAll(query);
    }

    @Override
    public List<SimpleProjectReport> getSimpleAll(String projectId) {
        return projectReportDAO.getSimpleAll(projectId);
    }

    @Override
    public ResultDO insert(ProjectReportDO projectReport) {
        if (projectReport.getProjectId() == null || projectReport.getProjectId().isEmpty()) {
            return ResultDO.buildError(ResultCode.PROJECT_ID_CANNOT_BE_EMPTY);
        }
        if (projectReport.getProjectName() == null || projectReport.getProjectName().isEmpty()) {
            return ResultDO.buildError(ResultCode.PROJECT_NAME_CANNOT_BE_EMPTY);
        }
        try {
            projectReport.setCreateDate(new Date());
            projectReport.setLastModifiedDate(new Date());
            projectReportDAO.insert(projectReport);
            return ResultDO.build(projectReport);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO update(ProjectReportDO projectReport) {
        if (projectReport.getId() == null || projectReport.getId().isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try {
            projectReport.setLastModifiedDate(new Date());
            projectReportDAO.update(projectReport);
            return ResultDO.build(projectReport);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.UPDATE_ERROR);
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            projectReportDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ProjectReportDO getById(String id) {
        try {
            ProjectReportDO projectReport = projectReportDAO.getById(id);
            return projectReport;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        }
    }

    @Override
    public long count(ProjectReportQuery query) {
        return projectReportDAO.count(query);
    }
}
