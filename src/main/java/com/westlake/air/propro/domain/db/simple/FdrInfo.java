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

}
