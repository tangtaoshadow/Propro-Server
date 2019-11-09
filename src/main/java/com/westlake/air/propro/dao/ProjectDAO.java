package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.query.ProjectQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2019-01-10 13:16
 */
@Service
public class ProjectDAO extends BaseDAO<ProjectDO, ProjectQuery>{

    public static String CollectionName = "project";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class getDomainClass() {
        return ProjectDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(ProjectQuery projectQuery) {
        Query query = new Query();
        if (projectQuery.getId() != null) {
            query.addCriteria(where("id").is(projectQuery.getId()));
        }
        if (projectQuery.getName() != null) {
            query.addCriteria(where("name").is(projectQuery.getName()));
        }
        if (projectQuery.getOwnerName() != null) {
            query.addCriteria(where("ownerName").is(projectQuery.getOwnerName()));
        }
        if (projectQuery.getDoPublic() != null) {
            query.addCriteria(where("doPublic").is(projectQuery.getDoPublic()));
        }
        return query;
    }

    public ProjectDO getByName(String name) {
        ProjectQuery query = new ProjectQuery();
        query.setName(name);
        return mongoTemplate.findOne(buildQuery(query), ProjectDO.class, CollectionName);
    }
}
