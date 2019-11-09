package com.westlake.air.propro.test.library;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.algorithm.parser.TraMLParser;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 14:11
 */
public class LibraryTest extends BaseTest {

    @Autowired
    TraMLParser traMLParser;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;

    @Test
    public void extractor_tsv_parser_Test_1() throws Exception {
        LibraryDO libraryDO = new LibraryDO();
        libraryDO.setName("测试用临时库");
        libraryDO.setType(LibraryDO.TYPE_STANDARD);
        libraryService.insert(libraryDO);
        String filePath = getClass().getClassLoader().getResource("ChromatogramExtractor_input.tsv").getPath();
        File file = new File(filePath);
        ResultDO resultDO = libraryService.parseAndInsert(libraryDO, new FileInputStream(file), filePath, null, new TaskDO());
        assert resultDO.isSuccess();
        List<PeptideDO> trans = peptideService.getAllByLibraryId(libraryDO.getId());
        assert trans.size() == 3;

        peptideService.deleteAllByLibraryId(libraryDO.getId());
        libraryService.delete(libraryDO.getId());
    }

}
