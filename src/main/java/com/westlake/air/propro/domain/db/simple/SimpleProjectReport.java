package com.westlake.air.propro.domain.db.simple;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
public class SimpleProjectReport {

    String id;

    String projectId;

    //冗余字段,项目名称
    String projectName;

    //统计使用的analyseOverviewId列表
    List<String> overviewIds;

    //统计使用的实验列表,key为实验的id,value为实验的名称
    HashMap<String, String> exps;

    //合并报告创建人
    String creator;

    //项目合并分析的创建日期
    Date createDate;

    //项目合并分析的最后修改日期
    Date lastModifiedDate;
}
