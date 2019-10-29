package com.westlake.air.propro.domain.db.simple;

import lombok.Data;

@Data
public class FdrInfo {

    /**
     * 参考 AnalyseDataDO dataRef字段
     */
    String dataRef;

    String peptideRef;

    Boolean isDecoy;

    Double fdr;

    Double qValue;

    //最终选出的最佳峰
    Double bestRt;

    //最终定量结果
    Double intensitySum;

}
