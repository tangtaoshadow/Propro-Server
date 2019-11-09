package com.westlake.air.propro.async.task;

import com.westlake.air.propro.constants.enums.TaskStatus;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("libraryTask")
public class LibraryTask extends BaseTask{

    @Autowired
    LibraryService libraryService;

    @Async(value = "uploadFileExecutor")
    public void saveLibraryTask(LibraryDO library, InputStream libFileStream, String fileName, InputStream prmFileStream, TaskDO taskDO) {
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        libraryService.uploadFile(library, libFileStream, fileName, prmFileStream, taskDO);
    }
}
