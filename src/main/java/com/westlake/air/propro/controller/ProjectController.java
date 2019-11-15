package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.westlake.air.propro.component.ChunkUploader;
import com.westlake.air.propro.config.VMProperties;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.constants.SuffixConst;
import com.westlake.air.propro.constants.SymbolConst;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.ScoreType;
import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.db.simple.PeptideIntensity;
import com.westlake.air.propro.domain.db.simple.SimpleExperiment;
import com.westlake.air.propro.domain.params.ExtractParams;
import com.westlake.air.propro.domain.params.IrtParams;
import com.westlake.air.propro.domain.params.WorkflowParams;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.domain.vo.FileBlockVO;
import com.westlake.air.propro.domain.vo.FileVO;
import com.westlake.air.propro.domain.vo.UploadVO;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 10:00
 */
@RestController
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
    SwathIndexService swathIndexService;
    @Autowired
    VMProperties vmProperties;
    @Autowired
    ChunkUploader chunkUploader;

    /***
     * @UpdateTime 2019-10-13 18:52:38 tangtao
     * @Archive 查询项目列表
     * @param currentPage
     * @param pageSize
     * @param name
     * @return
     */
    @PostMapping(value = "/list")
    String list(
            @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
            @RequestParam(value = "pageSize", required = false, defaultValue = "500") Integer pageSize,
            @RequestParam(value = "name", required = false) String name) {

        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            data.put("name", name);
            data.put("pageSize", pageSize);

            ProjectQuery query = new ProjectQuery();

            if (name != null && !name.isEmpty()) {
                query.setName(name);
            }

            if (!isAdmin()) {
                query.setOwnerName(getCurrentUsername());
            }

            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            ResultDO<List<ProjectDO>> resultDO = projectService.getList(query);

            data.put("repository", RepositoryUtil.getRepo());
            data.put("projectList", resultDO.getModel());
            data.put("totalPage", resultDO.getTotalPage());
            data.put("totalNumbers", resultDO.getTotalNum());
            data.put("currentPage", currentPage);

            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    @PostMapping(value = "/create")
    String create(Model model) {

        model.addAttribute("libraries", getLibraryList(0, true));
        model.addAttribute("iRtLibraries", getLibraryList(1, true));

        return "project/create";
    }





    @PostMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "type", required = true) String type,
               @RequestParam(value = "libraryId", required = false) String libraryId,
               @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
               RedirectAttributes redirectAttributes) {

        String ownerName = getCurrentUsername();

        model.addAttribute("ownerName", ownerName);
        model.addAttribute("name", name);
        model.addAttribute("description", description);
        model.addAttribute("type", type);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("iRtLibraryId", iRtLibraryId);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName(name);
        projectDO.setDescription(description);
        projectDO.setOwnerName(ownerName);
        projectDO.setType(type);
        LibraryDO lib = libraryService.getById(libraryId);
        if (lib != null) {
            projectDO.setLibraryId(lib.getId());
            projectDO.setLibraryName(lib.getName());
        }

        LibraryDO iRtLib = libraryService.getById(iRtLibraryId);
        if (iRtLib != null) {
            projectDO.setIRtLibraryId(iRtLib.getId());
            projectDO.setIRtLibraryName(iRtLib.getName());
        }

        ResultDO result = projectService.insert(projectDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "project/create";
        }

        //Project创建成功再去创建文件夹
        String repository = vmProperties.getRepository();
        File file = new File(FilenameUtils.concat(repository, name));
        if (!file.exists()) {
            file.mkdirs();
        }

        return "redirect:/project/list";
    }


    /***
     *
     * @updateTime 2019-10-17 23:52:22 tangtao
     * @archive 编辑 project list
     * @param id
     * @return 0 success
     */
    @PostMapping(value = "/edit")
    String edit(@RequestParam("id") String id) {
        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();


        do {
            ProjectDO project = projectService.getById(id);
            if (project == null) {
                status = -2;
                break;
                // redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED.getMessage());
                // return "redirect:/project/list";
            }

            PermissionUtil.check(project);
            data.put("libraryId", project.getLibraryId());
            data.put("iRtLibraryId", project.getIRtLibraryId());
            data.put("libraries", getLibraryList(0, true));
            data.put("iRtLibraries", getLibraryList(1, true));
            data.put("project", project);
            status = 0;
        } while (false);


        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @archive 更新项目列表
     * @update tangtao 2019-10-23 12:40:21
     * @param id
     * @param description
     * @param type
     * @param libraryId
     * @param iRtLibraryId
     * @return
     */
    @PostMapping(value = "/update")
    String update(@RequestParam("id") String id,
                  @RequestParam(value = "description", required = false) String description,
                  @RequestParam(value = "type") String type,
                  @RequestParam(value = "libraryId", required = false) String libraryId,
                  @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId) {


        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();


        do {
            ProjectDO project = projectService.getById(id);
            try {
                PermissionUtil.check(project);
            } catch (Exception e) {
                status = -2;
                break;
            }

            project.setDescription(description);
            project.setType(type);
            LibraryDO lib = libraryService.getById(libraryId);
            if (lib != null) {
                project.setLibraryId(lib.getId());
                project.setLibraryName(lib.getName());
            }

            LibraryDO iRtLib = libraryService.getById(iRtLibraryId);
            if (iRtLib != null) {
                project.setIRtLibraryId(iRtLib.getId());
                project.setIRtLibraryName(iRtLib.getName());
            }

            ResultDO result = projectService.update(project);
            if (result.isFailed()) {
                data.put("errorMsg", result.getMsgInfo());
                status = -3;
                break;
            }

            status = 0;
        } while (false);


        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }

    /***
     * @Archive 文件管理 tangtao 2019-10-18 13:21:09
     * @param name
     * @return 0 status
     */
    @PostMapping(value = "/filemanager")
    String fileManager(@RequestParam(value = "projectName") String name) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {
            ProjectDO project = projectService.getByName(name);
            try {
                PermissionUtil.check(project);
            } catch (Exception e) {
                status = -2;
                break;
            }

            List<File> fileList = FileUtil.scanFiles(name);
            List<FileVO> fileVOList = new ArrayList<>();

            for (File file : fileList) {
                FileVO fileVO = new FileVO();
                // 返回文件名称 和 大小 即可
                fileVO.setName(file.getName());
                fileVO.setSize(file.length());
                fileVOList.add(fileVO);
            }

            data.put("project", project);
            data.put("fileList", fileVOList);

            status = 0;
        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @author tangtao https://www.promiselee.cn/tao
     * @updateTime 2019-11-10 01:32:26
     * @archive 删除指定项目下的指定的文件
     * @param fileName
     * @param projectName
     * @return 0 删除成功
     */
    @RequestMapping(value = "/deleteFile")
    @ResponseBody
    String deleteFile(@RequestParam(value = "fileName") String fileName,
                      @RequestParam(value = "projectName") String projectName
    ) {

        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();
        // 添加好请求信息 供前端判断
        data.put("fileName",fileName);
        data.put("projectName",projectName);

        do {

            try {
                // 尝试提取项目名字
                ProjectDO project = projectService.getByName(projectName);
                PermissionUtil.check(project);
                File file = FileUtil.getFile(projectName, fileName);
                if (null != file) {
                    //  文件存在 执行删除
                    if(FileUtil.deleteFile(file.toString())){
                        //  删除成功
                        data.put("info","delete success");
                    }else{
                        status=-4;
                        data.put("errorInfo","删除失败");
                        break;
                    }
                } else {
                    // 文件不存在
                    status = -3;
                    data.put("errorInfo","文件不存在");
                    break;
                }
                // 成功删除文件
                status = 0;
                break;
            } catch (Exception e) {
                //
                status = -2;
                break;
            }
        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    @PostMapping(value = "/doupload")
    ResultDO doUpload(Model model,
                      @RequestParam(value = "projectName", required = true) String projectName,
                      @RequestParam("file") MultipartFile file,
                      @FormParam("form-data") UploadVO uploadVO) {

        ProjectDO project = projectService.getByName(projectName);
        PermissionUtil.check(project);
        model.addAttribute("project", project);
        chunkUploader.chunkUpload(file, uploadVO, projectName);
        return new ResultDO(true);
    }

    @PostMapping(value = "/check")
    public Object check(FileBlockVO block,
                        @RequestParam(value = "projectName", required = true) String projectName) {
        return chunkUploader.check(block, projectName);
    }


    @PostMapping(value = "/scan")
    String scan(
            @RequestParam(value = "projectId") String projectId) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ProjectDO project = projectService.getById(projectId);
            PermissionUtil.check(project);

            List<File> fileList = FileUtil.scanFiles(project.getName());
            List<File> newFileList = new ArrayList<>();
            List<ExperimentDO> exps = experimentService.getAllByProjectId(projectId);
            List<String> existedExpNames = new ArrayList<>();

            for (ExperimentDO exp : exps) {
                existedExpNames.add(exp.getName());
            }

            //过滤文件
            for (File file : fileList) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(SuffixConst.JSON) && !existedExpNames.contains(FilenameUtils.getBaseName(file.getName()))) {
                    newFileList.add(file);
                }
            }

            if (newFileList.isEmpty()) {
                // 没有扫描到新实验
                data.put("errorMsg", ResultCode.NO_NEW_EXPERIMENTS.getMessage());
                status = -2;
                break;
            }

            TaskDO taskDO = new TaskDO(TaskTemplate.SCAN_AND_UPDATE_EXPERIMENTS, project.getName());
            taskDO.addLog(newFileList.size() + " total");
            taskService.insert(taskDO);
            List<ExperimentDO> expsToUpdate = new ArrayList<>();

            for (File file : newFileList) {
                ExperimentDO exp = new ExperimentDO();
                exp.setName(FilenameUtils.getBaseName(file.getName()));
                exp.setOwnerName(project.getOwnerName());
                exp.setProjectId(project.getId());
                exp.setProjectName(project.getName());
                exp.setType(project.getType());
                ResultDO result = experimentService.insert(exp);
                if (result.isFailed()) {
                    taskDO.addLog("ERROR-" + exp.getId() + "-" + exp.getName());
                    taskDO.addLog(result.getMsgInfo());
                    taskService.update(taskDO);
                }
                expsToUpdate.add(exp);
            }


            experimentTask.uploadAird(expsToUpdate, taskDO);
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }

    /***
     * @author tangtao https://www.promiselee.cn/tao
     * @updateTiem 2019-11-6 10:51:01
     * @archive 查找指定项目下的指定的文件
     * @param fileName
     * @param projectName
     * @return
     */
    @PostMapping(value = "/checkFile")
    String checkFile(@RequestParam(value = "fileName") String fileName,
                     @RequestParam(value = "projectName") String projectName
    ) {

        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {
            try {
                // 尝试提取项目名字
                ProjectDO project = projectService.getByName(projectName);
                PermissionUtil.check(project);
                File file = FileUtil.getFile(projectName, fileName);
                if (null != file) {
                } else {
                    // 文件不存在
                    status = -3;
                    break;
                }
                // 成功找到文件
                status = 0;
                break;
            } catch (Exception e) {
                //
                status = -2;
                break;
            }
        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @createAuthor tangtao
     * @createTime 2019-11-6 09:18:32
     * @updateTime 2019-11-6 19:28:57
     * @archive 读取json文件
     * @param fileName
     * @param projectName
     * @return
     */
    @PostMapping(value = "/readJsonFile")
    @ResponseBody
    String readJsonFile(@RequestParam(value = "fileName") String fileName,
                        @RequestParam(value = "projectName") String projectName
    ) {

        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {
            try {
                // 尝试提取项目名字
                ProjectDO project = projectService.getByName(projectName);
                PermissionUtil.check(project);
                File file = FileUtil.getFile(projectName, fileName);
                if (null != file) {
                } else {
                    // 文件不存在
                    status = -3;
                    break;
                }

                // 成功找到文件
                // 提前写入 status=0 便于代码阅读 和理解
                // 其次这样即使下面出错了会捕获 而且status会重新设定
                // 仍能保证 status=0 才是成功
                status = 0;

                /******** 开始读取json 文件 并且进行计算分片位置 *********/
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(
                        new FileReader(String.valueOf(file))
                );
                //
                JSONArray indexList = (JSONArray) jsonObject.get("indexList");

                /** 思路：tangtao
                 * 存储文件分片位置 但是整数多大可能不太确定 使用 BigInteger 就可以不考虑大小
                 * 这里不使用 ArrayList 因为 BigInteger 这种方式快 充分利用了索引
                 * 而且整个处理逻辑简单
                 */
                BigInteger fragmentList[] = 0 < indexList.size() ? new BigInteger[indexList.size()] : null;

                Stream.iterate(0, i -> i + 1).limit(indexList.size()).forEach(i -> {
                    JSONObject obj = (JSONObject) indexList.get(i);
                    // tangtao at 2019-11-6 10:43:19
                    // 因为文件分片数据很大 采用大数存储计算
                    BigInteger start = new BigInteger(obj.get("startPtr").toString());
                    BigInteger end = new BigInteger(obj.get("endPtr").toString());
                    BigInteger res = end.subtract(start);
                    fragmentList[i] = res;
                });

                // 写入文件分片列表
                data.put("fileFragmentList", fragmentList);
                // 写回前端 告诉前端这个是哪个json文件的 否则前端不知道
                data.put("jsonFileName", fileName);
            } catch (Exception e) {
                // 可能出现转换异常等等错误 都归结为出错
                status = -2;
                break;
            }
        } while (false);


        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
    }


    /***
     * @archive 批量计算irt
     * @update tangtao at 2019-10-23 13:35:30 https://www.promiselee.cn/tao
     * @param id
     * @return
     */
    @PostMapping(value = "/irt")
    String irt(
            @RequestParam(value = "id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;

        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ProjectDO project = projectService.getById(id);
            try {

                PermissionUtil.check(project);

            } catch (Exception e) {
                status = -2;
                break;
            }
            List<ExperimentDO> expList = experimentService.getAllByProjectName(project.getName());

            data.put("exps", expList);
            data.put("project", project);
            data.put("iRtLibraryId", project.getIRtLibraryId());
            data.put("iRtLibraries", getLibraryList(1, true));

            // success
            status = 0;
        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }


    /***
     * @update tangtao at 2019-10-24 23:18:42
     * @Archive 计算irt 计算成功后返回任务列表
     * @param id
     * @param iRtLibraryId
     * @param sigma
     * @param spacing
     * @param mzExtractWindow
     * @return 0 success -3 实验列表为空 -2 校验失败
     */
    @PostMapping(value = "/doirt")
    String doIrt(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
            @RequestParam(value = "sigma", defaultValue = Constants.DEFAULT_SIGMA_STR) Float sigma,
            @RequestParam(value = "spacing", defaultValue = Constants.DEFAULT_SPACING_STR) Float spacing,
            @RequestParam(value = "mzExtractWindow", defaultValue = Constants.DEFAULT_MZ_EXTRACTION_WINDOW_STR) Float mzExtractWindow) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ProjectDO project = projectService.getById(id);
            try {
                PermissionUtil.check(project);
            } catch (Exception e) {
                status = -2;
                break;
            }

            List<ExperimentDO> expList = experimentService.getAllByProjectId(id);
            if (expList == null) {
                // 该项目实验数据为空
                status = -3;
                data.put("info", ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
                break;
            }

            TaskDO taskDO = new TaskDO(TaskTemplate.IRT, project.getName() + ":" + iRtLibraryId + "-Num:" + expList.size());
            taskService.insert(taskDO);
            SigmaSpacing sigmaSpacing = new SigmaSpacing(sigma, spacing);

            // 支持直接使用标准库进行irt预测,在这里进行库的类型的检测,已进入不同的流程渠道
            LibraryDO lib = libraryService.getById(iRtLibraryId);
            IrtParams irtParams = new IrtParams();
            irtParams.setLibrary(lib);
            irtParams.setMzExtractWindow(mzExtractWindow);
            irtParams.setSigmaSpacing(sigmaSpacing);
            experimentTask.irt(taskDO, expList, irtParams);

            // success
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);


        // 返回数据  前端接收到数据之后应该定向到 task/list
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    @PostMapping(value = "/setPublic/{id}")
    String setPublic(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        ProjectDO project = projectService.getById(id);
        if (project == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED.getMessage());
            return "redirect:/project/list";
        }
        PermissionUtil.check(project);

        project.setDoPublic(true);
        projectService.update(project);

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.SET_PUBLIC_SUCCESS);
        return "redirect:/project/list";
    }

    @PostMapping(value = "/deleteirt/{id}")
    String deleteIrt(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {

        ProjectDO project = projectService.getById(id);
        PermissionUtil.check(project);

        List<ExperimentDO> expList = experimentService.getAllByProjectId(id);
        if (expList == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
            return "redirect:/project/list";
        }
        for (ExperimentDO experimentDO : expList) {
            experimentDO.setIrtResult(null);
            experimentService.update(experimentDO);
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @PostMapping(value = "/deleteAll/{id}")
    String deleteAll(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        ProjectDO project = projectService.getById(id);
        PermissionUtil.check(project);

        List<ExperimentDO> expList = experimentService.getAllByProjectId(id);
        for (ExperimentDO experimentDO : expList) {
            String expId = experimentDO.getId();
            experimentService.delete(expId);
            swathIndexService.deleteAllByExpId(expId);
            analyseOverviewService.deleteAllByExpId(experimentDO.getId());
        }

        projectService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }


    @PostMapping(value = "/deleteAnalyse/{id}")
    String deleteAnalyse(@PathVariable("id") String id,
                         RedirectAttributes redirectAttributes) {

        ProjectDO project = projectService.getById(id);
        PermissionUtil.check(project);

        String name = project.getName();
        List<ExperimentDO> expList = experimentService.getAllByProjectName(name);
        for (ExperimentDO experimentDO : expList) {
            analyseOverviewService.deleteAllByExpId(experimentDO.getId());
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }


    /***
     * @updateTime tangtao at 2019-10-25 10:42:03
     * @archive 对应前端 批量执行完整流程
     * @param id
     * @return -3 不存在项目 0 success -2 project 没有通过校验 抛出了异常
     */
    @PostMapping(value = "/extractor")
    String extractor(
            @RequestParam(value = "id") String id) {

        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {
            ProjectDO project = projectService.getById(id);
            try {
                PermissionUtil.check(project);

            } catch (Exception e) {
                status = -2;
                break;
            }
            if (project == null) {
                // 项目为空
                status = -3;
                data.put("errorMsg", ResultCode.PROJECT_NOT_EXISTED.getMessage());
                break;
            }

            List<ExperimentDO> expList = experimentService.getAllByProjectName(project.getName());
            data.put("libraryId", project.getLibraryId());
            data.put("iRtLibraryId", project.getIRtLibraryId());
            data.put("exps", expList);
            data.put("librariedoextracts", getLibraryList(0, true));
            data.put("iRtLibraries", getLibraryList(1, true));
            data.put("project", project);
            data.put("scoreTypes", ScoreType.getShownTypes());

            // success
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    /***
     * @archive 提取数据
     * @update tangtao at 2019-10-26 17:37:45
     * @param id
     * @param iRtLibraryId
     * @param libraryId
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param note
     * @param fdr
     * @param sigma
     * @param spacing
     * @param shapeScoreThreshold
     * @param shapeWeightScoreThreshold
     * @param request
     * @return
     */
    @PostMapping(value = "/doextract")
    String doExtract(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
            @RequestParam(value = "libraryId") String libraryId,
            @RequestParam(value = "rtExtractWindow", defaultValue = Constants.DEFAULT_RT_EXTRACTION_WINDOW_STR) Float rtExtractWindow,
            @RequestParam(value = "mzExtractWindow", defaultValue = Constants.DEFAULT_MZ_EXTRACTION_WINDOW_STR) Float mzExtractWindow,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "fdr", defaultValue = Constants.DEFAULT_FDR_STR) Double fdr,
            //打分相关的入参
            @RequestParam(value = "sigma", required = false, defaultValue = Constants.DEFAULT_SIGMA_STR) Float sigma,
            @RequestParam(value = "spacing", required = false, defaultValue = Constants.DEFAULT_SPACING_STR) Float spacing,
            @RequestParam(value = "shapeScoreThreshold", required = false, defaultValue = Constants.DEFAULT_SHAPE_SCORE_THRESHOLD_STR) Float shapeScoreThreshold,
            @RequestParam(value = "shapeWeightScoreThreshold", required = false, defaultValue = Constants.DEFAULT_SHAPE_WEIGHT_SCORE_THRESHOLD_STR) Float shapeWeightScoreThreshold,
            HttpServletRequest request) {


        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ProjectDO project = projectService.getById(id);
            if (project == null) {
                // 项目为空
                data.put("errorMsg", ResultCode.PROJECT_NOT_EXISTED.getMessage());
                status = -3;
                break;
                // return "redirect:/project/extractor?id=" + id;
            }

            try {
                PermissionUtil.check(project);
            } catch (Exception e) {
                status = -2;
                break;
            }

            LibraryDO library = libraryService.getById(libraryId);
            if (library == null) {
                status = -4;
                data.put("errorMsg", ResultCode.LIBRARY_NOT_EXISTED.getMessage());
                break;
                // return "redirect:/project/extractor?id=" + id;
            }

            boolean doIrt = false;
            LibraryDO irtLibrary = null;
            if (iRtLibraryId != null && !iRtLibraryId.isEmpty()) {
                irtLibrary = libraryService.getById(iRtLibraryId);
                if (irtLibrary == null) {
                    status = -5;
                    data.put("errorMsg", ResultCode.IRT_LIBRARY_NOT_EXISTED.getMessage());
                    // return "redirect:/project/extractor?id=" + id;
                    break;
                }

                doIrt = true;
            }

            List<String> scoreTypes = ScoreUtil.getScoreTypes(request);
            List<ExperimentDO> exps = experimentService.getAllByProjectId(id);
            if (exps == null) {
                // 实验为空
                data.put("errorMsg", ResultCode.NO_EXPERIMENT_UNDER_PROJECT.getMessage());
                status = 6;
                break;
                // return "redirect:/project/list";
            }

            String errorInfo = "";
            TaskTemplate template = null;
            if (doIrt) {
                template = TaskTemplate.IRT_EXTRACT_PEAKPICK_SCORE;
            } else {
                template = TaskTemplate.EXTRACT_PEAKPICK_SCORE;
            }

            for (ExperimentDO exp : exps) {
                if (!doIrt && (exp.getIrtResult() == null)) {
                    errorInfo = errorInfo + ResultCode.IRT_FIRST + ":" + exp.getName() + "(" + exp.getId() + ")";
                    continue;
                }

                TaskDO taskDO = new TaskDO(template, exp.getName() + ":" + library.getName() + "(" + libraryId + ")");
                taskService.insert(taskDO);

                WorkflowParams input = new WorkflowParams();
                SigmaSpacing ss = new SigmaSpacing(sigma, spacing);
                input.setSigmaSpacing(ss);
                input.setExperimentDO(exp);
                input.setFdr(fdr);
                if (doIrt) {
                    input.setIRtLibrary(irtLibrary);
                } else {
                    input.setSlopeIntercept(exp.getIrtResult().getSi());
                }

                input.setLibrary(library);
                input.setOwnerName(getCurrentUsername());
                input.setExtractParams(new ExtractParams(mzExtractWindow, rtExtractWindow));
                input.setScoreTypes(scoreTypes);
                input.setXcorrShapeThreshold(shapeScoreThreshold);
                input.setXcorrShapeWeightThreshold(shapeWeightScoreThreshold);
                experimentTask.extract(taskDO, input);
            }

            if (StringUtils.isNotEmpty(errorInfo)) {
                data.put("errorMsg", errorInfo);
            }

            // success
            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据 前端接收到后应跳转到任务列表
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);
        // return "redirect:/task/list";
    }


    /***
     * @update tangtao at 2019-11-3 18:43:19
     * @archive 实验部分选取控制面板
     * @param id
     * @return
     */
    @PostMapping(value = "/portionSelector")
    String portionSelector(
            @RequestParam(value = "id", required = true) String id
    ) {
        Map<String, Object> map = new HashMap<String, Object>();
        // 状态标记
        int status = -1;
        Map<String, Object> data = new HashMap<String, Object>();

        do {

            ProjectDO project = projectService.getById(id);
            if (project == null) {
                status = -2;
                data.put("errorMsg", ResultCode.PROJECT_NOT_EXISTED);
                break;
            }

            try {
                //
                PermissionUtil.check(project);

            } catch (Exception e) {
                //
                status = -3;
                break;
            }

            List<ExperimentDO> expList = experimentService.getAllByProjectName(project.getName());
            List<ExperimentDO> collect = expList.stream().sorted(Comparator.comparing(ExperimentDO::getName)).collect(Collectors.toList());
            data.put("project", project);
            data.put("expList", collect);

            status = 0;

        } while (false);

        map.put("status", status);
        map.put("data", data);

        // 返回数据 前端接收到后应跳转到任务列表
        return JSON.toJSONString(map, SerializerFeature.WriteNonStringKeyAsString);

    }

    @PostMapping(value = "/overview")
    String overview(Model model,
                    @RequestParam(value = "id", required = true) String projectId,
                    @RequestParam(value = "peptideRefInfo", required = false) String peptideRefInfo,
                    @RequestParam(value = "proteinNameInfo", required = false) String proteinNameInfo,
                    HttpServletRequest request,
                    RedirectAttributes redirectAttributes) {

        //get project name
        ProjectDO project = projectService.getById(projectId);
        if (project == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }

        PermissionUtil.check(project);
        String projectName = project.getName() + "(" + project.getId() + ")";
        //get corresponding experiments
        List<ExperimentDO> expList = experimentService.getAllByProjectName(project.getName());
        expList.sort(new Comparator<ExperimentDO>() {
            @Override
            public int compare(ExperimentDO o1, ExperimentDO o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
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
                List<PeptideDO> peptideDOList = peptideService.getAllByLibraryIdAndProteinName(libraryId, proteinName);
                for (PeptideDO peptideDO : peptideDOList) {
                    peptideDOMap.put(peptideDO.getPeptideRef(), peptideDO);
                }
            }
        }
        if (!peptideRefInfo.isEmpty()) {
            String[] peptideRefs = peptideRefInfo.split(";");
            for (String peptideRef : peptideRefs) {
                PeptideDO peptideDO = peptideService.getByLibraryIdAndPeptideRef(libraryId, peptideRef);
                if (peptideDO == null) {
                    continue;
                }
                peptideDOMap.put(peptideRef, peptideDO);
            }
        }

        HashMap<String, List<Boolean>> identifyMap = new HashMap<>();
        for (PeptideDO peptideDO : peptideDOMap.values()) {
            //protein name
            protNameList.add(peptideDO.getProteinName());
            //fragment cutInfo list
            Set<String> cutInfoSet = peptideDO.getFragmentMap().keySet();
            //experiments
            HashMap<String, List<Integer>> fragIntListMap = new HashMap<>();
            String intOverall = "";
            List<Boolean> identifyStatList = new ArrayList<>();
            for (String analyseOverviewId : analyseOverviewIdList) {
                //get fragment intensity map
                AnalyseDataDO analyseDataDO = analyseDataService.getByOverviewIdAndPeptideRefAndIsDecoy(analyseOverviewId, peptideDO.getPeptideRef(), false);
                Map<String, Double> fragIntMap;
                if (analyseDataDO == null) {
                    fragIntMap = new HashMap<>();
                    identifyStatList.add(false);
                } else {
                    if (analyseDataDO.getIdentifiedStatus() == 0) {
                        identifyStatList.add(true);
                    } else {
                        identifyStatList.add(false);
                    }
                    fragIntMap = FeatureUtil.toMap(analyseDataDO.getFragIntFeature());
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
            if (intOverall.isEmpty()) {
                intOverall = "0";
            }
            identifyMap.put(peptideDO.getPeptideRef(), identifyStatList);
            intMap.put(peptideDO.getPeptideRef(), intOverall);
            pepFragIntListMap.put(peptideDO.getPeptideRef(), fragIntListMap);
        }

        //  横坐标实验，纵坐标不同pep
        model.addAttribute("projectName", projectName);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("libName", libName);
        model.addAttribute("protNameList", protNameList);
        model.addAttribute("pepFragIntListMap", pepFragIntListMap);
        model.addAttribute("expNameList", expNameList);
        model.addAttribute("intMap", intMap);
        model.addAttribute("identifyMap", identifyMap);

        return "project/overview";
    }

    @PostMapping(value = "/writeToFile")
    String writeToFile(Model model,
                       @RequestParam(value = "id", required = true) String id,
                       RedirectAttributes redirectAttributes) {

        ProjectDO projectDO = projectService.getById(id);
        PermissionUtil.check(projectDO);
        List<ExperimentDO> experimentDOList = experimentService.getAllByProjectId(id);
        String defaultOutputPath = RepositoryUtil.buildOutputPath(projectDO.getName(), projectDO.getName() + ".tsv");
        model.addAttribute("expList", experimentDOList);
        model.addAttribute("project", projectDO);
        model.addAttribute("defaultPathName", defaultOutputPath);
        model.addAttribute("outputAllPeptides", false);
        return "project/outputSelector";
    }


    @PostMapping(value = "/doWriteToFile")
    void doWriteToFile(Model model,
                       @RequestParam(value = "projectId", required = true) String projectId,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       RedirectAttributes redirectAttributes) {

        ProjectDO projectDO = projectService.getById(projectId);
        PermissionUtil.check(projectDO);

        List<SimpleExperiment> experimentList = experimentService.getAllSimpleExperimentByProjectId(projectId);
        HashMap<String, HashMap<String, String>> intensityMap = new HashMap<>();//key为PeptideRef, value为另外一个Map,map的key为ExperimentName,value为intensity值
        HashMap<String, String> pepToProt = new HashMap<>();//key为PeptideRef,value为ProteinName

        for (SimpleExperiment simpleExp : experimentList) {
            String checkState = request.getParameter(simpleExp.getId());
            if (checkState != null && checkState.equals("on")) {
                //  取每一个实验的第一个分析结果进行分析
                AnalyseOverviewDO analyseOverview = analyseOverviewService.getFirstAnalyseOverviewByExpId(simpleExp.getId());
                if (analyseOverview == null) {
                    continue;
                }
                List<PeptideIntensity> peptideIntensityList = analyseDataService.getPeptideIntensityByOverviewId(analyseOverview.getId());
                for (PeptideIntensity peptideIntensity : peptideIntensityList) {
                    if (intensityMap.containsKey(peptideIntensity.getPeptideRef())) {
                        intensityMap.get(peptideIntensity.getPeptideRef()).put(simpleExp.getName(), peptideIntensity.getIntensitySum().toString());
                    } else {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(simpleExp.getName(), peptideIntensity.getIntensitySum().toString());
                        intensityMap.put(peptideIntensity.getPeptideRef(), map);
                        pepToProt.put(peptideIntensity.getPeptideRef(), peptideIntensity.getProteinName());
                    }
                }
            }
        }

        try {
            OutputStream os = response.getOutputStream();
            StringBuffer sb = new StringBuffer();

            sb.append("ProteinName").append(SymbolConst.TAB).append("PeptideRef");
            for (SimpleExperiment simpleExperiment : experimentList) {
                sb.append(SymbolConst.TAB).append(simpleExperiment.getName());
            }
            sb.append(SymbolConst.RETURN);
            for (String peptideRef : intensityMap.keySet()) {
                sb.append(pepToProt.get(peptideRef)).append(SymbolConst.TAB).append(peptideRef);
                for (SimpleExperiment simpleExperiment : experimentList) {
                    if (intensityMap.get(peptideRef).containsKey(simpleExperiment.getName())) {
                        sb.append(SymbolConst.TAB).append(intensityMap.get(peptideRef).get(simpleExperiment.getName()));
                    } else {
                        sb.append(SymbolConst.TAB);
                    }
                }
                sb.append(SymbolConst.RETURN);
            }

            response.setHeader("content-type", "application/octet-stream");
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("gbk");
            response.setHeader("Cache-Control", "max-age=60");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(projectDO.getName() + ".tsv", "gbk"));
            os.write(sb.toString().getBytes("gbk"));
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
