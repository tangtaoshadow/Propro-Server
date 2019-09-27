package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.ScoreType;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 16:02
 */
@RestController
@RequestMapping("/score")
public class ScoreController extends BaseController {

    @Autowired
    ScoreService scoreService;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;

    /***
     *
     * @UpdateAuthor 2019-9-27 10:45:21
     * @UpdateTime 2019-9-27 10:45:12
     * @param currentPage
     * @param pageSize
     * @param overviewId
     * @param peptideRef
     * @param fdrStart
     * @param fdrEnd
     * @param isIdentified
     * @param isDecoy
     * @return
     */
    @PostMapping(value = "/list")
    String list(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "500") Integer pageSize,
            @RequestParam(value = "overviewId", required = false) String overviewId,
            @RequestParam(value = "peptideRef", required = false) String peptideRef,
            @RequestParam(value = "fdrStart", required = false) Double fdrStart,
            @RequestParam(value = "fdrEnd", required = false) Double fdrEnd,
            @RequestParam(value = "isIdentified", required = false) String isIdentified,
            @RequestParam(value = "isDecoy", required = false) String isDecoy) {

        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            data.put("overviewId", overviewId);
            data.put("peptideRef", peptideRef);
            data.put("pageSize", pageSize);
            data.put("fdrStart", fdrStart);
            data.put("fdrEnd", fdrEnd);
            data.put("isIdentified", isIdentified);
            data.put("isDecoy", isDecoy);

            AnalyseDataQuery query = new AnalyseDataQuery();
            if (peptideRef != null && !peptideRef.isEmpty()) {
                query.setPeptideRef(peptideRef);
            }

            if (isIdentified != null && isIdentified.equals("Yes")) {
                query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
            } else if (isIdentified != null && isIdentified.equals("No")) {
                query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
                query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
            }

            if (isDecoy != null && isDecoy.equals("Yes")) {
                query.setIsDecoy(true);
            } else if (isDecoy != null && isDecoy.equals("No")) {
                query.setIsDecoy(false);
            }

            if (fdrStart != null) {
                query.setFdrStart(fdrStart);
            }

            if (fdrEnd != null) {
                query.setFdrEnd(fdrEnd);
            }

            if (overviewId == null) {
                status = -2;
                break;
                // redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY.getMessage());
            }

            query.setOverviewId(overviewId);
            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);

            if (overviewResult.isFailed()) {
                status = -3;
                break;
            }

            PermissionUtil.check(overviewResult.getModel());

            ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);

            data.put("overview", overviewResult.getModel());
            data.put("scores", resultDO.getModel());
            data.put("scoreTypes", overviewResult.getModel().getScoreTypes());
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

    @PostMapping(value = "/result/list")
    String resultList(Model model,
                      @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                      @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                      @RequestParam(value = "overviewId", required = true) String overviewId,
                      @RequestParam(value = "peptideRef", required = false) String peptideRef,
                      @RequestParam(value = "proteinName", required = false) String proteinName,
                      @RequestParam(value = "isIdentified", required = false) String isIdentified,
                      RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);
        model.addAttribute("proteinName", proteinName);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("isIdentified", isIdentified);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        PermissionUtil.check(overviewResult.getModel());

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setIsDecoy(false);
        if (peptideRef != null && !peptideRef.isEmpty()) {
            query.setPeptideRef(peptideRef);
        }
        if (proteinName != null && !proteinName.isEmpty()) {
            query.setProteinName(proteinName);
        }
        if (isIdentified != null && isIdentified.equals("Yes")) {
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        } else if (isIdentified != null && isIdentified.equals("No")) {
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
        }
        query.setOverviewId(overviewId);

        List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAll(query);
        if (peptideRef != null && peptideRef.length() > 0) {
            query.setPeptideRef(null);
            query.setProteinName(analyseDataDOList.get(0).getProteinName());
            analyseDataDOList = analyseDataService.getAll(query);
        }
        HashMap<String, List<AnalyseDataDO>> proteinMap = new HashMap<>();
        for (AnalyseDataDO dataDO : analyseDataDOList) {
            if (proteinMap.containsKey(dataDO.getProteinName())) {
                proteinMap.get(dataDO.getProteinName()).add(dataDO);
            } else {
                List<AnalyseDataDO> newList = new ArrayList<>();
                newList.add(dataDO);
                proteinMap.put(dataDO.getProteinName(), newList);
            }
        }
        List<String> protList = new ArrayList<>(proteinMap.keySet());
        int totalPage = (int) Math.ceil(protList.size() / (double) pageSize);
        HashMap<String, List<AnalyseDataDO>> pageProtMap = new HashMap<>();

        for (int i = pageSize * (currentPage - 1); i < protList.size() && i < pageSize * currentPage; i++) {
            pageProtMap.put(protList.get(i), proteinMap.get(protList.get(i)));
        }

        model.addAttribute("protMap", pageProtMap);
        model.addAttribute("overview", overviewResult.getModel());
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", protList.size());
        return "scores/result/list";
    }

    @PostMapping(value = "/detail")
    String detail(Model model, @RequestParam(value = "overviewId", required = true) String overviewId, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        PermissionUtil.check(overviewResult.getModel());

        model.addAttribute("scoreTypes", ScoreType.getUsedTypes());
        model.addAttribute("scoreTypeArray", JSONArray.parseArray(JSON.toJSONString(ScoreType.getUsedTypes())));
        model.addAttribute("overview", overviewResult.getModel());
        return "scores/detail";
    }
}
