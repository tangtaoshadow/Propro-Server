package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.westlake.air.propro.utils.PeptideUtil.removeUnimod;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 21:13
 */
public abstract class BaseLibraryParser {

    public final Logger logger = LoggerFactory.getLogger(BaseLibraryParser.class);

    @Autowired
    PeptideService peptideService;
    @Autowired
    TaskService taskService;

    public abstract ResultDO parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO);

    public abstract ResultDO selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO);

    protected BufferedReader prepareForReader (InputStream in, LibraryDO library, TaskDO taskDO) throws Exception {

        //开始插入前先清空原有的数据库数据
        ResultDO resultTmp = peptideService.deleteAllByLibraryId(library.getId());
        if (resultTmp.isFailed()) {
            logger.error(resultTmp.getMsgInfo());
            throw new Exception(ResultCode.DELETE_ERROR.getMessage());
        }
        taskDO.addLog("删除旧数据完毕,开始文件解析");
        taskService.update(taskDO);

        InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);

        return reader;
    }



    /**
     * 从去除的Peptide推断的Protein中删除含有未去除Peptide的Protein
     *
     * @param dropSet   非Unique的Peptide推断得到的ProtSet
     * @param uniqueSet Unique的Peptide推断得到的ProtSet
     * @return 非Unique蛋白的数量
     */
    protected int getDropCount(HashSet<String> dropSet, HashSet<String> uniqueSet) {
        List<String> dropList = new ArrayList<>(dropSet);
        int dropCount = dropList.size();
        for (String prot : dropList) {
            if (uniqueSet.contains(prot)) {
                dropCount--;
            }
        }
        return dropCount;
    }

    protected void setUnique(PeptideDO peptide, HashSet<String> fastaUniqueSet, HashSet<String> fastaDropPep, HashSet<String> libraryDropPep, HashSet<String> fastaDropProt, HashSet<String> libraryDropProt, HashSet<String> uniqueProt) {
        if (peptide.getProteinName().startsWith("1/")) {
            if (!fastaUniqueSet.isEmpty() && !fastaUniqueSet.contains(peptide.getSequence())) {
                peptide.setIsUnique(false);
                fastaDropProt.add(peptide.getProteinName());
                fastaDropPep.add(peptide.getPeptideRef());
            } else {
                uniqueProt.add(peptide.getProteinName());
            }
        } else {
            peptide.setIsUnique(false);
            libraryDropProt.add(peptide.getProteinName());
            libraryDropPep.add(peptide.getPeptideRef());
        }
    }

    protected void addFragment(PeptideDO peptide, HashMap<String, PeptideDO> map) {
        PeptideDO existedPeptide = map.get(peptide.getPeptideRef());
        if (existedPeptide == null) {
            map.put(peptide.getPeptideRef(), peptide);
        } else {
            for (String key : peptide.getFragmentMap().keySet()) {
                existedPeptide.putFragment(key, peptide.getFragmentMap().get(key));
            }
        }
    }

    protected HashMap<String, Integer> parseColumns(String line) {
        String[] columns = line.split("\t");
        HashMap<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnMap.put(StringUtils.deleteWhitespace(columns[i].toLowerCase()), i);
        }
        return columnMap;
    }

    protected HashSet<String> convertPepToSeq(HashSet<String> selectedPepSet, boolean withCharge){
        HashSet<String> selectedSeqSet = new HashSet<>();
        for (String pep: selectedPepSet){
            if (withCharge){
                selectedSeqSet.add(removeUnimod(pep.split("_")[0]));
            } else {
                selectedSeqSet.add(removeUnimod(pep));
            }
        }
        return selectedSeqSet;
    }
}
