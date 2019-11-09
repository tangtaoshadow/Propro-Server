package com.westlake.air.propro.algorithm.merger;

import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.simple.FdrInfo;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.utils.ArrayUtil;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将多个实验的实验结果按照算法进行合并处理
 * Created by Nico Wang
 * Time: 2019-03-25 16:08
 */

@Component
public class ProjectMerger {

    public final Logger logger = LoggerFactory.getLogger(ProjectMerger.class);

    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ExperimentService experimentService;

    public HashMap<String, Integer> parameterEstimation(List<String> analyseOverviewIdList, double peakGroupFdr) {

        // 1) get peptide matrix
        HashMap<String, HashMap<String, FdrInfo>> peptideMatrix = getMatrix(analyseOverviewIdList, peakGroupFdr);

        // 2) get peptide fdr by peptideAllRun fdr
        double decoyRatio = getDecoyRatio(peptideMatrix);

        double peptideFdrCalculated = findPeptideFdr(peptideMatrix, decoyRatio, 0);
        if (peptideFdrCalculated > peakGroupFdr) {
            peptideMatrix = getMatrix(analyseOverviewIdList, peptideFdrCalculated);
        }

        double alignedFdr;
        if (peptideFdrCalculated < peakGroupFdr) {
            alignedFdr = peakGroupFdr;
        } else {
            alignedFdr = 2 * peptideFdrCalculated;
        }

        // 3) peptide select and count
        HashMap<String, Integer> pepRefMap = countSelectedPeptideRef(peptideMatrix, alignedFdr);
        return pepRefMap;
    }

    public List<FdrInfo> getSelectedPeptideMatrix(List<String> analyseOverviewIdList, double peakGroupFdr){

        // 1) get peptide matrix
        HashMap<String, HashMap<String, FdrInfo>> peptideMatrix = getMatrix(analyseOverviewIdList, peakGroupFdr);

        // 2) get peptide fdr by peptideAllRun fdr
        double decoyRatio = getDecoyRatio(peptideMatrix);

        double peptideFdrCalculated = findPeptideFdr(peptideMatrix, decoyRatio, 0);
        if (peptideFdrCalculated > peakGroupFdr) {
            peptideMatrix = getMatrix(analyseOverviewIdList, peptideFdrCalculated);
        }

        double alignedFdr;
        if (peptideFdrCalculated < peakGroupFdr) {
            alignedFdr = peakGroupFdr;
        } else {
            alignedFdr = 2 * peptideFdrCalculated;
        }

        return getSelectedPeptideList(peptideMatrix, alignedFdr);
    }
    /**
     * 将OverviewList中的所有分析结果以 HashMap<String, HashMap<String, AnalyseDataDO>> 的内存结构保留
     * Key为DECOY_PeptideRef或者是PeptideRef(前者为伪肽段,后者为真实肽段)
     * Value为另外一个Map,这个Map的Key是AnalyseOverviewId
     * TODO: 应该直接使用SQL语句进行统计合并,而不是在内存中进行归并  --陆妙善
     * @param analyseOverviewIdList
     * @param fdrLimit
     * @return
     */
    private HashMap<String, HashMap<String, FdrInfo>> getMatrix(List<String> analyseOverviewIdList, double fdrLimit) {
        HashMap<String, HashMap<String, FdrInfo>> peptideMatrix = new HashMap<>();
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setQValueEnd(fdrLimit);
        for (String overviewId : analyseOverviewIdList) {
            query.setOverviewId(overviewId);
            List<FdrInfo> fdrInfoList = analyseDataService.getAllFdrInfo(query);
            for (FdrInfo fdrInfo : fdrInfoList) {
                if (fdrInfo.getIsDecoy()) {
                    if (peptideMatrix.containsKey(Constants.DECOY_PREFIX + fdrInfo.getPeptideRef())) {
                        peptideMatrix.get(Constants.DECOY_PREFIX + fdrInfo.getPeptideRef()).put(overviewId, fdrInfo);
                    } else {
                        HashMap<String, FdrInfo> runMap = new HashMap<>();
                        runMap.put(overviewId, fdrInfo);
                        peptideMatrix.put(Constants.DECOY_PREFIX + fdrInfo.getPeptideRef(), runMap);
                    }
                } else {
                    if (peptideMatrix.containsKey(fdrInfo.getPeptideRef())) {
                        peptideMatrix.get(fdrInfo.getPeptideRef()).put(overviewId, fdrInfo);
                    } else {
                        HashMap<String, FdrInfo> runMap = new HashMap<>();
                        runMap.put(overviewId, fdrInfo);
                        peptideMatrix.put(fdrInfo.getPeptideRef(), runMap);
                    }
                }
            }
        }
        return peptideMatrix;
    }

    /**
     * 统计所有的实验分析结果中伪肽段的数目占总体数目的比例
     *
     * @param peptideMatrix
     * @return Decoy/(Decoy+Target)
     */
    private float getDecoyRatio(HashMap<String, HashMap<String, FdrInfo>> peptideMatrix) {
        Long targetCount = 0L, decoyCount = 0L;
        //统计多个分析结果中真伪肽段的数目,通过统计Key值中是否包含DECOY_特征字符来统计真伪肽段数目,提升速度.--陆妙善
        for (String peptideRef : peptideMatrix.keySet()) {
            if(peptideRef.startsWith(Constants.DECOY_PREFIX)){
                decoyCount+=peptideMatrix.get(peptideRef).size();
            }else{
                targetCount+=peptideMatrix.get(peptideRef).size();
            }
        }

        //返回伪肽段所占的比例
        return (float) decoyCount / (targetCount + decoyCount);
    }

    /**
     * 统计所有的实验分析结果中符合FDR条件的伪肽段的数目占符合FDR条件的总体数目的比例
     * 多次实验中只要有一个肽段的FDR小于指定值那么就认为该肽段为鉴定成功的肽段
     * 相当于同时增加了Decoy和Target的数目
     * @param peptideMatrix
     * @param fdr
     * @return Decoy/(Decoy+Target)
     */
    private double getSelectedUniqueDecoyRatio(HashMap<String, HashMap<String, FdrInfo>> peptideMatrix, double fdr) {

        Long decoyCount = 0L, targetCount = 0L;
        for (HashMap<String, FdrInfo> map : peptideMatrix.values()) {
            for (FdrInfo fdrInfo : map.values()) {
                if (fdrInfo.getQValue() < fdr) {
                    if (fdrInfo.getIsDecoy()) {
                        decoyCount++;
                    } else {
                        targetCount++;
                    }
                    break;
                }
            }
        }
        return (double) decoyCount / (targetCount + decoyCount);
    }

    /**
     *
     * @param peptideMatrix
     * @param decoyRatio
     * @param recursion
     * @return
     */
    private double findPeptideFdr(HashMap<String, HashMap<String, FdrInfo>> peptideMatrix, double decoyRatio, int recursion) {
        double startFdr = 0.0005d / Math.pow(10, recursion);
        double endFdr = 0.01d / Math.pow(10, recursion);
        double step = startFdr;
        double decoyRatio001 = getSelectedUniqueDecoyRatio(peptideMatrix, startFdr + step);
        double decoyRatio01 = getSelectedUniqueDecoyRatio(peptideMatrix, endFdr);
        //如果还没有进行递归,并且合并后的FDR值与累加FDR值几乎一样(差的绝对值小于10^-6),那么我们直接将0.01最为最终的FDR值
        if (recursion == 0 && Math.abs(decoyRatio01 - decoyRatio) < 1e-6) {
            return endFdr;
        }
        //如果累加FDR值小于0.001, 那么进行递归,比如第一轮startFdr和endFdr同时除以10,变为0.0005和0.001, step为0.0005
        if (decoyRatio < decoyRatio001) {
            return findPeptideFdr(peptideMatrix, decoyRatio, recursion + 1);
        }
        //如果累加FDR值大于0.01,即累加后伪肽段增多
        if (decoyRatio > decoyRatio01) {
            startFdr = 0.005d;
            endFdr = 1d;
            step = 0.005d;
        }
        double prevFrac = 0d, tempFrac = 0d;
        double tempFdr = 0d;
        for (double fdr = startFdr; fdr <= endFdr + step; fdr += step) {
            tempFrac = getSelectedUniqueDecoyRatio(peptideMatrix, fdr);
            tempFdr = fdr;
            if (tempFrac > recursion) {
                break;
            }
            if (Math.abs(tempFrac - recursion) < 1e-6) {
                break;
            }
            prevFrac = tempFrac;
        }
        return tempFdr - step * (tempFrac - recursion) / (tempFrac - prevFrac);
    }

    private String detemineBestRun(List<String> analyseOverviewIdList, double bestRunFdr) {
        Long maxCount = -1L;
        String maxOverviewId = "";
        for (String overviewId : analyseOverviewIdList) {
            AnalyseDataQuery query = new AnalyseDataQuery();
            query.setOverviewId(overviewId);
            query.setQValueEnd(bestRunFdr);
            Long tempCount = analyseDataService.count(query);
            if (tempCount > maxCount) {
                maxCount = tempCount;
                maxOverviewId = overviewId;
            }
        }
        return maxOverviewId;
    }

    /**
     * 1. align slave runs' rt to master rt
     * 2. return median of std(rt) within all runs
     *
     * @param peptideMatrix
     * @param analyseOverviewIdList
     * @param masterOverviewId
     * @return median of std("aligned slave rt" and "original master rt") in all slave runs
     */
    private double alignAndGetStd(HashMap<String, HashMap<String, AnalyseDataDO>> peptideMatrix, List<String> analyseOverviewIdList, String masterOverviewId) {

        List<Double> stdList = new ArrayList<>();
        for (String slaveOverviewId : analyseOverviewIdList) {
            if (slaveOverviewId.equals(masterOverviewId)) {
                continue;
            }

            List<Double> masterList = new ArrayList<>();
            List<Double> slaveList = new ArrayList<>();
            for (HashMap<String, AnalyseDataDO> peptide : peptideMatrix.values()) {
                if (peptide.get(masterOverviewId) != null && peptide.get(slaveOverviewId) != null) {
                    masterList.add(peptide.get(masterOverviewId).getBestRt());
                    slaveList.add(peptide.get(slaveOverviewId).getBestRt());
                }
            }
            int[] index = ArrayUtil.indexAfterSort(masterList);
            double[] masterRtArray = new double[masterList.size()];
            double[] slaveRtArray = new double[slaveList.size()];
            for (int i = 0; i < masterRtArray.length; i++) {
                masterRtArray[i] = masterList.get(index[i]);
                slaveRtArray[i] = slaveList.get(index[i]);
            }
            LoessInterpolator loess = new LoessInterpolator(0.01, 10);
            double[] smoothedMasterRt = loess.smooth(slaveRtArray, masterRtArray);
            PolynomialSplineFunction function = new LinearInterpolator().interpolate(slaveRtArray, smoothedMasterRt);
            double squareSum = 0d;
            int count = 0;
            for (HashMap<String, AnalyseDataDO> peptide : peptideMatrix.values()) {
                if (peptide.containsKey(slaveOverviewId) && function.isValidPoint(peptide.get(slaveOverviewId).getBestRt())) {
                    double slaveAlignedRt = function.value(peptide.get(slaveOverviewId).getBestRt());
                    peptide.get(slaveOverviewId).setBestRt(slaveAlignedRt);
                    if (peptide.containsKey(masterOverviewId)) {
                        double masterRt = peptide.get(masterOverviewId).getBestRt();
                        squareSum += (masterRt - slaveAlignedRt) * (masterRt - slaveAlignedRt);
                        count++;
                    }
                }
            }

            double std = Math.sqrt(squareSum / count);
            stdList.add(std);
        }
        Collections.sort(stdList);
        return stdList.get(stdList.size() / 2);
    }

    /**
     *
     * @param peptideMatrix
     * @param alignedFdr
     * @return
     */
    private HashMap<String, Integer> countSelectedPeptideRef(HashMap<String, HashMap<String, FdrInfo>> peptideMatrix, double alignedFdr) {
        HashMap<String, Integer> pepRefMap = new HashMap<>();
        for (String peptideRef : peptideMatrix.keySet()) {
            for (FdrInfo peptideRun : peptideMatrix.get(peptideRef).values()) {
                if (peptideRun.getQValue() < alignedFdr) {
                    if (pepRefMap.containsKey(peptideRef)) {
                        pepRefMap.put(peptideRef, pepRefMap.get(peptideRef) + 1);
                    } else {
                        pepRefMap.put(peptideRef, 1);
                    }
                }
            }
        }
        return pepRefMap;
    }

    /**
     *
     * @param peptideMatrix
     * @param alignedFdr
     * @return
     */
    private List<FdrInfo> getSelectedPeptideList(HashMap<String, HashMap<String, FdrInfo>> peptideMatrix, double alignedFdr) {
        List<FdrInfo> fdrInfos = new ArrayList<>();
        for (String peptideRef : peptideMatrix.keySet()) {
            for (FdrInfo peptideRun : peptideMatrix.get(peptideRef).values()) {
                if (peptideRun.getQValue() < alignedFdr) {
                    fdrInfos.add(peptideRun);
                    break;
                }
            }
        }
        return fdrInfos;
    }
}
