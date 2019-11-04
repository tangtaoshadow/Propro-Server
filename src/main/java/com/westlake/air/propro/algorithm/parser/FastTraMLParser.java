package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.algorithm.decoy.generator.ShuffleGenerator;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.TaskService;
import com.westlake.air.propro.utils.FileUtil;
import com.westlake.air.propro.utils.PeptideUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.westlake.air.propro.utils.PeptideUtil.parseModification;

/**
 * 对于TraML文件的高速解析引擎
 */
@Component("fastTraMLParser")
public class FastTraMLParser extends BaseLibraryParser {

    @Autowired
    TaskService taskService;
    @Autowired
    ShuffleGenerator shuffleGenerator;

    private static String PeptideListBeginMarker = "<CompoundList>";
    private static String TransitionListBeginMarker = "<TransitionList>";

    private static String PeptideMarker = "<Peptide";
    private static String PeptideEndMarker = "</Peptide";
    private static String ProteinNameMarker = "<ProteinRef";
    private static String RetentionTimeMarker = "<RetentionTime>";

    private static String TransitionMarker = "<Transition";
    private static String TransitionEndMarker = "</Transition>";
    private static String PrecursorMarker = "<Precursor>";

    private static String CvParamMarker = "<cvParam";
    private static String UserParamMarker = "<userParam";
    private static String ValueMarker = "value=\"";
    private static String RefMarker = "ref=\"";

    @Override
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {
        ResultDO tranResult = new ResultDO(true);
        try {
            BufferedReader reader = prepareForReader(in, library, taskDO);
            HashMap<String, PeptideDO> peptideMap = parsePeptide(reader, library.getId());
            parseTransitionAndInsert(peptideMap, reader, taskDO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultDO.buildError(ResultCode.FILE_FORMAT_NOT_SUPPORTED);
        }
        return tranResult;
    }

    @Override
    public ResultDO selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO) {
        ResultDO tranResult = new ResultDO(true);
        try {
            //开始插入前先清空原有的数据库数据
            BufferedReader reader = prepareForReader(in, library, taskDO);

            //parse Peptides
            HashMap<String, PeptideDO> peptideMap = selectiveParsePeptide(reader, library.getId(), selectedPepSet, selectBySequence);

            parseTransitionAndInsert(peptideMap, reader, taskDO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDO.buildError(ResultCode.FILE_FORMAT_NOT_SUPPORTED);
        }
        return tranResult;
    }

    private void seekForBeginPosition(BufferedReader reader, String marker) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.contains(marker)) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param reader
     * @param libraryId
     * @return key为TraML文件中<Peptide></Peptide>中id的值,这个值和仅有PeptideSequence+Charge组成的PeptideRef有可能是不一样的
     */
    private HashMap<String, PeptideDO> parsePeptide(BufferedReader reader, String libraryId) {
        try {
            seekForBeginPosition(reader, PeptideListBeginMarker);
            HashMap<String, PeptideDO> peptideMap = new HashMap<>();
            String line = "";
            boolean isDecoy = false;
            PeptideDO peptideDO = new PeptideDO();
            String peptideId = "";
            while ((line = reader.readLine()) != null) {
                //如果遇到TransitionList标签,说明Peptide区域已经解析完毕
                if (line.contains(TransitionListBeginMarker)) {
                    break;
                }
                if (line.contains(PeptideMarker)) {
                    isDecoy = line.contains("DECOY");
                    peptideId = line.split("\"")[1];
                    continue;
                }
                //不解析Decoy,Decoy会在后续由Propro生成
                if (isDecoy) {
                    continue;
                }

                //解析带电电荷
                if (line.contains(CvParamMarker) && line.contains("charge state")) {
                    Integer charge = Integer.parseInt(line.split(ValueMarker)[1].split("\"")[0]);
                    peptideDO.setCharge(charge);
                    continue;
                }
                //解析肽段全称
                if (line.contains(UserParamMarker) && line.contains("full_peptide_name")) {
                    String fullPeptideName = line.split(ValueMarker)[1].split("\"")[0];
                    peptideDO.setFullName(fullPeptideName);
                    continue;
                }
                //解析蛋白质名称
                if (line.contains(ProteinNameMarker)) {
                    String proteinName = line.split(RefMarker)[1].split("\"")[0];
                    peptideDO.setProteinName(proteinName);
                    continue;
                }
                //解析保留时间
                if (line.contains(RetentionTimeMarker)) {
                    while ((line = reader.readLine()).contains(CvParamMarker)) {
                        if (line.contains(ValueMarker)) {
                            String rt = line.split(ValueMarker)[1].split("\"")[0];
                            peptideDO.setRt(Double.parseDouble(rt));
                            break;
                        }
                    }
                }
                //遇到结尾字符时开始进行全面解析
                if (line.contains(PeptideEndMarker)) {
                    //以下3项的赋值顺序不能乱
                    peptideDO.setLibraryId(libraryId);
                    peptideDO.setSequence(PeptideUtil.removeUnimod(peptideDO.getFullName()));
                    peptideDO.setPeptideRef(peptideDO.getSequence() + "_" + peptideDO.getCharge());
                    parseModification(peptideDO);
                    peptideMap.put(peptideId, peptideDO);

                    //恢复初始状态--Start
                    peptideDO = new PeptideDO();
                    isDecoy = false;
                    peptideId = "";
                    //恢复初始状态--End
                }
            }
            return peptideMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private HashMap<String, PeptideDO> selectiveParsePeptide(BufferedReader reader, String libraryId, HashSet<String> selectedPepSet, boolean selectBySequence) {
        try {
            boolean withCharge = new ArrayList<>(selectedPepSet).get(0).contains("_");
            if (selectBySequence) {
                selectedPepSet = convertPepToSeq(selectedPepSet, withCharge);
            }
            seekForBeginPosition(reader, PeptideListBeginMarker);
            HashMap<String, PeptideDO> peptideMap = new HashMap<>();
            String line, filePepRef = "";
            PeptideDO peptideDO = new PeptideDO();
            while ((line = reader.readLine()) != null) {
                if (line.contains(TransitionListBeginMarker)) {
                    break;
                }
                if (line.contains(PeptideMarker)) {
                    filePepRef = line.split("\"")[1];
                    if (filePepRef.startsWith("DECOY")) {
                        continue;
                    }
                    String[] pepInfo = filePepRef.split("_");
                    String peptideRef = pepInfo[1] + "_" + pepInfo[2];
                    String fullName = pepInfo[1];
                    String sequence = PeptideUtil.removeUnimod(pepInfo[1]);
                    if (selectBySequence) {
                        if (!selectedPepSet.contains(sequence)) {
                            continue;
                        }
                    } else {
                        if (withCharge && !selectedPepSet.contains(peptideRef)) {
                            continue;
                        }
                        if (!withCharge && !selectedPepSet.contains(fullName)) {
                            continue;
                        }
                    }
                    peptideDO.setPeptideRef(peptideRef);
                    peptideDO.setFullName(fullName);
                    peptideDO.setSequence(sequence);
                    peptideDO.setCharge(Integer.parseInt(pepInfo[2]));
                    peptideDO.setLibraryId(libraryId);
                    parseModification(peptideDO);
                    continue;
                }
                if (peptideDO.getPeptideRef() != null && line.contains(ProteinNameMarker)) {
                    String proteinName = line.split(RefMarker)[1].split("\"")[0];
                    peptideDO.setProteinName(proteinName);
                    continue;
                }
                if (peptideDO.getPeptideRef() != null && line.contains(RetentionTimeMarker)) {
                    while ((line = reader.readLine()).contains(CvParamMarker)) {
                        if (line.contains(ValueMarker)) {
                            String rt = line.split(ValueMarker)[1].split("\"")[0];
                            peptideDO.setRt(Double.parseDouble(rt));
                            break;
                        }
                    }
                    peptideMap.put(filePepRef, peptideDO);
                    peptideDO = new PeptideDO();
                }
            }
            return peptideMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResultDO parseTransitions(BufferedReader reader, HashMap<String, PeptideDO> peptideMap) {
        try {
            PeptideDO peptideDO = null;
            FragmentInfo fi = new FragmentInfo();
            String line, filePepRef;
            String ionSeries = "";
            String fragType = "";
            while ((line = reader.readLine()) != null) {
                if (peptideDO == null && line.contains(TransitionMarker)) {
                    filePepRef = line.split("\"")[3];
                    if (filePepRef.startsWith("DECOY")) {
                        continue;
                    }
                    //如果PepRef仅包含一个下划线,那么这个PeptideRef是一个单纯的PeptideRef,否则只取最后一段
                    String[] labels = filePepRef.split("_");
                    if (labels.length <= 1) {
                        peptideDO = peptideMap.get(filePepRef);
                    } else {
                        String peptideRef = labels[labels.length - 2] + "_" + labels[labels.length - 1];
                        peptideDO = peptideMap.get(peptideRef);
                    }
                }

                if (peptideDO != null && peptideDO.getMz() == null && line.contains(PrecursorMarker)) {
                    while ((line = reader.readLine()).contains(CvParamMarker)) {
                        if (line.contains(ValueMarker)) {
                            String mz = line.split(ValueMarker)[1].split("\"")[0];
                            peptideDO.setMz(Double.parseDouble(mz));
                            break;
                        }
                    }
                    continue;
                }

                if (peptideDO != null && line.contains(CvParamMarker)) {
                    if (line.contains("charge")) {
                        String charge = line.split(ValueMarker)[1].split("\"")[0];
                        fi.setCharge(Integer.parseInt(charge));
                        continue;
                    }
                    if (line.contains("m/z")) {
                        String mz = line.split(ValueMarker)[1].split("\"")[0];
                        fi.setMz(Double.parseDouble(mz));
                        continue;
                    }
                    if (line.contains("intensity")) {
                        String intensity = line.split(ValueMarker)[1].split("\"")[0];
                        fi.setIntensity(Double.parseDouble(intensity));
                        continue;
                    }
                    //直接使用<Product>-><InterpretationList>-><Interpretation>标签进行Annotation的解析,如果后面检测到有annotation标签,那么直接使用annotation标签的解析结果进行覆盖
                    if (line.contains("product ion series ordinal")) {
                        ionSeries = line.split(ValueMarker)[1].split("\"")[0];
                        continue;
                    }
                    if (line.contains("frag: y ion")) {
                        fragType = "y";
                        continue;
                    }
                    if (line.contains("frag: b ion")) {
                        fragType = "b";
                        continue;
                    }
                }

                //本逻辑条件需要放在最后面
                if (peptideDO != null && line.contains("annotation")) {
                    String annotations = line.split(ValueMarker)[1].split("\"")[0];
                    fi.setAnnotations(annotations);
                    PeptideUtil.parseAnnotations(fi, annotations);
                }

                //如果检测到结束符
                if (peptideDO != null && line.contains(TransitionEndMarker)) {
                    if (fi.getAnnotation() == null && !fragType.isEmpty() && !ionSeries.isEmpty()) {
                        PeptideUtil.parseAnnotations(fi, fragType + ionSeries + "/" + "0.000");
                    }
                    peptideDO.putFragment(fi.getCutInfo(), fi);
                    fi = new FragmentInfo();
                    peptideDO = null;
                    continue;
                }

            }
            return new ResultDO(true);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResultDO(false);
        }
    }

    private void parseTransitionAndInsert(HashMap<String, PeptideDO> peptideMap, BufferedReader reader, TaskDO taskDO) throws Exception {
        if (peptideMap == null || peptideMap.isEmpty()) {
            throw new Exception();
        }

        //parse Transitions
        ResultDO resultDO = parseTransitions(reader, peptideMap);
        if (resultDO.isFailed()) {
            throw new Exception();
        }

        for (PeptideDO peptide : peptideMap.values()) {
            shuffleGenerator.generate(peptide);
        }

        peptideService.insertAll(new ArrayList<>(peptideMap.values()), false);
        taskDO.addLog(peptideMap.size() + "条肽段数据插入成功");
        taskService.update(taskDO);
        logger.info(peptideMap.size() + "条肽段数据插入成功");
    }
}
