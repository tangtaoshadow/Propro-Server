package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.westlake.air.propro.algorithm.parser.LibraryTsvParser;
import com.westlake.air.propro.algorithm.parser.TraMLParser;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 * UpdateTime：2019-9-3 21:50:33
 */
@RestController
@RequestMapping("library")
public class LibraryController extends BaseController {

    @Autowired
    LibraryTsvParser tsvParser;
    @Autowired
    TraMLParser traMLParser;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;

    // 标准库

    /***
     * @UpdateTime 2019-9-4 21:38:14
     * @Archive 查询当前标准库列表(非公开)
     * @param currentPage 默认 1
     * @param pageSize 默认 100
     * @param searchName
     * @return
     */
    @RequestMapping(value = "/list")
    String list(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
            @RequestParam(value = "searchName", required = false) String searchName) {

        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();
        // 数据打包
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("searchName", searchName);
        data.put("pageSize", pageSize);

        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }

        if (!isAdmin()) {
            query.setCreator(getCurrentUsername());
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(0);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        data.put("libraryList", resultDO.getModel());
        data.put("totalPage", resultDO.getTotalPage());
        data.put("currentPage", currentPage);
        status = 0;
        map.put("status", status);

        map.put("data", data);

        // 返回数据

        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @UpdateTime 2019-9-3 21:59:07
     * @param currentPage
     * @param pageSize
     * @param searchName
     * @Archive 查询 irt 库
     * @return
     */
    @PostMapping(value = "/listIrt")
    String listIrt(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "1000") Integer pageSize,
            @RequestParam(value = "searchName", required = false) String searchName) {


        // 执行状态
        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        // 数据打包
        Map<String, Object> data = new HashMap<String, Object>();

        data.put("searchName", searchName);
        data.put("pageSize", pageSize);

        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }

        if (!isAdmin()) {
            query.setCreator(getCurrentUsername());
        }

        if (!isAdmin()) {
            query.setCreator(getCurrentUsername());
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(1);

        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        data.put("libraryList", resultDO.getModel());
        data.put("totalPage", resultDO.getTotalPage());
        data.put("currentPage", currentPage);

        status = 0;
        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     *
     * @UpdateTime 2019-8-27 18:40:32
     * @Archive 公共标准库
     * @param currentPage
     * @param pageSize
     * @param searchName
     * @return
     */
    @PostMapping(value = "/listPublic")
    String listPublic(@RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                      @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
                      @RequestParam(value = "searchName", required = false) String searchName) {
        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();
        // 检查参数 username
        int status = -1;

        do {
            // 验证 token
            String username = getCurrentUsername();

            if (username == null) {
                // token 不正常
                status = -2;
                break;
            }

            map.put("searchName", searchName);
            map.put("pageSize", pageSize);
            LibraryQuery query = new LibraryQuery();
            if (searchName != null && !searchName.isEmpty()) {
                query.setName(searchName);
            }

            query.setDoPublic(true);
            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            query.setType(0);
            ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

            map.put("libraryList", resultDO.getModel());
            map.put("totalPage", resultDO.getTotalPage());
            map.put("currentPage", currentPage);

            // 最后标记成功
            status = 0;
        } while (false);

        // 返回状态结果
        map.put("status", status);

        // 返回数据
        JSONObject json = new JSONObject(map);

        return json.toString();
    }

    @PostMapping(value = "/listPublicIrt")
    String listPublicIrt(Model model,
                         @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                         @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
                         @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }
        query.setDoPublic(true);
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(1);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        model.addAttribute("libraryList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "library/listPublicIrt";
    }

    @PostMapping(value = "/create")
    String create(Model model) {
        return "library/create";
    }

    /***
     * @UpdateTime 2019-8-29 10:18:41
     * @param name 库名称
     * @param libType 库类型
     * @param description 描述
     * @param libFile
     * @param prmFile
     * @return 0 正在执行插入 返回 taskId  -3 文件不存在 -4 插入失败
     */
    @PostMapping(value = "/add")
    String add(
            @RequestParam(value = "name") String name,
            @RequestParam(value = "libType") String libType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "libFile") MultipartFile libFile,
            @RequestParam(value = "prmFile", required = false) MultipartFile prmFile
    ) {

        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();

        do {
            if (libFile == null || libFile.getOriginalFilename() == null || libFile.getOriginalFilename().isEmpty()) {
                // 文件不存在
                status = -3;
                break;
            }
            LibraryDO library = new LibraryDO();
            library.setName(name);
            library.setDescription(description);
            int type = "library".equals(libType) ? 0 : 1;
            library.setType(type);
            library.setCreator(getCurrentUsername());
            ResultDO resultDO = libraryService.insert(library);
            TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
            taskService.insert(taskDO);
            if (resultDO.isFailed()) {
                // 文件插入失败 比如名称重复
                status = -4;
                break;
            }
            try {
                InputStream libFileStream = libFile.getInputStream();
                InputStream prmFileStream = null;
                System.out.println("prrmFile" + prmFile);
                if (null != prmFile && !prmFile.isEmpty()) {
                    prmFileStream = prmFile.getInputStream();
                }
                libraryTask.saveLibraryTask(library, libFileStream, libFile.getOriginalFilename(), prmFileStream, taskDO);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 注意 这里只是标记 status 并不代表库上传成功
            status = 0;
            map.put("taskId", taskDO.getId());

        } while (false);

        map.put("status", status);
        // 返回数据
        JSONObject json = new JSONObject(map);
        return json.toString();
    }

    /***
     * @Achieve 重新统计标准库的蛋白质和肽段数目
     * @UpdateTiem 2019-8-26 15:51:53
     * @param id            标准库的id
     * @return 0 succes
     */
    @PostMapping(value = "/aggregate")
    String aggregate(@RequestParam(value = "id") String id) {

        int status = -1;
        Map<String, Object> map = new HashMap<String, Object>();
        do {
            LibraryDO library = libraryService.getById(id);
            PermissionUtil.check(library);
            if (library == null) {
                // 库不存在
                status = -2;
                break;
            }

            libraryService.countAndUpdateForLibrary(library);
            status = 0;
        } while (false);
        map.put("status", status);
        // 返回数据
        JSONObject json = new JSONObject(map);
        return json.toString();
    }


    @PostMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id,
                RedirectAttributes redirectAttributes) {

        LibraryDO library = libraryService.getById(id);
        if (library == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/library/list";
        } else {
            PermissionUtil.check(library);
            model.addAttribute("library", library);
            return "library/edit";
        }
    }


    /***
     * @Archive 查询指定库的id irt库 标准库共用
     * @param id 查询库的id
     * @return
     */
    @PostMapping(value = "/detail")
    public String libraryListIdDetail(@RequestParam(value = "id") String id
    ) {

        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Object> data = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        LibraryDO library = libraryService.getById(id);
        do {

            if (library == null) {
                // 查询库为空  严重错误
                status = -3;
                break;
            } else {

                PermissionUtil.check(library);

                // IRT 库
                if (library.getType().equals(LibraryDO.TYPE_IRT)) {

                    Double[] range = peptideService.getRTRange(id);
                    if (range != null && range.length == 2) {
                        data.put("minRt", range[0]);
                        data.put("maxRt", range[1]);
                    }
                }
                data.put("libraryInfo", library);
                status = 0;
            }
        } while (false);


        // 返回状态结果
        map.put("status", status);
        // 打包数据
        map.put("data", data);


        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }


    @PostMapping(value = "/update")
    public String update(
            @RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "libType") String type,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "libFile") MultipartFile libFile,
            @RequestParam(value = "prmFile", required = false) MultipartFile prmFile
    ) {


        System.out.println(prmFile);
        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        do {

            System.out.println("2");

            LibraryDO library = libraryService.getById(id);

            if (library == null) {
                status = -2;
                break;
            }
            System.out.println("3");
            PermissionUtil.check(library);
            library.setDescription(description);

            // 注意 这里传入的是字符串
            if ("library".equals(type)) {
                library.setType(0);
            } else {
                library.setType(1);

            }
            ResultDO updateResult = libraryService.update(library);
            if (updateResult.isFailed()) {
                // 更新失败
                status = -3;
                break;
            }

            System.out.println("4");
            // 没有更新源文件
            if (libFile == null || libFile.getOriginalFilename() == null || libFile.getOriginalFilename().isEmpty()) {
                status = -4;
                break;
            }

            TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
            taskService.insert(taskDO);
            System.out.println("5");

            try {
                InputStream libFileStream = libFile.getInputStream();
                InputStream prmFileStream = null;

                if (null != prmFile && !prmFile.isEmpty()) {
                    System.out.println("null != prmFile && !prmFile.isEmpty()");
                    prmFileStream = prmFile.getInputStream();
                }


                libraryTask.saveLibraryTask(library, libFileStream, libFile.getOriginalFilename(), prmFileStream, taskDO);

            } catch (IOException e) {
                // 更新过程出错
                e.printStackTrace();
                status = -5;
                break;
            }
            System.out.println("6");

            // 更新成功
            status = 0;
            // 返回 taskId
            map.put("taskId", taskDO.getId());


        } while (false);

        // 返回状态结果
        map.put("status", status);

        // 返回数据
        JSONObject json = new JSONObject(map);

        return json.toString();

    }


    @PostMapping(value = "/delete")
    String delete(@RequestParam("id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        do {
            LibraryDO library = libraryService.getById(id);
            int type = 0;
            if (library != null) {
                type = library.getType();
            }
            PermissionUtil.check(library);
            ResultDO resultDO = libraryService.delete(id);

            // 执行删除操作
            peptideService.deleteAllByLibraryId(id);
            if (resultDO.isSuccess()) {
                // 删除成功
                status = 0;
            } else {
                // 删除失败
                status = -3;
            }

        } while (false);

        // 返回状态结果
        map.put("status", status);
        // 返回数据
        JSONObject json = new JSONObject(map);
        return json.toString();
    }

    /***
     * @UpdateTime 2019-8-30 12:53:54
     * @param id 需要公开的标准库 id
     * @return
     */
    @PostMapping(value = "/setPublic")
    String setPublic(
            @RequestParam("id") String id
    ) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        do {
            LibraryDO library = libraryService.getById(id);
            if (library == null) {
                //  库不存在
                status = -3;
            }
            PermissionUtil.check(library);
            library.setDoPublic(true);
            libraryService.update(library);

            status = 0;
        } while (false);

        // 返回状态结果
        map.put("status", status);
        // 返回数据
        JSONObject json = new JSONObject(map);
        return json.toString();
    }

    @PostMapping(value = "/search")
    ResultDO<JSONObject> search(Model model,
                                @RequestParam(value = "fragmentSequence", required = false) String fragmentSequence,
                                @RequestParam(value = "precursorMz", required = false) Float precursorMz,
                                @RequestParam(value = "experimentId", required = false) String experimentId,
                                @RequestParam(value = "libraryId", required = false) String libraryId) {
        if (fragmentSequence.length() < 3) {
            return ResultDO.buildError(ResultCode.SEARCH_FRAGMENT_LENGTH_MUST_BIGGER_THAN_3);
        }

        LibraryDO library = libraryService.getById(libraryId);
        PermissionUtil.check(library);

        ResultDO<ExperimentDO> expResult = experimentService.getById(experimentId);
        PermissionUtil.check(expResult.getModel());
        if (expResult.isFailed()) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        ExperimentDO exp = expResult.getModel();
        List<WindowRange> rangs = exp.getWindowRanges();
        WindowRange targetRang = null;
        for (WindowRange rang : rangs) {
            if (precursorMz >= rang.getStart() && precursorMz < rang.getEnd()) {
                targetRang = rang;
                break;
            }
        }
        PeptideQuery query = new PeptideQuery();
        query.setLibraryId(libraryId);
        query.setSequence(fragmentSequence);
        if (targetRang != null) {
            query.setMzStart(Double.parseDouble(targetRang.getStart().toString()));
            query.setMzEnd(Double.parseDouble(targetRang.getEnd().toString()));
        }

        List<PeptideDO> peptides = peptideService.getAll(query);

        JSONArray peptidesArray = new JSONArray();
        for (PeptideDO peptide : peptides) {
            peptidesArray.add(peptide.getPeptideRef());
        }
        JSONObject res = new JSONObject();
        res.put("peptides", peptidesArray);
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        resultDO.setModel(res);
        return resultDO;
    }

    @PostMapping(value = "overview/{id}")
    String overview(Model model, @PathVariable("id") String id) {
        LibraryDO library = libraryService.getById(id);
        PermissionUtil.check(library);

        List<PeptideDO> peptides = peptideService.getAllByLibraryId(id);
        int count = 0;
        for (PeptideDO peptide : peptides) {
            if (peptide.getFragmentMap().size() <= 3) {
                count++;
            }
        }
        return count + "个不符合要求的离子";
    }
}
