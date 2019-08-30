package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.westlake.air.propro.algorithm.decoy.generator.ShuffleGenerator;
import com.westlake.air.propro.algorithm.formula.FormulaCalculator;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.simple.Protein;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 12:19
 */
@RestController
@RequestMapping("peptide")
public class PeptideController extends BaseController {

    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    FormulaCalculator formulaCalculator;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ShuffleGenerator shuffleGenerator;

    @PostMapping(value = "/list")
    String peptideList(
            @RequestParam(value = "libraryId") String libraryId,
            @RequestParam(value = "proteinName", required = false) String proteinName,
            @RequestParam(value = "peptideRef", required = false) String peptideRef,
            @RequestParam(value = "sequence", required = false) String sequence,
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "uniqueFilter", required = false, defaultValue = "All") String uniqueFilter,
            @RequestParam(value = "pageSize", required = false, defaultValue = "500") Integer pageSize) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        JSONObject json = new JSONObject(map);
        Map<String, Object> data = new HashMap<String, Object>();

        do {

            long startTime = System.currentTimeMillis();
            LibraryDO temp = libraryService.getById(libraryId);
            PermissionUtil.check(temp);
            //
            data.put("libraryInfo", temp);
            data.put("proteinName", proteinName);
            data.put("peptideRef", peptideRef);
            data.put("pageSize", pageSize);
            data.put("uniqueFilter", uniqueFilter);
            data.put("libraries", getLibraryList(null, true));
            data.put("sequence", sequence);

            PeptideQuery query = new PeptideQuery();

            if (libraryId != null && !libraryId.isEmpty()) {
                query.setLibraryId(libraryId);
            }
            if (peptideRef != null && !peptideRef.isEmpty()) {
                query.setPeptideRef(peptideRef);
            }

            if (proteinName != null && !proteinName.isEmpty()) {
                query.setProteinName(proteinName);
            }

            if (sequence != null && !sequence.isEmpty()) {
                query.setSequence(sequence);
            }

            if (!uniqueFilter.equals("All")) {
                if (uniqueFilter.equals("Yes")) {
                    query.setIsUnique(true);
                } else if (uniqueFilter.equals("No")) {
                    query.setIsUnique(false);
                }
            }

            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);

            data.put("peptideList", resultDO.getModel());
            data.put("totalPage", resultDO.getTotalPage());
            data.put("currentPage", currentPage);
            // 搜索用时
            data.put("searchTime", System.currentTimeMillis() - startTime);
            // 搜索结果共计
            data.put("searchNumbers", resultDO.getTotalNum());
            data.put("pageSize", pageSize);
            status = 0;
        } while (false);

        map.put("status", status);
        // 将数据再打包一次 简化前端数据处理逻辑
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    @PostMapping(value = "/protein")
    String protein(Model model,
                   @RequestParam(value = "libraryId", required = true) String libraryId,
                   @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                   @RequestParam(value = "pageSize", required = false, defaultValue = "30") Integer pageSize) {
        long startTime = System.currentTimeMillis();

        LibraryDO temp = libraryService.getById(libraryId);
        PermissionUtil.check(temp);

        model.addAttribute("libraryId", libraryId);
        model.addAttribute("pageSize", pageSize);

        PeptideQuery query = new PeptideQuery();

        if (libraryId != null && !libraryId.isEmpty()) {
            query.setLibraryId(libraryId);
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<Protein>> resultDO = peptideService.getProteinList(query);

        model.addAttribute("proteins", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        StringBuilder builder = new StringBuilder();
        builder.append("本次搜索耗时:").append(System.currentTimeMillis() - startTime).append("毫秒;包含搜索结果总计:")
                .append(resultDO.getTotalNum()).append("条");
        model.addAttribute("searchResult", builder.toString());
        return "peptide/protein";
    }

    @PostMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<PeptideDO> resultDO = peptideService.getById(id);
        if (resultDO.isSuccess()) {

            LibraryDO temp = libraryService.getById(resultDO.getModel().getLibraryId());
            PermissionUtil.check(temp);

            model.addAttribute("peptide", resultDO.getModel());
            return "peptide/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/peptide/list";
        }
    }

    @PostMapping(value = "/calculator")
    String calculator(Model model,
                      @RequestParam(value = "sequence", required = false) String sequence,
                      @RequestParam(value = "type", required = false) String type,
                      @RequestParam(value = "adjust", required = false, defaultValue = "0") int adjust,
                      @RequestParam(value = "deviation", required = false, defaultValue = "0") double deviation,
                      @RequestParam(value = "unimodIds", required = false) String unimodIds,
                      @RequestParam(value = "charge", required = false, defaultValue = "1") int charge
    ) {
        model.addAttribute("charge", charge);
        model.addAttribute("adjust", adjust);
        model.addAttribute("deviation", deviation);
        model.addAttribute("unimodIds", unimodIds);
        if (sequence == null || sequence.isEmpty()) {
            return "peptide/calculator";
        }

        if (type == null || type.isEmpty()) {
            return "peptide/calculator";
        }

        model.addAttribute("sequence", sequence);
        model.addAttribute("type", type);

        List<String> unimodList = null;
        if (unimodIds != null && !unimodIds.isEmpty()) {
            String[] unimodIdArray = unimodIds.split(",");
            unimodList = Arrays.asList(unimodIdArray);
        }

        //默认偏差为0
        double monoMz = formulaCalculator.getMonoMz(sequence, type, charge, adjust, deviation, false, unimodList);
        double averageMz = formulaCalculator.getAverageMz(sequence, type, charge, adjust, deviation, false, unimodList);
        model.addAttribute("monoMz", monoMz);
        model.addAttribute("averageMz", averageMz);

        return "peptide/calculator";
    }

}
