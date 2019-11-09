package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.simple.SimplePeptide;
import com.westlake.air.propro.domain.params.WorkflowParams;

import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
public interface ScoreService {

    /**
     * 请确保调用本函数时传入的AnalyseDataDO已经解压缩
     * @param data
     * @param peptide
     * @param rtMap
     * @param input
     * @return
     */
    void scoreForOne(AnalyseDataDO data, SimplePeptide peptide, TreeMap<Float, MzIntensityPairs> rtMap, WorkflowParams input);

    /**
     * 仅Shape和Shape Weighted Score均高于99分的可以过
     * @param data
     * @param peptide
     * @param rtMap
     */
    void strictScoreForOne(AnalyseDataDO data, SimplePeptide peptide , TreeMap<Float, MzIntensityPairs> rtMap, float shapeScoreThreshold);
}
