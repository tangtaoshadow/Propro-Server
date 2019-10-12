package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.config.VMProperties;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.ScoreType;
import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.params.ExtractParams;
import com.westlake.air.propro.domain.params.IrtParams;
import com.westlake.air.propro.domain.params.WorkflowParams;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.SwathIndexQuery;
import com.westlake.air.propro.exception.UnauthorizedAccessException;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.PermissionUtil;
import com.westlake.air.propro.utils.ScoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 10:00
 */
@RestController
@RequestMapping("experiment")
public class ExperimentController extends BaseController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    SwathIndexService swathIndexService;
    @Autowired
    ProjectService projectService;
    @Autowired
    Extractor extractor;
    @Autowired
    VMProperties vmProperties;

    /***
     * @UpdateAuthor tangtao https://www.promiselee.cn/tao
     * @UpadteTime 2019-10-7 21:31:02
     * @Archieve 实验数据列表
     * @param model
     * @param currentPage
     * @param pageSize
     * @param projectName
     * @param type
     * @param expName
     * @return
     */
    @PostMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                @RequestParam(value = "projectName", required = false) String projectName,
                @RequestParam(value = "type", required = false) String type,
                @RequestParam(value = "expName", required = false) String expName) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            data.put("expName", expName);
            data.put("projectName", projectName);
            data.put("pageSize", pageSize);
            data.put("type", type);

            ExperimentQuery query = new ExperimentQuery();

            if (expName != null && !expName.isEmpty()) {
                query.setName(expName);
            }

            if (projectName != null && !projectName.isEmpty()) {

                ProjectDO project = projectService.getByName(projectName);
                if (project == null) {
                    // 项目不存在
                    status = -2;
                    break;
                } else {
                    query.setProjectId(project.getId());
                }
                pageSize = Integer.MAX_VALUE;//如果是根据项目名称进行搜索的,直接全部展示出来
            }

            if (type != null && !type.isEmpty()) {
                query.setType(type);
            }

            if (!isAdmin()) {
                query.setOwnerName(getCurrentUsername());
            }

            query.setPageSize(pageSize);
            query.setPageNo(currentPage);

            ResultDO<List<ExperimentDO>> resultDO = experimentService.getList(query);
            HashMap<String, AnalyseOverviewDO> analyseOverviewDOMap = new HashMap<>();

            for (ExperimentDO experimentDO : resultDO.getModel()) {
                List<AnalyseOverviewDO> analyseOverviewDOList = analyseOverviewService.getAllByExpId(experimentDO.getId());

                if (analyseOverviewDOList.isEmpty()) {
                    continue;
                }
                analyseOverviewDOMap.put(experimentDO.getId(), analyseOverviewDOList.get(0));
                // analyseOverviewDOMap.put(experimentDO.getId(), analyseOverviewDOList.get(0));
            }

            data.put("experiments", resultDO.getModel());
            data.put("analyseOverviewDOMap", analyseOverviewDOMap);
            data.put("totalPage", resultDO.getTotalPage());
            data.put("totalNum", resultDO.getTotalNum());
            data.put("currentPage", currentPage);

            // tangtao : 获取数据成功

            status = 0;
        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }

    @PostMapping(value = "/listByExpId")
    String listByExpId(Model model,
                       @RequestParam(value = "expId", required = true) String expId) {

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        if (expResult.isFailed()) {
            return "redirect:/experiment/list";
        }

        ExperimentDO exp = expResult.getModel();
        return "redirect:/experiment/list?projectName=" + exp.getProjectName();
    }

    @PostMapping(value = "/create")
    String create(Model model) {
        return "experiment/create";
    }

    @PostMapping(value = "/batchcreate")
    String batchCreate(Model model,
                       @RequestParam(value = "projectName", required = false) String projectName,
                       RedirectAttributes redirectAttributes) {

        ProjectDO project = projectService.getByName(projectName);
        if (project == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED.getMessage());
            return "redirect:/project/list";
        }
        PermissionUtil.check(project);
        model.addAttribute("project", project);
        return "experiment/batchcreate";
    }


    /***
     * @UpdateTime 2019-10-11 13:22:19
     * @updateAuthor tangtao https://www.promiselee.cn/tao
     * @Archive 编辑实验列表数据
     * @param id 传入查询的实验列表的id
     * @return 0 succes
     */
    @PostMapping(value = "/edit")
    String edit(@RequestParam(value = "id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
            if (resultDO.isFailed()) {
                status = -2;
                data.put("errorMsg", resultDO.getMsgInfo());
                break;
            }

            // 检查 model
            try {
                PermissionUtil.check(resultDO.getModel());
                data.put("experiment", resultDO.getModel());
            } catch (Exception e) {
                status = -3;
                break;
            }

            // 提取成功
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @updateTime 2019-10-11 16:34:15
     * @Archive 查看实验列表详情
     * @param id
     * @return 0 success
     */
    @PostMapping(value = "/detail")
    String detail(@RequestParam(value = "id") String id) {


        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
            if (true != resultDO.isSuccess()) {

                status = -2;
                data.put("errorMsg", resultDO.getMsgInfo());
                break;
            }

            ExperimentDO exp = resultDO.getModel();

            try {

                PermissionUtil.check(exp);
            } catch (Exception e) {

                status = -3;
                break;
            }
            data.put("experiment", exp);
            // 成功
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @UpdateAuthor tangtao https://www.promiselee.cn/tao
     * @UpadteTime 2019-10-11 16:16:20
     * @Archieve 更新实验数据列表
     * @param id
     * @param name
     * @param type
     * @param iRtLibraryId
     * @param slope
     * @param intercept
     * @param description
     * @param projectName
     * @return 0 success
     */
    @PostMapping(value = "/update")
    String update(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "type") String type,
            @RequestParam(value = "iRtLibraryId") String iRtLibraryId,
            @RequestParam(value = "slope") Double slope,
            @RequestParam(value = "intercept") Double intercept,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "projectName") String projectName) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
            if (resultDO.isFailed()) {
                status = -2;
                data.put("errorMsg", resultDO.getMsgInfo());
                break;
            }

            ExperimentDO experimentDO = resultDO.getModel();
            try {
                PermissionUtil.check(resultDO.getModel());

            } catch (Exception e) {
                status = -3;
                break;
            }

            ProjectDO project = projectService.getByName(projectName);
            if (project == null) {
                status = -4;
                data.put("errorMsg", resultDO.getMsgInfo());
                break;
            }

            try {
                PermissionUtil.check(project);
            } catch (UnauthorizedAccessException e) {
                status = -5;
                data.put("errorMsg", ResultCode.UNAUTHORIZED_ACCESS.getMessage());
                break;
            }

            experimentDO.setName(name);
            experimentDO.setType(type);
            experimentDO.setProjectName(projectName);
            experimentDO.setProjectId(project.getId());
            experimentDO.setDescription(description);
            experimentDO.setIRtLibraryId(iRtLibraryId);
            IrtResult irtResult = experimentDO.getIrtResult();
            irtResult.setSi(new SlopeIntercept(slope, intercept));
            experimentDO.setIrtResult(irtResult);
            ResultDO result = experimentService.update(experimentDO);

            if (result.isFailed()) {
                status = -6;
                data.put("errorMsg", result.getMsgInfo());
                break;
            }

            // 更新成功
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @updateTime 2019-10-13 00:32:53 tangtao
     * @Archive 删除指定id的实验列表数据
     * @param id 删除的id
     * @return success 0
     */
    @PostMapping(value = "/delete")
    String delete(@RequestParam("id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            try {

                ResultDO<ExperimentDO> exp = experimentService.getById(id);
                PermissionUtil.check(exp.getModel());
                experimentService.delete(id);
                swathIndexService.deleteAllByExpId(id);
                analyseOverviewService.deleteAllByExpId(id);
                data.put("projectName", exp.getModel().getProjectName());

            } catch (Exception e) {
                status = -2;
                break;
            }


            status = 0;

        } while (false);


        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    @PostMapping(value = "/deleteAll/{id}")
    String deleteAll(Model model, @PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        ResultDO<ExperimentDO> exp = experimentService.getById(id);
        PermissionUtil.check(exp.getModel());
        analyseOverviewService.deleteAllByExpId(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/experiment/list";
    }

    @PostMapping(value = "/extractor")
    String extractor(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.OBJECT_NOT_EXISTED);
            return "redirect:/experiment/list";
        }
        PermissionUtil.check(resultDO.getModel());

        ProjectDO project = projectService.getById(resultDO.getModel().getProjectId());
        if (project != null) {
            model.addAttribute("libraryId", project.getLibraryId());
            model.addAttribute("iRtLibraryId", project.getIRtLibraryId());
        }

        model.addAttribute("iRtLibraries", getLibraryList(1, true));
        model.addAttribute("libraries", getLibraryList(0, true));
        model.addAttribute("experiment", resultDO.getModel());
        model.addAttribute("scoreTypes", ScoreType.getShownTypes());

        return "experiment/extractor";
    }

    @PostMapping(value = "/doextract")
    String doExtract(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "rtExtractWindow", required = true, defaultValue = Constants.DEFAULT_RT_EXTRACTION_WINDOW_STR) Float rtExtractWindow,
                     @RequestParam(value = "mzExtractWindow", required = true, defaultValue = Constants.DEFAULT_MZ_EXTRACTION_WINDOW_STR) Float mzExtractWindow,
                     @RequestParam(value = "slope", required = false) Double slope,
                     @RequestParam(value = "intercept", required = false) Double intercept,
                     @RequestParam(value = "note", required = false) String note,
                     //打分相关的入参
                     @RequestParam(value = "sigma", required = false, defaultValue = Constants.DEFAULT_SIGMA_STR) Float sigma,
                     @RequestParam(value = "spacing", required = false, defaultValue = Constants.DEFAULT_SPACING_STR) Float spacing,
                     @RequestParam(value = "fdr", required = false, defaultValue = Constants.DEFAULT_FDR_STR) Double fdr,
                     @RequestParam(value = "shapeScoreThreshold", required = false, defaultValue = Constants.DEFAULT_SHAPE_SCORE_THRESHOLD_STR) Float shapeScoreThreshold,
                     @RequestParam(value = "shapeWeightScoreThreshold", required = false, defaultValue = Constants.DEFAULT_SHAPE_WEIGHT_SCORE_THRESHOLD_STR) Float shapeWeightScoreThreshold,
                     @RequestParam(value = "uniqueOnly", required = false, defaultValue = "false") Boolean uniqueOnly,
                     HttpServletRequest request,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            return "redirect:/extractor?id=" + id;
        }

        PermissionUtil.check(resultDO.getModel());
        LibraryDO library = libraryService.getById(libraryId);
        if (library == null) {
            return "redirect:/extractor?id=" + id;
        }

        List<String> scoreTypes = ScoreUtil.getScoreTypes(request);

        TaskDO taskDO = new TaskDO(TaskTemplate.EXTRACT_PEAKPICK_SCORE, resultDO.getModel().getName() + ":" + library.getName() + "(" + libraryId + ")");
        taskService.insert(taskDO);
        SlopeIntercept si = SlopeIntercept.create();
        if (slope != null && intercept != null) {
            si.setSlope(slope);
            si.setIntercept(intercept);
        }
        SigmaSpacing ss = new SigmaSpacing(sigma, spacing);

        WorkflowParams input = new WorkflowParams();
        input.setExperimentDO(resultDO.getModel());
        input.setLibrary(library);
        input.setSlopeIntercept(si);
        input.setNote(note);
        input.setFdr(fdr);
        input.setOwnerName(getCurrentUsername());
        input.setExtractParams(new ExtractParams(mzExtractWindow, rtExtractWindow));
        input.setUniqueOnly(uniqueOnly);
        input.setScoreTypes(scoreTypes);
        input.setSigmaSpacing(ss);
        input.setXcorrShapeThreshold(shapeScoreThreshold);
        input.setXcorrShapeWeightThreshold(shapeWeightScoreThreshold);

        experimentTask.extract(taskDO, input);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @PostMapping(value = "/irt")
    String irt(Model model,
               @RequestParam(value = "id", required = true) String id,
               RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.OBJECT_NOT_EXISTED);
            return "redirect:/experiment/list";
        }
        PermissionUtil.check(resultDO.getModel());

        ProjectDO project = projectService.getById(resultDO.getModel().getProjectId());
        if (project != null) {
            model.addAttribute("iRtLibraryId", project.getIRtLibraryId());
            model.addAttribute("libraryId", project.getLibraryId());
        }

        model.addAttribute("irtLibraries", getLibraryList(1, true));
        model.addAttribute("libraries", getLibraryList(0, true));
        model.addAttribute("experiment", resultDO.getModel());
        return "experiment/irt";
    }

    @PostMapping(value = "/doirt")
    String doIrt(Model model,
                 @RequestParam(value = "id", required = true) String id,
                 @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                 @RequestParam(value = "useLibrary", required = true, defaultValue = "false") boolean useLibrary,
                 @RequestParam(value = "libraryId", required = false) String libraryId,
                 @RequestParam(value = "sigma", required = true, defaultValue = Constants.DEFAULT_SIGMA_STR) Float sigma,
                 @RequestParam(value = "spacing", required = true, defaultValue = Constants.DEFAULT_SPACING_STR) Float spacing,
                 @RequestParam(value = "mzExtractWindow", required = true, defaultValue = Constants.DEFAULT_MZ_EXTRACTION_WINDOW_STR) Float mzExtractWindow,
                 RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            return "redirect:/irt/" + id;
        }
        PermissionUtil.check(resultDO.getModel());
        TaskDO taskDO = new TaskDO(TaskTemplate.IRT, resultDO.getModel().getName() + ":" + (useLibrary ? libraryId : iRtLibraryId) + "-Num:1");
        taskService.insert(taskDO);

        SigmaSpacing sigmaSpacing = new SigmaSpacing(sigma, spacing);
        List<ExperimentDO> exps = new ArrayList<>();
        exps.add(resultDO.getModel());

        LibraryDO lib = libraryService.getById((useLibrary ? libraryId : iRtLibraryId));

        IrtParams irtParams = new IrtParams();
        irtParams.setMzExtractWindow(mzExtractWindow);
        irtParams.setSigmaSpacing(sigmaSpacing);
        irtParams.setUseLibrary(useLibrary);
        irtParams.setLibrary(lib);

        experimentTask.irt(taskDO, exps, irtParams);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @PostMapping(value = "/irtresult")
    @ResponseBody
    ResultDO<JSONObject> irtResult(Model model,
                                   @RequestParam(value = "expId", required = false) String expId) {
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        if (expResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.EXPERIMENT_NOT_EXISTED);
            return resultDO;
        }
        PermissionUtil.check(expResult.getModel());
        ExperimentDO experimentDO = expResult.getModel();

        IrtResult irtResult = experimentDO.getIrtResult();
        if (irtResult == null) {
            return ResultDO.buildError(ResultCode.IRT_FIRST);
        }

        JSONObject res = new JSONObject();
        JSONArray selectedArray = new JSONArray();
        JSONArray unselectedArray = new JSONArray();
        JSONArray lineArray = new JSONArray();
        for (Double[] pair : irtResult.getSelectedPairs()) {
            selectedArray.add(JSONArray.toJSON(pair));
            lineArray.add(JSONArray.toJSON(new Double[]{pair[0], (pair[0] - irtResult.getSi().getIntercept()) / irtResult.getSi().getSlope()}));
        }
        for (Double[] pair : irtResult.getUnselectedPairs()) {
            unselectedArray.add(JSONArray.toJSON(pair));
        }

        res.put("slope", irtResult.getSi().getSlope());
        res.put("intercept", irtResult.getSi().getIntercept());
        res.put("lineArray", lineArray);
        res.put("selectedArray", selectedArray);
        res.put("unselectedArray", unselectedArray);
        resultDO.setModel(res);
        return resultDO;
    }

    @PostMapping(value = "/getWindows")
    @ResponseBody
    ResultDO<JSONObject> getWindows(Model model, @RequestParam(value = "expId", required = true) String expId) {

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        PermissionUtil.check(expResult.getModel());

        List<WindowRange> ranges = expResult.getModel().getWindowRanges();
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        //按照mz进行排序
        ranges.sort(Comparator.comparingDouble(WindowRange::getMz));
        JSONObject res = new JSONObject();
        JSONArray mzStartArray = new JSONArray();
        JSONArray mzRangArray = new JSONArray();
        for (int i = 0; i < ranges.size(); i++) {
            mzStartArray.add(ranges.get(i).getStart());
            mzRangArray.add((ranges.get(i).getEnd() - ranges.get(i).getStart()));
        }
        res.put("starts", mzStartArray);
        res.put("rangs", mzRangArray);
        res.put("min", ranges.get(0).getStart());
        res.put("max", ranges.get(ranges.size() - 1).getEnd());
        resultDO.setModel(res);
        return resultDO;
    }

    @PostMapping(value = "/getPrmWindows")
    @ResponseBody
    ResultDO<JSONObject> getPrmWindows(Model model, @RequestParam(value = "expId", required = true) String expId) {

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        PermissionUtil.check(expResult.getModel());

        HashMap<Float, Float[]> peptideMap = experimentService.getPrmRtWindowMap(expId);
        JSONArray peptideMs1List = new JSONArray();
        for (Float precursorMz : peptideMap.keySet()) {
            if (Math.abs(peptideMap.get(precursorMz)[0] - peptideMap.get(precursorMz)[1]) < 20) {
                continue;
            }
            JSONArray peptide = new JSONArray();
            peptide.add(new Float[]{peptideMap.get(precursorMz)[0], precursorMz});
            peptide.add(new Float[]{peptideMap.get(precursorMz)[1], precursorMz});
            peptideMs1List.add(peptide);
        }
        JSONObject res = new JSONObject();
        res.put("peptideList", peptideMs1List);
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        resultDO.setModel(res);
        return resultDO;
    }

    @PostMapping(value = "/getPrmDensity")
    @ResponseBody
    ResultDO<JSONObject> getPrmDensity(Model model, @RequestParam(value = "expId", required = true) String expId) {

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        PermissionUtil.check(expResult.getModel());

        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        JSONArray ms2Density = new JSONArray();
        SwathIndexQuery query = new SwathIndexQuery();
        query.setExpId(expId);

        query.setLevel(1);
        List<SwathIndexDO> ms1Indexs = swathIndexService.getAll(query);
        if (ms1Indexs == null || ms1Indexs.size() != 1) {
            return ResultDO.buildError(ResultCode.DATA_IS_EMPTY);
        }
        List<Float> ms1RtList = ms1Indexs.get(0).getRts();
        Collections.sort(ms1RtList);

        query.setLevel(2);
        List<SwathIndexDO> ms2Indexs = swathIndexService.getAll(query);
        List<Float> ms2RtList = new ArrayList<>();
        for (SwathIndexDO ms2 : ms2Indexs) {
            ms2RtList.addAll(ms2.getRts());
        }
        Collections.sort(ms2RtList);
        int ms2Index = ms2RtList.size() - 1;
        int max = Integer.MIN_VALUE;
        for (int ms1Index = ms1RtList.size() - 1; ms1Index >= 0; ms1Index--) {
            int count = 0;
            for (; ms2Index >= 0; ms2Index--) {
                if (ms2Index - count >= 0 && ms2RtList.get(ms2Index - count) > ms1RtList.get(ms1Index)) {
                    count++;
                } else {
                    break;
                }
            }
            ms2Density.add(new Float[]{ms1RtList.get(ms1Index), (float) count});
            if (count > max) {
                max = count;
            }
        }

        JSONObject res = new JSONObject();
        res.put("ms2Density", ms2Density);
        res.put("upMax", (int) Math.ceil(max / 10d) * 10d);
        resultDO.setModel(res);
        return resultDO;
    }
}
