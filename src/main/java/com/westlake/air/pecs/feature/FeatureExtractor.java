package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureByPep;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.rtnormalizer.RtNormalizerScorer;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.TaskService;
import com.westlake.air.pecs.service.PeptideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-20 15:47
 */
@Component("featureExtractor")
public class FeatureExtractor {

    public final Logger logger = LoggerFactory.getLogger(FeatureExtractor.class);

    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    ChromatogramPicker chromatogramPicker;
    @Autowired
    FeatureFinder featureFinder;
    @Autowired
    RtNormalizerScorer RTNormalizerScorer;
    @Autowired
    TaskService taskService;
    @Autowired
    ChromatogramFilter chromatogramFilter;

    public FeatureByPep getExperimentFeature(AnalyseDataDO dataDO, IntensityGroup intensityGroupByPep, SigmaSpacing sigmaSpacing) {
        boolean featureFound = true;
        if (dataDO.getIntensityMap() == null || dataDO.getIntensityMap().size() == 0) {
            featureFound = false;
        }

        List<RtIntensityPairsDouble> rtIntensityPairsOriginList = new ArrayList<>();
        List<RtIntensityPairsDouble> maxRtIntensityPairsList = new ArrayList<>();
        List<IntensityRtLeftRtRightPairs> intensityRtLeftRtRightPairsList = new ArrayList<>();

        List<Double> libraryIntensityList = new ArrayList<>();
        //得到标准库中peptideRef对应的碎片和强度的键值对
        HashMap<String, Float> intensityMap = intensityGroupByPep.getIntensityMap();

        //对每一个chromatogram进行运算,dataDO中不含有ms1
        List<double[]> noise1000List = new ArrayList<>();
        for (String cutInfo : intensityMap.keySet()) {
            //获取对应的卷积数据
            Float[] intensityArray = dataDO.getIntensityMap().get(cutInfo);
            //如果没有卷积到信号,dataDO为null
            if (!dataDO.getIsHit() || intensityArray == null) {
                continue;
            }

            //得到卷积后的chromatogram的RT,Intensity对
            RtIntensityPairsDouble rtIntensityPairsOrigin = new RtIntensityPairsDouble(dataDO.getRtArray(), intensityArray);

            //进行高斯平滑,得到平滑后的chromatogram
            RtIntensityPairsDouble rtIntensityPairsAfterSmooth = gaussFilter.filter(rtIntensityPairsOrigin, sigmaSpacing);
            //计算两个信噪比
            //@Nico parameter configured
            //TODO legacy or corrected noise1000 is not the same
            double[] noises200 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 200, 30);
//            double[] noises1000 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 1000, 30);
            double[] noisesOri1000 = signalToNoiseEstimator.computeSTN(rtIntensityPairsOrigin, 1000, 30);
            //根据信噪比和峰值形状选择最高峰
            RtIntensityPairsDouble maxPeakPairs = peakPicker.pickMaxPeak(rtIntensityPairsAfterSmooth, noises200);

            //根据信噪比和最高峰选择谱图
            IntensityRtLeftRtRightPairs intensityRtLeftRtRightPairs = chromatogramPicker.pickChromatogram(rtIntensityPairsOrigin, rtIntensityPairsAfterSmooth, noisesOri1000, maxPeakPairs);
            rtIntensityPairsOriginList.add(rtIntensityPairsOrigin);
            maxRtIntensityPairsList.add(maxPeakPairs);
            intensityRtLeftRtRightPairsList.add(intensityRtLeftRtRightPairs);
            libraryIntensityList.add(Double.parseDouble(Float.toString(intensityMap.get(cutInfo))));
            noise1000List.add(noisesOri1000);
        }
        if (rtIntensityPairsOriginList.size() == 0) {
            featureFound = false;
        }
        List<List<ExperimentFeature>> experimentFeatures = featureFinder.findFeatures(rtIntensityPairsOriginList, maxRtIntensityPairsList, intensityRtLeftRtRightPairsList);

        FeatureByPep featureResult = new FeatureByPep();
        featureResult.setFeatureFound(featureFound);
        featureResult.setExperimentFeatures(experimentFeatures);
        featureResult.setLibraryIntensityList(libraryIntensityList);
        featureResult.setRtIntensityPairsOriginList(rtIntensityPairsOriginList);
        featureResult.setNoise1000List(noise1000List);

        return featureResult;
    }

    /**
     * get intensityGroup corresponding to peptideRef
     *
     * @param intensityGroupList intensity group of all peptides
     * @param peptideRef         chosen peptide
     * @return intensity group of peptideRef
     */
    private IntensityGroup getIntensityGroupByPep(List<IntensityGroup> intensityGroupList, String peptideRef) {
        for (IntensityGroup intensityGroup : intensityGroupList) {
            if (intensityGroup.getPeptideRef().equals(peptideRef)) {
                return intensityGroup;
            }
        }
        System.out.println("GetIntensityGroupByPep Error.");
        return null;
    }
}
