package com.westlake.air.propro.domain.bean.analyse;

import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.simple.MatchedPeptide;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class ComparisonResult {

    /**
     * 各组间均相同的肽段列表
     */
    List<MatchedPeptide> samePeptides;

    /**
     * 各组间不相同的肽段列表
     */
    List<MatchedPeptide> diffPeptides;

    /**
     * 每一组对于diffPeptides的鉴定结果,其size与diffPeptides.size是相同的
     * key为分析ID.
     */
    HashMap<AnalyseOverviewDO, List<Boolean>> identifiesMap;
}
