package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.algorithm.FormulaCalculator;
import com.westlake.air.pecs.algorithm.FragmentCalculator;
import com.westlake.air.pecs.decoy.generator.ShuffleGenerator;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 12:19
 */
@Controller
@RequestMapping("transition")
public class TransitionController extends BaseController {

    @Autowired
    FragmentCalculator fragmentCalculator;
    @Autowired
    FormulaCalculator formulaCalculator;
    @Autowired
    TransitionService transitionService;
    @Autowired
    ShuffleGenerator shuffleGenerator;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "libraryId", required = false) String libraryId,
                @RequestParam(value = "proteinName", required = false) String proteinName,
                @RequestParam(value = "sequence", required = false) String sequence,
                @RequestParam(value = "name", required = false) String name,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "isDecoy", required = false) Boolean isDecoy,
                @RequestParam(value = "pageSize", required = false, defaultValue = "30") Integer pageSize) {
        long startTime = System.currentTimeMillis();
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("proteinName", proteinName);
        model.addAttribute("sequence", sequence);
        model.addAttribute("name", name);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("isDecoy", isDecoy);
        model.addAttribute("libraries",getLibraryList());

        TransitionQuery query = new TransitionQuery();

        if (libraryId != null && !libraryId.isEmpty()) {
            query.setLibraryId(libraryId);
        }
        if (sequence != null && !sequence.isEmpty()) {
            query.setSequence(sequence);
        }
        if (proteinName != null && !proteinName.isEmpty()) {
            query.setProteinName(proteinName);
        }
        if (name != null && !name.isEmpty()) {
            query.setName(name);
        }
        if (isDecoy != null) {
            query.setIsDecoy(isDecoy);
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<TransitionDO>> resultDO = transitionService.getList(query);

        model.addAttribute("transitionList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        StringBuilder builder = new StringBuilder();
        builder.append("本次搜索耗时:").append(System.currentTimeMillis() - startTime).append("毫秒;包含搜索结果总计:")
                .append(resultDO.getTotalNum()).append("条");
        model.addAttribute("searchResult", builder.toString());
        return "transition/list";
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<TransitionDO> resultDO = transitionService.getById(id);
        if (resultDO.isSuccess()) {
            model.addAttribute("transition", resultDO.getModel());
            return "/transition/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/transition/list";
        }
    }

    @RequestMapping(value = "/createdecoy/{id}")
    String generateDecoy(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<TransitionDO> resultDO = transitionService.getById(id);

        if (resultDO.isFailured()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/transition/list";
        }

        TransitionDO transitionDO = shuffleGenerator.generate(resultDO.getModel());
        if(transitionDO != null){
            logger.info(JSON.toJSONString(transitionDO));
        }else{
            logger.info("未能够生成伪肽段");
        }
        model.addAttribute("transition", resultDO.getModel());
        return "/transition/detail";
    }

    @RequestMapping(value = "/calculator")
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
            return "/transition/calculator";
        }

        if (type == null || type.isEmpty()) {
            return "/transition/calculator";
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

        return "/transition/calculator";
    }

}