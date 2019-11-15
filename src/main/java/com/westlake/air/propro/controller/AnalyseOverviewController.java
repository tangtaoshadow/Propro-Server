package com.westlake.air.propro.controller;

import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.feature.ChromatographicScorer;
import com.westlake.air.propro.algorithm.feature.LibraryScorer;
import com.westlake.air.propro.algorithm.formula.FormulaCalculator;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.algorithm.peak.FeatureExtractor;
import com.westlake.air.propro.algorithm.peak.GaussFilter;
import com.westlake.air.propro.algorithm.peak.SignalToNoiseEstimator;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.ScoreType;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.AnalyseDataRT;
import com.westlake.air.propro.domain.bean.analyse.ComparisonResult;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.domain.query.AnalyseOverviewQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.PermissionUtil;
import com.westlake.air.propro.utils.RepositoryUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:50
 */
@Controller
@RequestMapping("analyse")
public class AnalyseOverviewController extends BaseController {

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

    @RequestMapping(value = "/overview/list")
    String list(Model model,
                        @RequestParam(value = "expId", required = false) String expId,
                        @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("expId", expId);
        ResultDO<ExperimentDO> expResult = null;
        if (StringUtils.isNotEmpty(expId)) {
            expResult = experimentService.getById(expId);
            if (expResult.isFailed()) {
                model.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED);
                return "analyse/overview/list";
            }
            PermissionUtil.check(expResult.getModel());
            model.addAttribute("experiment", expResult.getModel());
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
        model.addAttribute("overviews", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("scores", ScoreType.getUsedTypes());

        return "analyse/overview/list";
    }

    @RequestMapping(value = "/overview/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

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
        model.addAttribute("library", library);
        model.addAttribute("rts", rts);
        model.addAttribute("badRts", badRts);
        model.addAttribute("overview", resultDO.getModel());
        model.addAttribute("slope", resultDO.getModel().getSlope());
        model.addAttribute("intercept", resultDO.getModel().getIntercept());
        model.addAttribute("targetMap", overview.getTargetDistributions());
        model.addAttribute("decoyMap", overview.getDecoyDistributions());
        return "analyse/overview/detail";
    }

    @RequestMapping(value = "/overview/export/{id}")
    String export(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) throws IOException {

        return "redirect:/analyse/overview/export";

    }

    @RequestMapping(value = "/overview/doExport/{id}")
    String doExport(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) throws IOException {

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
            logger.info("打印第" + i + "/" + totalPage + "页;");
        }
        os.close();
        return "redirect:/analyse/overview/list";
    }
    @RequestMapping(value = "/overview/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(id);
        PermissionUtil.check(overviewResult.getModel());
        String expId = overviewResult.getModel().getExpId();
        analyseOverviewService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/analyse/overview/list?expId=" + expId;
    }

    @RequestMapping(value = "/overview/select")
    String select(Model model, RedirectAttributes redirectAttributes) {
        return "analyse/overview/select";
    }

    @RequestMapping(value = "/overview/comparison")
    String comparison(Model model,
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
}
