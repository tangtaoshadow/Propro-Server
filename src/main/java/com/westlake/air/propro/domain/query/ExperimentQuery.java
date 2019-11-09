package com.westlake.air.propro.domain.query;

import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 21:16
 */
@Data
public class ExperimentQuery extends PageQuery {

    private static final long serialVersionUID = -3258829839160856645L;

    String id;

    String name;

    String projectId;

    String type;

    String ownerName;

    Date createDate;

    Date lastModifiedDate;

    public ExperimentQuery(){}

    public ExperimentQuery(String ownerName){
        this.ownerName = ownerName;
    }

    public ExperimentQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn){
        super(pageNo, pageSize, direction, sortColumn);
    }
}
