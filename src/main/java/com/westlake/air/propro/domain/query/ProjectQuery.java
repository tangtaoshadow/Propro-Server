package com.westlake.air.propro.domain.query;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
public class ProjectQuery extends PageQuery {

    String id;

    String name;

    //项目负责人名称
    String ownerName;

    Boolean doPublic;

    public ProjectQuery(){}

    public ProjectQuery(String ownerName){
        this.ownerName = ownerName;
    }

    public ProjectQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn){
        super(pageNo, pageSize, direction, sortColumn);
    }
}
