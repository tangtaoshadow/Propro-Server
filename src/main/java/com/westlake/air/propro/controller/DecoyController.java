package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.algorithm.decoy.BaseGenerator;
import com.westlake.air.propro.algorithm.decoy.generator.NicoGenerator;
import com.westlake.air.propro.algorithm.decoy.generator.ShuffleGenerator;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.westlake.air.propro.constants.Constants.MAX_UPDATE_RECORD_FOR_PEPTIDE;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-19 16:03
 */
@RestController
@RequestMapping("decoy")
public class DecoyController extends BaseController {

    @Autowired
    ShuffleGenerator shuffleGenerator;
    @Autowired
    NicoGenerator nicoGenerator;
    @Autowired
    PeptideService peptideService;

    @RequestMapping(value = "/delete")
    String delete(@RequestParam(value = "id", required = true) String id) {

        System.out.println("删除 肽段" + id);
        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        LibraryDO library = libraryService.getById(id);
        PermissionUtil.check(library);

        peptideService.deleteAllDecoyByLibraryId(id);

        libraryService.countAndUpdateForLibrary(library);
        status = 0;
        map.put("status", status);
        // 返回数据
        JSONObject json = new JSONObject(map);
        return json.toString();
    }

    // 生成伪肽段
    @RequestMapping(value = "/generate")
    String generate(Model model,
                    @RequestParam(value = "id", required = true) String id,
                    @RequestParam(value = "generator", required = false, defaultValue = "shuffle") String generator) {
        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        LibraryDO library = libraryService.getById(id);
        PermissionUtil.check(library);

        logger.info("正在删除原有伪肽段");
        //删除原有的伪肽段
        peptideService.deleteAllDecoyByLibraryId(id);
        logger.info("原有伪肽段删除完毕");
        //计算原始肽段数目
        PeptideQuery query = new PeptideQuery();
        query.setLibraryId(id);

        long totalCount = peptideService.count(query);
        int totalPage = (int) (totalCount / MAX_UPDATE_RECORD_FOR_PEPTIDE) + 1;
        query.setPageSize(MAX_UPDATE_RECORD_FOR_PEPTIDE);
        int countForInsert = 0;
        BaseGenerator bg = null;
        switch (generator) {
            case NicoGenerator.NAME:
                bg = nicoGenerator;
                library.setGenerator(NicoGenerator.NAME);
                break;
            case ShuffleGenerator.NAME:
                bg = shuffleGenerator;
                library.setGenerator(ShuffleGenerator.NAME);
                break;
            default:
                bg = shuffleGenerator;
                library.setGenerator(NicoGenerator.NAME);
        }
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<PeptideDO>> resultDO = peptideService.getList(query);
            List<PeptideDO> list = resultDO.getModel();
            bg.generate(list);
            ResultDO resultTmp = peptideService.updateDecoyInfos(list);
            if (resultTmp.isSuccess()) {
                countForInsert += list.size();
                logger.info(i + "新生成伪肽段" + countForInsert + "条");
            }
        }

        libraryService.countAndUpdateForLibrary(library);

        logger.info("肽段生成完成");
        status = 0;
        map.put("status", status);
        // 重定向到详情
        // 返回数据  有可能不能正常返回状态连接就断了
        JSONObject json = new JSONObject(map);
        return json.toString();
    }


}
