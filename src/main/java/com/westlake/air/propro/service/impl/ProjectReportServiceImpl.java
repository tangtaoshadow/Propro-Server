package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.algorithm.merger.ProjectMerger;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.dao.ProjectReportDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.db.ProjectReportDO;
import com.westlake.air.propro.domain.db.simple.FdrInfo;
import com.westlake.air.propro.domain.db.simple.SimpleExperiment;
import com.westlake.air.propro.domain.db.simple.SimpleProjectReport;
import com.westlake.air.propro.domain.query.ProjectReportQuery;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.ProjectReportService;
import com.westlake.air.propro.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service("projectReportService")
public class ProjectReportServiceImpl implements ProjectReportService {

    public final Logger logger = LoggerFactory.getLogger(ProjectReportServiceImpl.class);

    @Autowired
    ProjectReportDAO projectReportDAO;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ProjectMerger projectMerger;
    @Autowired
    ProjectService projectService;

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
    public ResultDO insertAll(List<ProjectReportDO> projectReports) {
        if (projectReports == null || projectReports.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            for(ProjectReportDO reportDO : projectReports){
                reportDO.setCreateDate(new Date());
                reportDO.setLastModifiedDate(new Date());
            }
            projectReportDAO.insert(projectReports);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.error(e.getMessage());
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

    /**
     * 默认按照batchName进行聚合,生成多个ProjectReport
     *
     * @param projectId
     */
    @Override
    public List<ProjectReportDO> generateReport(String projectId) {
        List<ProjectReportDO> reports = new ArrayList<>();
        ProjectDO project = projectService.getById(projectId);
        List<SimpleExperiment> experimentList = experimentService.getAllSimpleExperimentByProjectId(project.getId());
        HashMap<String, List<String>> batchNameMap = new HashMap<>();
        for (SimpleExperiment se : experimentList) {
            if (batchNameMap.containsKey(se.getBatchName())) {
                batchNameMap.get(se.getBatchName()).add(se.getId());
            } else {
                List<String> expIds = new ArrayList<>();
                expIds.add(se.getId());
                batchNameMap.put(se.getBatchName(), expIds);
            }
        }

        for (String batchName : batchNameMap.keySet()) {
            ProjectReportDO reportDO = generateReport(project, batchNameMap.get(batchName));
            reportDO.setBatchName(batchName);
            reports.add(reportDO);
        }
        return reports;
    }

    /**
     * 根据选定的expId进行合并,生成对应的ProjectReport.此时对应的BatchName字段为空
     *
     * @param project
     * @param selectedExpIds
     */
    @Override
    public ProjectReportDO generateReport(ProjectDO project, List<String> selectedExpIds) {
        List<SimpleExperiment> experimentList = experimentService.getAllSimpleExperimentByProjectId(project.getId());

        List<String> overviewIds = new ArrayList<>();
        HashMap<String, String> exps = new HashMap<>();
        for (SimpleExperiment simpleExp : experimentList) {
            if (selectedExpIds.contains(simpleExp.getId())) {
                //取每一个实验的第一个分析结果进行分析
                AnalyseOverviewDO analyseOverview = analyseOverviewService.getFirstAnalyseOverviewByExpId(simpleExp.getId());
                if (analyseOverview == null) {
                    continue;
                }
                overviewIds.add(analyseOverview.getId());
                exps.put(simpleExp.getId(), simpleExp.getName());
            }
        }

        List<FdrInfo> results = projectMerger.getSelectedPeptideMatrix(overviewIds, 0.01);
        List<String> dataRefs = new ArrayList<>();
        List<Double> bestRts = new ArrayList<>();
        List<Double> intensitySums = new ArrayList<>();
        List<String> proteinNames = new ArrayList<>();
        for (FdrInfo fi : results) {
            if (!fi.getIsDecoy()) {
                dataRefs.add(fi.getDataRef());
                bestRts.add(fi.getBestRt());
                intensitySums.add(fi.getIntensitySum());
                proteinNames.add(fi.getProteinName());
            }
        }

        ProjectReportDO reportDO = new ProjectReportDO();
        reportDO.setProjectId(project.getId());
        reportDO.setProjectName(project.getName());
        reportDO.setExps(exps);
        reportDO.setOverviewIds(overviewIds);
        reportDO.setDataRefs(dataRefs);
        reportDO.setIdentifiedNumber(dataRefs.size());
        reportDO.setIntensitySums(intensitySums);
        reportDO.setBestRts(bestRts);
        reportDO.setProteinNames(proteinNames);

        return reportDO;
    }
}
