package com.westlake.air.propro.domain.db;

import com.westlake.air.propro.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
@Document(collection = "mergeReport")
public class ProjectReportDO extends BaseDO {

    private static final long serialVersionUID = -3267829839260853625L;

    @Id
    String id;

    @Indexed
    String projectId;

    //冗余字段,项目名称
    String projectName;

    //合并报告创建人
    String creator;

    //项目合并分析的创建日期
    Date createDate;

    //项目合并分析的最后修改日期
    Date lastModifiedDate;

    //归并时Experiment的BatchNo字段
    String batchName;

    //统计使用的analyseOverviewId列表
    List<String> overviewIds;

    //统计使用的实验列表,key为实验的id,value为实验的名称
    HashMap<String, String> exps;

    //最终鉴定到的肽段列表(真肽段)
    List<String> dataRefs;

    //最终鉴定到的肽段的数目
    Integer identifiedNumber;

    //最佳RT位置
    List<Double> bestRts;

    //最终定量结果
    List<Double> intensitySums;

    List<String> proteinNames;

}
