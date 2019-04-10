package com.westlake.air.propro.controller;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.service.ProjectService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.io.File;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 10:00
 */
@Controller
@RequestMapping("project")
public class ProjectController extends BaseController {

    @Autowired
    ProjectService projectService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                @RequestParam(value = "name", required = false) String name,
                @RequestParam(value = "ownerName", required = false) String ownerName) {
        model.addAttribute("name", name);
        model.addAttribute("ownerName", ownerName);
        model.addAttribute("pageSize", pageSize);
        ProjectQuery query = new ProjectQuery();
        if (name != null && !name.isEmpty()) {
            query.setName(name);
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            query.setOwnerName(ownerName);
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ProjectDO>> resultDO = projectService.getList(query);

        model.addAttribute("projectList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "project/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "project/create";
    }


    @RequestMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "repository", required = true) String repository,
               @RequestParam(value = "ownerName", required = false) String ownerName,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "type", required = true) String type,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("repository", repository);
        model.addAttribute("ownerName", ownerName);
        model.addAttribute("name", name);
        model.addAttribute("description", description);
        model.addAttribute("type", type);

        File file = new File(repository);
        if (!file.exists()) {
            file.mkdirs();
        }

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName(name);
        projectDO.setDescription(description);
        projectDO.setRepository(repository);
        projectDO.setOwnerName(ownerName);
        projectDO.setType(type);

        ResultDO result = projectService.insert(projectDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "project/create";
        }

        return "redirect:/project/list";
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/project/list";
        } else {
            model.addAttribute("project", resultDO.getModel());
            return "project/edit";
        }
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        projectService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/irt")
    String irt(Model model,
               @RequestParam(value = "id", required = true) String id,
               @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
               RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }
        model.addAttribute("exps", expList);
        model.addAttribute("project", resultDO.getModel());
        model.addAttribute("iRtLibraryId", iRtLibraryId);
        model.addAttribute("libraries", getLibraryList(1));

        return "project/irt";
    }

    @RequestMapping(value = "/doirt")
    String doIrt(Model model,
                 @RequestParam(value = "id", required = true) String id,
                 @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                 @RequestParam(value = "sigma", required = true, defaultValue = "3.75") Float sigma,
                 @RequestParam(value = "spacing", required = true, defaultValue = "0.01") Float spacing,
                 @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                 RedirectAttributes redirectAttributes) {

        List<ExperimentDO> expList = getAllExperimentsByProjectId(id);
        if (expList == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
            return "redirect:/project/list";
        }
        int count = 0;
        for (ExperimentDO exp : expList) {
            if (exp.getSlope() == null || exp.getIntercept() == null) {
                TaskDO taskDO = new TaskDO(TaskTemplate.IRT, exp.getName() + ":" + iRtLibraryId);
                taskService.insert(taskDO);
                SigmaSpacing sigmaSpacing = new SigmaSpacing(sigma, spacing);
                experimentTask.convAndIrt(exp, iRtLibraryId, mzExtractWindow, sigmaSpacing, taskDO);
                count++;
            }
        }
        if (count == 0) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.ALL_EXPERIMENTS_UNDER_THIS_PROJECT_ARE_ALREADY_COMPUTE_IRT);
            return "redirect:/project/list";
        } else {
            return "redirect:/task/list";
        }
    }
    @RequestMapping(value = "/deleteirt/{id}")
    String deleteIrt(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes){
        List<ExperimentDO> expList = getAllExperimentsByProjectId(id);
        if (expList == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
            return "redirect:/project/list";
        }
        for (ExperimentDO experimentDO: expList){
            experimentDO.setSlope(null);
            experimentDO.setIntercept(null);
            experimentService.update(experimentDO);
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/deleteAll/{id}")
    String deleteAll(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes){
        String name = projectService.getById(id).getModel().getName();
        List<ExperimentDO> expList = experimentService.getAllByProjectName(name);
        for (ExperimentDO experimentDO: expList){
            String expId = experimentDO.getId();
            experimentService.delete(expId);
            scanIndexService.deleteAllByExperimentId(expId);
            List<AnalyseOverviewDO> overviewDOList = analyseOverviewService.getAllByExpId(expId);
            for (AnalyseOverviewDO overviewDO : overviewDOList) {
                analyseDataService.deleteAllByOverviewId(overviewDO.getId());
            }
            analyseOverviewService.deleteAllByExpId(experimentDO.getId());
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/deleteAnalyse/{id}")
    String deleteAnalyse(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes){
        String name = projectService.getById(id).getModel().getName();
        List<ExperimentDO> expList = experimentService.getAllByProjectName(name);
        for (ExperimentDO experimentDO: expList){
            String expId = experimentDO.getId();
            List<AnalyseOverviewDO> overviewDOList = analyseOverviewService.getAllByExpId(expId);
            for (AnalyseOverviewDO overviewDO : overviewDOList) {
                analyseDataService.deleteAllByOverviewId(overviewDO.getId());
            }
            analyseOverviewService.deleteAllByExpId(expId);
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/extractor")
    String extractor(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "libraryId", required = false) String libraryId,
                     RedirectAttributes redirectAttributes) {
        model.addAttribute("libraryId", libraryId);

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }

        model.addAttribute("exps", expList);
        model.addAttribute("useEpps", true);
        model.addAttribute("libraries", getLibraryList(0));
        model.addAttribute("project", resultDO.getModel());
        model.addAttribute("scoreTypes", ScoreType.getShownTypes());

        return "project/extractor";
    }

    @RequestMapping(value = "/doextract")
    String doExtract(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "creator", required = false) String creator,
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "rtExtractWindow", required = true, defaultValue = "800") Float rtExtractWindow,
                     @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                     @RequestParam(value = "note", required = false) String note,
                     //打分相关的入参
                     @RequestParam(value = "sigma", required = false, defaultValue = "6.25") Float sigma,
                     @RequestParam(value = "spacing", required = false, defaultValue = "0.01") Float spacing,
                     @RequestParam(value = "shapeScoreThreshold", required = false, defaultValue = "0.6") Float shapeScoreThreshold,
                     @RequestParam(value = "shapeWeightScoreThreshold", required = false, defaultValue = "0.8") Float shapeWeightScoreThreshold,
                     @RequestParam(value = "useEpps", required = false, defaultValue = "true") Boolean useEpps,
                     HttpServletRequest request,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> projectResult = projectService.getById(id);
        if (projectResult.isFailed()) {
            return "redirect:/extractor?id=" + id;
        }

        ResultDO<LibraryDO> libResult = libraryService.getById(libraryId);
        if (libResult.isFailed()) {
            return "redirect:/extractor?id=" + id;
        }

        HashSet<String> scoreTypes = new HashSet<>();
        for (ScoreType type : ScoreType.values()) {
            String typeParam = request.getParameter(type.getTypeName());
            if (typeParam != null && typeParam.equals("on")) {
                scoreTypes.add(type.getTypeName());
            }
        }

        List<ExperimentDO> exps = getAllExperimentsByProjectId(id);
        if (exps == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
            return "redirect:/project/list";
        }
        String errorInfo = "";
        for (ExperimentDO exp : exps) {
            if (exp.getSlope() == null || exp.getIntercept() == null) {
                errorInfo = errorInfo + ResultCode.IRT_FIRST + ":" + exp.getName() + "(" + exp.getId() + ")";
                continue;
            }
            TaskDO taskDO = new TaskDO(useEpps ? TaskTemplate.EXTRACT_PEAKPICK_SCORE : TaskTemplate.EXTRACTOR, exp.getName() + ":" + libResult.getModel().getName() + "(" + libraryId + ")");
            taskService.insert(taskDO);
            SlopeIntercept si = new SlopeIntercept(exp.getSlope(), exp.getIntercept());
            SigmaSpacing ss = new SigmaSpacing(sigma, spacing);

            LumsParams input = new LumsParams();
            input.setExperimentDO(exp);
            input.setLibraryId(libraryId);
            input.setSlopeIntercept(si);
            if (StringUtils.isEmpty(creator)) {
                creator = projectResult.getModel().getOwnerName();
            }
            input.setCreator(creator);
            input.setRtExtractWindow(rtExtractWindow);
            input.setMzExtractWindow(mzExtractWindow);
            input.setUseEpps(useEpps);
            input.setScoreTypes(scoreTypes);
            input.setSigmaSpacing(ss);
            input.setXcorrShapeThreshold(shapeScoreThreshold);
            input.setXcorrShapeWeightThreshold(shapeWeightScoreThreshold);
            experimentTask.extract(input, taskDO);
        }

        if (StringUtils.isNotEmpty(errorInfo)) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, errorInfo);
        }
        return "redirect:/task/list";
    }

    @RequestMapping(value = "/portionSelector")
    String portionSelector(Model model,
                           @RequestParam(value = "id", required = true) String id,
                           RedirectAttributes redirectAttributes) {
        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }
        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        model.addAttribute("project", resultDO.getModel());
        model.addAttribute("expList", expList);
        return "project/portionSelector";
    }

    @RequestMapping(value = "/overview")
    String overview(Model model,
                    @RequestParam(value = "id", required = true) String projectId,
//                                 @RequestParam(value = "batchName", required = false) String batchName,
                    @RequestParam(value = "peptideRefInfo", required = false) String peptideRefInfo,
                    @RequestParam(value = "proteinNameInfo", required = false) String proteinNameInfo,
                    HttpServletRequest request,
                    RedirectAttributes redirectAttributes) {

        //get project name
        ResultDO<ProjectDO> resultDO = projectService.getById(projectId);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }
        String projectName = resultDO.getModel().getName()+"("+resultDO.getModel().getId() +")";
        //get corresponding experiments
        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        String libraryId = "";
        String libName = "";
        List<String> analyseOverviewIdList = new ArrayList<>();
        List<String> expNameList = new ArrayList<>();
        for (ExperimentDO experimentDO : expList) {
            String checkState = request.getParameter(experimentDO.getId());
            if (checkState != null && checkState.equals("on")) {
                //analyse checked experiments
                AnalyseOverviewDO analyseOverviewDO = analyseOverviewService.getAllByExpId(experimentDO.getId()).get(0);
                libraryId = analyseOverviewDO.getLibraryId();
                libName = analyseOverviewDO.getLibraryName() + "(" + libraryId + ")";
                analyseOverviewIdList.add(analyseOverviewDO.getId());
                expNameList.add(experimentDO.getName());
            }
        }
        HashMap<String, PeptideDO> peptideDOMap = new HashMap<>();
        List<String> protNameList = new ArrayList<>();
        HashMap<String, HashMap<String, List<Integer>>> pepFragIntListMap = new HashMap<>();
        HashMap<String, String> intMap = new HashMap<>();
        if (!proteinNameInfo.isEmpty()) {
            for (String proteinName : proteinNameInfo.split(";")) {
                List<PeptideDO> peptideDOList = peptideService.getAllByLibraryIdAndProteinNameAndIsDecoy(libraryId, proteinName, false);
                for (PeptideDO peptideDO : peptideDOList) {
                    peptideDOMap.put(peptideDO.getPeptideRef(), peptideDO);
                }
            }
        }
        if (!peptideRefInfo.isEmpty()) {
            String[] peptideRefs = peptideRefInfo.split(";");
            for (String peptideRef : peptideRefs) {
                PeptideDO peptideDO = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(libraryId, peptideRef, false);
                peptideDOMap.put(peptideRef, peptideDO);
            }
        }

        HashMap<String, List<Boolean>> identifyMap = new HashMap<>();
        for (PeptideDO peptideDO: peptideDOMap.values()){
            //protein name
            protNameList.add(peptideDO.getProteinName());
            //fragment cutInfo list
            Set<String> cutInfoSet = peptideDO.getFragmentMap().keySet();
            //experiments
            HashMap<String, List<Integer>> fragIntListMap = new HashMap<>();
            String intOverall = "";
            List<Boolean> identifyStatList = new ArrayList<>();
            for (String analyseOverviewId: analyseOverviewIdList) {
                //get fragment intensity map
                AnalyseDataDO analyseDataDO = analyseDataService.getByOverviewIdAndPeptideRefAndIsDecoy(analyseOverviewId, peptideDO.getPeptideRef(), false);
                HashMap<String, Double> fragIntMap;
                if (analyseDataDO == null){
                    fragIntMap = new HashMap<>();
                    identifyStatList.add(false);
                }else {
                    if (analyseDataDO.getIdentifiedStatus() == 0){
                        identifyStatList.add(true);
                    }else {
                        identifyStatList.add(false);
                    }
                    fragIntMap = analyseDataDO.getFragIntMap();
                    intOverall += analyseDataDO.getIntensitySum() + ", ";
                }

                //get fragment intensity list map
                for (String cutInfo : cutInfoSet) {
                    if (fragIntListMap.get(cutInfo) == null) {
                        List<Integer> newList = new ArrayList<>();
                        newList.add(fragIntMap.get(cutInfo) == null ? 0 : (int) Math.round(fragIntMap.get(cutInfo)));
                        fragIntListMap.put(cutInfo, newList);
                    } else {
                        fragIntListMap.get(cutInfo).add(fragIntMap.get(cutInfo) == null ? 0 : (int) Math.round(fragIntMap.get(cutInfo)));
                    }
                }
            }
            if (intOverall.isEmpty()){
                intOverall = "0";
            }
            identifyMap.put(peptideDO.getPeptideRef(), identifyStatList);
            intMap.put(peptideDO.getPeptideRef(), intOverall);
            pepFragIntListMap.put(peptideDO.getPeptideRef(), fragIntListMap);
        }

        //横坐标实验，纵坐标不同pep
        model.addAttribute("projectName", projectName);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("libName", libName);
//        model.addAttribute("peptideRefs", peptideRefs);
        model.addAttribute("protNameList", protNameList);
        model.addAttribute("pepFragIntListMap", pepFragIntListMap);
        model.addAttribute("expNameList", expNameList);
        model.addAttribute("intMap", intMap);
        model.addAttribute("identifyMap", identifyMap);

        return "project/overview";
    }

    private List<ExperimentDO> getAllExperimentsByProjectId(String id) {
        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            return null;
        }

        ExperimentQuery query = new ExperimentQuery();
        query.setProjectName(resultDO.getModel().getName());
        List<ExperimentDO> expList = experimentService.getAll(query);
        return expList;
    }
}