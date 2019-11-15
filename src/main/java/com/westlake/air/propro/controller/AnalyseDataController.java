package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONArray;
import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.feature.ChromatographicScorer;
import com.westlake.air.propro.algorithm.feature.LibraryScorer;
import com.westlake.air.propro.algorithm.formula.FormulaCalculator;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.algorithm.peak.FeatureExtractor;
import com.westlake.air.propro.algorithm.peak.GaussFilter;
import com.westlake.air.propro.algorithm.peak.SignalToNoiseEstimator;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResidueType;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.params.ExtractParams;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.FeatureUtil;
import com.westlake.air.propro.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:50
 */
@Controller
@RequestMapping("analyse")
public class AnalyseDataController extends BaseController {

    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    SwathIndexService swathIndexService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    ChromatographicScorer chromatographicScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    Extractor extractor;
    @Autowired
    FormulaCalculator formulaCalculator;

    @RequestMapping(value = "/data/list")
    String list(Model model,
                    @RequestParam(value = "overviewId", required = true) String overviewId,
                    @RequestParam(value = "peptideRef", required = false) String peptideRef,
                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                    RedirectAttributes redirectAttributes) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("peptideRef", peptideRef);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);

        PermissionUtil.check(overviewResult.getModel());
        model.addAttribute("overview", overviewResult.getModel());

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);

        if (StringUtils.isNotEmpty(peptideRef)) {
            query.setPeptideRef(peptideRef);
        }
        query.setOverviewId(overviewId);
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);
        List<AnalyseDataDO> datas = resultDO.getModel();
        model.addAttribute("datas", datas);
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());

        return "analyse/data/list";
    }

    @RequestMapping(value = "/clinic")
    String clinic(Model model,
                  @RequestParam(value = "dataId", required = false) String dataId,
                  @RequestParam(value = "peptideRef", required = false) String peptideRef,
                  @RequestParam(value = "expId", required = false) String expId,
                  @RequestParam(value = "libraryId", required = false) String libraryId,
                  @RequestParam(value = "mzExtractWindow", required = false, defaultValue = "0.03") Float mzExtractWindow,
                  @RequestParam(value = "rtExtractWindow", required = false, defaultValue = "800") Float rtExtractWindow,
                  @RequestParam(value = "minLength", required = false, defaultValue = "3") Integer minLength,
                  @RequestParam(value = "maxCharge", required = false, defaultValue = "3") Integer maxCharge,
                  @RequestParam(value = "onlyLib", required = false, defaultValue = "false") Boolean onlyLib,
                  HttpServletRequest request) {
        model.addAttribute("mzExtractWindow", mzExtractWindow);
        model.addAttribute("rtExtractWindow", rtExtractWindow);
        model.addAttribute("minLength", minLength);
        model.addAttribute("maxCharge", maxCharge);
        model.addAttribute("onlyLib", onlyLib);

        /**
         * Step1. Prepare for dataId query.
         * 如果dataId不为空,那么可以根据dataId获取所有的相关信息.并且将这些信息全部填充入map中,用于在前端显示,
         * 如果dataId为空并且expId也为空,那么返回错误信息
         */
        AnalyseDataDO data = null;
        if (StringUtils.isNotEmpty(dataId)) {
            data = analyseDataService.getById(dataId).getModel();
            AnalyseOverviewDO analyseOverview = analyseOverviewService.getById(data.getOverviewId()).getModel();
            libraryId = analyseOverview.getLibraryId();
            peptideRef = data.getPeptideRef();
            expId = analyseOverview.getExpId();
            List<Double> leftRtList = new ArrayList<>();
            List<Double> rightRtList = new ArrayList<>();
            for (FeatureScores scores : data.getFeatureScoresList()) {
                Pair<Double, Double> rtRange = FeatureUtil.toDoublePair(scores.getRtRangeFeature());
                leftRtList.add(rtRange.getLeft());
                rightRtList.add(rtRange.getRight());
            }
            model.addAttribute("leftRtList", leftRtList);
            model.addAttribute("rightRtList", rightRtList);
            if (data.getBestRt() != null) {
                model.addAttribute("bestRt", (double) Math.round(data.getBestRt() * 100) / 100);
            }
        }
        model.addAttribute("expId", expId);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("peptideRef", peptideRef);

        /**
         * Step2. Check for required info and visit permission
         * 如果Experiment对象没有做过irt校准,那么rtExtractorWindow强制切换为-1
         */
        if (StringUtils.isEmpty(expId)) {
            model.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED.getMessage());
            return "analyse/data/clinic";
        }
        if (StringUtils.isEmpty(peptideRef)) {
            model.addAttribute(ERROR_MSG, ResultCode.PEPTIDE_REF_CANNOT_BE_EMPTY.getMessage());
            return "analyse/data/clinic";
        }
        ExperimentDO experiment = experimentService.getById(expId).getModel();
        PermissionUtil.check(experiment);
        if (experiment.getIrtResult() == null) {
            model.addAttribute("rtExtractWindow", -1f);
            rtExtractWindow = -1f;
        }
        model.addAttribute("exp", experiment);

        /**
         * Step3. Generate RealTime Peptide by peptideRef. For fragmentInfo length is big or equal than 3.
         */
        PeptideDO buildPeptide = null;
        if (!onlyLib) {//如果仅查询标准库,那么不构建全新的肽段
            List<Integer> charges = new ArrayList<>();
            for (int i = 1; i <= maxCharge; i++) {
                charges.add(i);
            }
            model.addAttribute("charges", charges);
            buildPeptide = peptideService.buildWithPeptideRef(peptideRef, minLength, ResidueType.abcxyz, charges);
        }

        /**
         * Step4. Merge with library Peptide if existed
         */
        List<String> libraryCutInfos = new ArrayList<>();
        if (libraryId != null) {
            PeptideDO libraryPeptide = peptideService.getByLibraryIdAndPeptideRef(libraryId, peptideRef);
            if (libraryPeptide != null) {
                if (onlyLib) {
                    buildPeptide = libraryPeptide;
                } else {
                    buildPeptide.setLibraryId(libraryPeptide.getLibraryId());
                    buildPeptide.setRt(libraryPeptide.getRt());
                    for (FragmentInfo fi : libraryPeptide.getFragmentMap().values()) {
                        FragmentInfo buildFi = buildPeptide.getFragmentMap().get(fi.getCutInfo());
                        if (buildFi != null) {
                            buildFi.setIntensity(fi.getIntensity());
                            libraryCutInfos.add(fi.getCutInfo());
                        }

                    }
                }
            } else {
                List<Integer> charges = new ArrayList<>();
                for (int i = 1; i <= maxCharge; i++) {
                    charges.add(i);
                }
                buildPeptide = peptideService.buildWithPeptideRef(peptideRef, minLength, ResidueType.abcxyz, charges);
            }
        }
        model.addAttribute("libCutInfos", libraryCutInfos);
        model.addAttribute("peptide", buildPeptide);

        /**
         * Step5. Extract with Generated Peptide
         */
        ResultDO<AnalyseDataDO> realTimeAnalyseDataResult = extractor.extractOneOnRealTime(experiment, buildPeptide, new ExtractParams(mzExtractWindow, rtExtractWindow));
        if (realTimeAnalyseDataResult.isFailed()) {
            model.addAttribute(ERROR_MSG, realTimeAnalyseDataResult.getMsgInfo());
            return "analyse/data/clinic";
        }
        AnalyseDataDO targetAnalyseData = realTimeAnalyseDataResult.getModel();
        HashMap<String, Float> mzMap = targetAnalyseData.getMzMap();
        model.addAttribute("hitCutInfos", mzMap.keySet());

        /**
         * Step6. 准备输出结果
         */
        Float[] rtArray = targetAnalyseData.getRtArray();
        model.addAttribute("rt", rtArray);
        model.addAttribute("intensityMap", targetAnalyseData.getIntensityMap());
        model.addAttribute("mzMap", mzMap);
        return "analyse/data/clinic";
    }


    private JSONArray noise(Float[] pairRtArray, Float[] pairIntensityArray) {

        Double[] rts = new Double[pairRtArray.length];
        Double[] ints = new Double[pairIntensityArray.length];
        for (int i = 0; i < rts.length; i++) {
            rts[i] = Double.parseDouble(pairRtArray[i].toString());
            ints[i] = Double.parseDouble(pairIntensityArray[i].toString());
        }
        double[] noisePairIntensityArray = signalToNoiseEstimator.computeSTN(rts, ints, 1000, 30);
        JSONArray noiseIntensityArray = new JSONArray();
        for (int i = 0; i < noisePairIntensityArray.length; i++) {
            if (noisePairIntensityArray[i] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                noiseIntensityArray.add(pairIntensityArray[i]);
            } else {
                noiseIntensityArray.add(0);
            }
        }
        JSONArray intensityArrays = new JSONArray();
        intensityArrays.addAll(noiseIntensityArray);
        return intensityArrays;
    }

    private Float[] noise(Float[] pairRtArray, Float[] pairIntensityArray, Integer noise) {

        Double[] rts = new Double[pairRtArray.length];
        Double[] ints = new Double[pairIntensityArray.length];
        for (int i = 0; i < rts.length; i++) {
            rts[i] = Double.parseDouble(pairRtArray[i].toString());
            ints[i] = Double.parseDouble(pairIntensityArray[i].toString());
        }
        double[] noisePairIntensityArray = signalToNoiseEstimator.computeSTN(rts, ints, noise, 30);
        Float[] noiseIntensityArray = new Float[noisePairIntensityArray.length];
        for (int i = 0; i < noisePairIntensityArray.length; i++) {
            if (noisePairIntensityArray[i] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                noiseIntensityArray[i] = pairIntensityArray[i];
            } else {
                noiseIntensityArray[i] = 0f;
            }
        }

        return noiseIntensityArray;
    }
}
