package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
import com.westlake.air.propro.constants.enums.ScoreType;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.AnalyseDataRT;
import com.westlake.air.propro.domain.bean.analyse.ComparisonResult;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.params.ExtractParams;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.domain.query.AnalyseOverviewQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.FeatureUtil;
import com.westlake.air.propro.utils.PermissionUtil;
import com.westlake.air.propro.utils.RepositoryUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:50
 * Update by tangtao
 * UpdateTime：2019-9-25 10:27:35
 */
@RestController
@RequestMapping("analyse")
public class AnalyseController extends BaseController {

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

    /***
     * @UpdateTime 2019-9-13 15:40:18
     * @UpdateAuthor tangtao
     * @Archive 分析列表
     * @param expId
     * @param currentPage
     * @param pageSize
     * @return 成功 0 失败 <-1
     */
    @PostMapping(value = "/list")
    String overviewList(
            @RequestParam(value = "expId", required = false) String expId,
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "1000") Integer pageSize) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            data.put("pageSize", pageSize);
            data.put("expId", expId);
            ResultDO<ExperimentDO> expResult = null;
            if (StringUtils.isNotEmpty(expId)) {
                expResult = experimentService.getById(expId);
                if (expResult.isFailed()) {
                    // 出错了
                    data.put(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED);
                    status = -2;
                    break;
                }
                PermissionUtil.check(expResult.getModel());
                data.put("experiment", expResult.getModel());
            }

            AnalyseOverviewQuery query = new AnalyseOverviewQuery();
            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            if (StringUtils.isNotEmpty(expId)) {
                query.setExpId(expId);
            }
            if (!isAdmin()) {
                query.setOwnerName(getCurrentUsername());
            }
            query.setOrderBy(Sort.Direction.DESC);
            query.setSortColumn("createDate");
            ResultDO<List<AnalyseOverviewDO>> resultDO = analyseOverviewService.getList(query);
            data.put("overviews", resultDO.getModel());
            data.put("totalPage", resultDO.getTotalPage());
            data.put("TotalNumbers", resultDO.getTotalNum());
            data.put("currentPage", currentPage);
            data.put("scores", ScoreType.getUsedTypes());
            status = 0;
        } while (false);


        map.put("status", status);

        // 将数据再打包一次 简化前端数据处理逻辑
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    /***
     * @UpdateTime 2019-9-24 00:29:15
     * @UpdateAuthor tangtao https://www.promiselee.cn/tao
     * @param id 查询的分析详情 id
     * @return 成功 0
     */
    @PostMapping(value = "/detail")
    String detail(@RequestParam("id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(id);
            AnalyseOverviewDO overview = resultDO.getModel();
            PermissionUtil.check(resultDO.getModel());

            LibraryDO library = libraryService.getById(overview.getLibraryId());

            AnalyseDataQuery query = new AnalyseDataQuery(id);

            query.setIsDecoy(false);
            query.setFdrEnd(0.01);
            query.setPageSize(10000);
            List<AnalyseDataRT> rts = analyseDataService.getRtList(query);
            query.setFdrStart(0.01);
            query.setFdrEnd(1.0);
            List<AnalyseDataRT> badRts = analyseDataService.getRtList(query);
            data.put("library", library);
            data.put("rts", rts);
            data.put("badRts", badRts);
            data.put("overview", resultDO.getModel());
            data.put("slope", resultDO.getModel().getSlope());
            data.put("intercept", resultDO.getModel().getIntercept());
            data.put("targetMap", overview.getTargetDistributions());
            data.put("decoyMap", overview.getDecoyDistributions());

            status = 0;
        } while (false);

        map.put("status", status);

        // 将数据再打包一次 简化前端数据处理逻辑
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }

    @PostMapping(value = "/overview/export/{id}")
    String overviewExport(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) throws IOException {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(id);

        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED);
            return "redirect:/analyse/overview/list";
        }
        PermissionUtil.check(overviewResult.getModel());
        ResultDO<ExperimentDO> experimentResult = experimentService.getById(overviewResult.getModel().getExpId());
        if (experimentResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED);
            return "redirect:/analyse/overview/list";
        }
        ProjectDO project = projectService.getByName(experimentResult.getModel().getProjectName());
        if (project == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/analyse/overview/list";
        }

        int pageSize = 1000;
        AnalyseDataQuery query = new AnalyseDataQuery(id);
        query.setIsDecoy(false);
        query.setFdrEnd(0.01);
        query.setSortColumn("fdr");
        query.setOrderBy(Sort.Direction.ASC);
        int count = analyseDataService.count(query).intValue();
        int totalPage = count % pageSize == 0 ? count / pageSize : (count / pageSize + 1);

        String exportPath = RepositoryUtil.buildOutputPath(project.getName(), overviewResult.getModel().getName() + "[" + overviewResult.getModel().getId() + "].txt");
        File file = new File(exportPath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        String header = "protein,peptideRef,intensity";
        String content = header + "\n";

        OutputStream os = new FileOutputStream(file);
        query.setPageSize(pageSize);
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<AnalyseDataDO>> dataListRes = analyseDataService.getList(query);
            for (AnalyseDataDO analyseData : dataListRes.getModel()) {
                String line = analyseData.getProteinName() + "," + analyseData.getPeptideRef() + "," + analyseData.getIntensitySum().longValue() + "\n";
                content += line;
            }
            byte[] b = content.getBytes();
            int l = b.length;

            os.write(b, 0, l);
            logger.info("打印第" + i + "/" + totalPage + "页,本页长度:" + l + ";");
        }
        os.close();
        return "redirect:/analyse/overview/list";

    }

    /***
     * @updateAuthor tangtao https://www.promiselee.cn/tao
     * @updateTime 2019-9-25 10:31:48
     * @Archive 根据 id 删除数据
     * @param id 删除的 id
     * @return 成功 status=0
     */
    @PostMapping(value = "delete")
    String overviewDelete(@RequestParam("id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(id);
        PermissionUtil.check(overviewResult.getModel());
        String expId = overviewResult.getModel().getExpId();
        // 执行删除操作
        analyseOverviewService.delete(id);
        status = 0;
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(d);
        data.put("time", time);
        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }


    @PostMapping(value = "/overview/select")
    String overviewSelect(Model model, RedirectAttributes redirectAttributes) {
        return "analyse/overview/select";
    }

    @PostMapping(value = "/overview/comparison")
    String overviewComparison(Model model,
                              @RequestParam(value = "overviewIdA", required = false) String overviewIdA,
                              @RequestParam(value = "overviewIdB", required = false) String overviewIdB,
                              @RequestParam(value = "overviewIdC", required = false) String overviewIdC,
                              @RequestParam(value = "overviewIdD", required = false) String overviewIdD,
                              RedirectAttributes redirectAttributes) {

        HashSet<String> overviewIds = new HashSet<>();
        if (StringUtils.isNotEmpty(overviewIdA)) {
            model.addAttribute("overviewIdA", overviewIdA);
            overviewIds.add(overviewIdA);
        }
        if (StringUtils.isNotEmpty(overviewIdB)) {
            model.addAttribute("overviewIdB", overviewIdB);
            overviewIds.add(overviewIdB);
        }

        if (StringUtils.isNotEmpty(overviewIdC)) {
            model.addAttribute("overviewIdC", overviewIdC);
            overviewIds.add(overviewIdC);
        }

        if (StringUtils.isNotEmpty(overviewIdD)) {
            model.addAttribute("overviewIdD", overviewIdD);
            overviewIds.add(overviewIdD);
        }


        if (overviewIds.size() <= 1) {
            Map<String, Object> map = model.asMap();
            for (String key : map.keySet()) {
                redirectAttributes.addFlashAttribute(key, map.get(key));
                redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.COMPARISON_OVERVIEW_IDS_MUST_BIGGER_THAN_TWO.getMessage());
            }
            return "redirect:/analyse/overview/select";
        }

        List<AnalyseOverviewDO> overviews = new ArrayList<>();
        for (String overviewId : overviewIds) {
            ResultDO<AnalyseOverviewDO> tempResult = analyseOverviewService.getById(overviewId);
            PermissionUtil.check(tempResult.getModel());
            overviews.add(tempResult.getModel());
        }

        ComparisonResult result = analyseOverviewService.comparison(overviews);
        model.addAttribute("samePeptides", result.getSamePeptides());
        model.addAttribute("diffPeptides", result.getDiffPeptides());
        model.addAttribute("identifiesMap", result.getIdentifiesMap());
        return "analyse/overview/comparison";
    }

    /***
     * @Archive 实现查询 xic 数据
     * @UpdateTime 2019-9-20 16:12:01
     * @UpdateAuthor tangtao
     * @param overviewId
     * @param peptideRef
     * @param currentPage
     * @param pageSizeString
     * @return
     */
    @PostMapping(value = "/dataList")
    String dataList(
            @RequestParam(value = "overviewId") String overviewId,
            @RequestParam(value = "peptideRef", required = false) String peptideRef,
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "1000") String pageSizeString) {

        int pageSize = Integer.parseInt(pageSizeString);
        System.out.println("datalist" + pageSize);
        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            data.put("pageSize", pageSize);
            data.put("overviewId", overviewId);
            data.put("peptideRef", peptideRef);

            ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);

            PermissionUtil.check(overviewResult.getModel());
            data.put("overview", overviewResult.getModel());

            AnalyseDataQuery query = new AnalyseDataQuery();
            query.setPageSize(pageSize);
            query.setPageNo(currentPage);

            if (StringUtils.isNotEmpty(peptideRef)) {
                query.setPeptideRef(peptideRef);
            }

            query.setOverviewId(overviewId);
            ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);
            List<AnalyseDataDO> datas = resultDO.getModel();

            data.put("datas", datas);
            data.put("totalPage", resultDO.getTotalPage());
            data.put("currentPage", currentPage);
            data.put("totalNum", resultDO.getTotalNum());
            status = 0;

        } while (false);


        map.put("status", status);

        // 将数据再打包一次 简化前端数据处理逻辑
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    @PostMapping(value = "/clinic")
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
