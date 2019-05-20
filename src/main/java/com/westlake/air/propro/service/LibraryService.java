package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.LibraryQuery;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface LibraryService {

    ResultDO<List<LibraryDO>> getList(LibraryQuery query);

    List<LibraryDO> getSimpleAll(Integer type);

    long count(LibraryQuery query);

    ResultDO insert(LibraryDO libraryDO);

    ResultDO update(LibraryDO libraryDO);

    ResultDO delete(String id);

    ResultDO<LibraryDO> getById(String id);

    ResultDO<LibraryDO> getByName(String name);

    String getNameById(String id);

    ResultDO parseAndInsert(LibraryDO library, InputStream in, String fileName, InputStream fastaFileStream, InputStream prmFileStream, String libraryId, TaskDO taskDO);

    void countAndUpdateForLibrary(LibraryDO library);

    void uploadFile(LibraryDO library, InputStream libFileStream, String fileName, InputStream fastaFileStream, InputStream prmFileStream, String libraryId, TaskDO taskDO);
}
