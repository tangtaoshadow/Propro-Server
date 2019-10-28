package com.westlake.air.propro.domain.query;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class ProjectReportQuery extends PageQuery {

    String id;

    String projectId;

    public ProjectReportQuery(){}

    public ProjectReportQuery(String projectId){
        this.projectId = projectId;
    }

    public ProjectReportQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn){
        super(pageNo, pageSize, direction, sortColumn);
    }
}
