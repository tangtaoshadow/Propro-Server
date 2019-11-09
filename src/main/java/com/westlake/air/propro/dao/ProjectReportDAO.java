package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.db.ProjectReportDO;
import com.westlake.air.propro.domain.db.simple.SimpleProjectReport;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.domain.query.ProjectReportQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ProjectReportDAO extends BaseDAO<ProjectReportDO, ProjectReportQuery> {

    public static String CollectionName = "projectReport";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class getDomainClass() {
        return ProjectReportDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(ProjectReportQuery projectReportQuery) {
        Query query = new Query();
        if (projectReportQuery.getId() != null) {
            query.addCriteria(where("id").is(projectReportQuery.getId()));
        }
        if (projectReportQuery.getProjectId() != null) {
            query.addCriteria(where("projectId").is(projectReportQuery.getProjectId()));
        }
        return query;
    }

    public List<SimpleProjectReport> getSimpleAll(String projectId) {
        ProjectReportQuery query = new ProjectReportQuery(projectId);
        return mongoTemplate.find(buildQueryWithoutPage(query), SimpleProjectReport.class, CollectionName);
    }
}
