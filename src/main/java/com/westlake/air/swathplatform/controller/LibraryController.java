package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.constants.SuccessMsg;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.query.LibraryQuery;
import com.westlake.air.swathplatform.domain.query.TransitionQuery;
import com.westlake.air.swathplatform.parser.TransitionTsvParser;
import com.westlake.air.swathplatform.service.LibraryService;
import com.westlake.air.swathplatform.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Controller
@RequestMapping("library")
public class LibraryController extends BaseController {

    @Autowired
    TransitionTsvParser tsvParser;

    @Autowired
    LibraryService libraryService;

    @Autowired
    TransitionService transitionService;

    int errorListNumberLimit = 10;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        model.addAttribute("libraryList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "library/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "library/create";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "instrument", required = false) String instrument,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "file") MultipartFile file,
               RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        LibraryDO library = new LibraryDO();
        library.setName(name);
        library.setInstrument(instrument);
        library.setDescription(description);
        ResultDO resultDO = libraryService.save(library);
        if (resultDO.isFailured()) {
            logger.warn(resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute("library", library);
            return "redirect:/library/create";
        }

        if (file != null) {

            //先Parse文件,再作数据库的操作
            ResultDO result = parseAndInsertTsv(file, library);
            if (result.getErrorList() != null) {
                if (result.getErrorList().size() > errorListNumberLimit) {
                    redirectAttributes.addFlashAttribute(ERROR_MSG, "解析错误,错误的条数过多,这边只显示" + errorListNumberLimit + "条错误信息");
                    redirectAttributes.addFlashAttribute("errorList", result.getErrorList().subList(0, errorListNumberLimit));
                } else {
                    redirectAttributes.addFlashAttribute("errorList", result.getErrorList());
                }
            }

            if (result.isFailured()) {
                redirectAttributes.addFlashAttribute(ResultCode.SAVE_ERROR.getMessage(), result.getMsgInfo());
                return "redirect:/library/list";
            }

            /**
             * 如果全部存储成功,开始统计蛋白质数目,肽段数目和Transition数目
             */
            try {
                library.setProteinCount(transitionService.countByProteinName(library.getId()));
                library.setPeptideCount(transitionService.countByPeptideSequence(library.getId()));
                TransitionQuery query = new TransitionQuery();
                query.setId(library.getId());
                library.setTransitionCount(transitionService.count(query));

                libraryService.update(library);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        long deltaTime = System.currentTimeMillis() - startTime;
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.CREATE_LIBRARY_SUCCESS + "总共耗时:" + deltaTime + "毫秒;");
        return "redirect:/library/detail/" + library.getId();
    }

    @RequestMapping(value = "/aggregate/{id}")
    String aggregate(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isFailured()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }

        LibraryDO library = resultDO.getModel();
        try {
            library.setProteinCount(transitionService.countByProteinName(library.getId()));
            library.setPeptideCount(transitionService.countByPeptideSequence(library.getId()));
            TransitionQuery query = new TransitionQuery();
            query.setLibraryId(library.getId());
            library.setTransitionCount(transitionService.count(query));

            libraryService.update(library);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return "redirect:/library/detail/" + library.getId();
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isFailured()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        } else {
            model.addAttribute("library", resultDO.getModel());
            return "/library/edit";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isSuccess()) {
            model.addAttribute("library", resultDO.getModel());
            return "/library/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    String update(Model model,
                  @RequestParam(value = "id", required = true) String id,
                  @RequestParam(value = "name") String name,
                  @RequestParam(value = "instrument") String instrument,
                  @RequestParam(value = "description") String description,
                  @RequestParam(value = "file") MultipartFile file,
                  RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isSuccess()) {
            LibraryDO library = resultDO.getModel();
            library.setDescription(description);
            library.setInstrument(instrument);
            ResultDO updateResult = libraryService.update(library);
            if (updateResult.isFailured()) {
                redirectAttributes.addFlashAttribute(ResultCode.UPDATE_ERROR.getMessage(), updateResult.getMsgInfo());
                return "redirect:/library/list";
            }

            if (file != null) {
                //先Parse文件,再作数据库的操作
                ResultDO parseResult = parseAndInsertTsv(file, library);
                if (parseResult.getErrorList() != null) {
                    if (parseResult.getErrorList().size() > errorListNumberLimit) {
                        redirectAttributes.addFlashAttribute(ERROR_MSG, "解析错误,错误的条数过多,这边只显示" + errorListNumberLimit + "条错误信息");
                        redirectAttributes.addFlashAttribute("errorList", parseResult.getErrorList().subList(0, errorListNumberLimit));
                    } else {
                        redirectAttributes.addFlashAttribute("errorList", parseResult.getErrorList());
                    }
                }

                /**
                 * 如果全部存储成功,开始统计蛋白质数目,肽段数目和Transition数目
                 */
                try {
                    library.setProteinCount(transitionService.countByProteinName(library.getId()));
                    library.setPeptideCount(transitionService.countByPeptideSequence(library.getId()));
                    TransitionQuery query = new TransitionQuery();
                    query.setId(library.getId());
                    library.setTransitionCount(transitionService.count(query));

                    libraryService.update(library);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }

            }

            long deltaTime = System.currentTimeMillis() - startTime;
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.CREATE_LIBRARY_SUCCESS + "解析源文件耗时:" + deltaTime + "秒;");
            return "redirect:/library/detail/" + library.getId();


        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO resultDO = libraryService.delete(id);
        if (resultDO.isSuccess()) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_LIBRARY_SUCCESS);
            return "redirect:/library/list";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }
    }

    private ResultDO parseAndInsertTsv(MultipartFile file, LibraryDO library) {

        ResultDO resultDO = new ResultDO<>(true);
        try {
            resultDO = tsvParser.parseAndInsert(file.getInputStream(), library);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultDO;
    }
}