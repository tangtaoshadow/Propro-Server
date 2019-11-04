package com.westlake.air.propro.utils;

import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.PeptideDO;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nico Wang
 * Time: 2019-07-04 16:24
 */
public class PeptideUtil {


    public static final Pattern unimodPattern = Pattern.compile("([a-z])[\\(]unimod[\\:](\\d*)[\\)]");

    public static String removeUnimod(String fullName){
        if (fullName.contains("(")){
            String[] parts = fullName.replaceAll("\\(","|(").replaceAll("\\)","|").split("\\|");
            String sequence = "";
            for(String part: parts){
                if (part.startsWith("(")){
                    continue;
                }
                sequence += part;
            }
            return sequence;
        }else {
            return fullName;
        }
    }


    /**
     * 解析出Modification的位置
     *
     * @param peptideDO
     */
    public static void parseModification(PeptideDO peptideDO) {
        //不论是真肽段还是伪肽段,fullUniModPeptideName字段都是真肽段的完整版
        String peptide = peptideDO.getFullName();
        peptide = peptide.toLowerCase();
        HashMap<Integer, String> unimodMap = new HashMap<>();

        while (peptide.contains("(unimod:") && peptide.indexOf("(unimod:") != 0) {
            Matcher matcher = unimodPattern.matcher(peptide);
            if (matcher.find()) {
                unimodMap.put(matcher.start(), matcher.group(2));
                peptide = StringUtils.replaceOnce(peptide, matcher.group(0), matcher.group(1));
            }
        }
        peptideDO.setUnimodMap(unimodMap);
    }

    /**
     * 解析出Modification的位置
     *
     * @param fullName
     */
    public static HashMap<Integer, String> parseModification(String fullName) {
        //不论是真肽段还是伪肽段,fullUniModPeptideName字段都是真肽段的完整版

        fullName = fullName.toLowerCase();
        HashMap<Integer, String> unimodMap = new HashMap<>();

        while (fullName.contains("(unimod:") && fullName.indexOf("(unimod:") != 0) {
            Matcher matcher = unimodPattern.matcher(fullName);
            if (matcher.find()) {
                unimodMap.put(matcher.start(), matcher.group(2));
                fullName = StringUtils.replaceOnce(fullName, matcher.group(0), matcher.group(1));
            }
        }
        return unimodMap;
    }

    public static Annotation parseAnnotation(String annotations) {
        String[] annotationStrs = annotations.split(",");
        Annotation annotation = new Annotation();

        try {
            String annotationStr = annotationStrs[0];
            if (StringUtils.startsWith(annotationStr, "[")) {
                annotation.setIsBrotherIcon(true);
                annotationStr = annotationStr.replace("[", "");
                annotationStr = annotationStr.replace("]", "");
            }
            String[] forDeviation = annotationStr.split("/");
            if (forDeviation.length > 1) {
                annotation.setDeviation(Double.parseDouble(forDeviation[1]));
            }

            if (forDeviation[0].endsWith("i")) {
                annotation.setIsotope(true);
                forDeviation[0] = forDeviation[0].replace("i", "");
            }

            String[] forCharge = forDeviation[0].split("\\^");
            if (forCharge.length == 2) {
                annotation.setCharge(Integer.parseInt(forCharge[1]));
            } else if (forDeviation[0].contains("(")) {
                String[] msmsCutoff = forDeviation[0].split("\\(");
                annotation.setCharge(Integer.parseInt(msmsCutoff[1].substring(0, 1)));
                forCharge[0] = msmsCutoff[0];
            }
            //默认为负,少数情况下校准值为正
            String nOrP = "-";
            String[] forAdjust;
            if (forCharge[0].contains("+")) {
                nOrP = "+";
                forAdjust = forCharge[0].split("\\+");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            } else if (forCharge[0].contains("-")) {
                forAdjust = forCharge[0].split("-");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            } else {
                forAdjust = forCharge;
            }

            String finalStr = forAdjust[0];
            //第一位必定是字母,代表fragment类型
            annotation.setType(finalStr.substring(0, 1));
            String location = finalStr.substring(1);
            if (!location.isEmpty()) {
                annotation.setLocation(Integer.parseInt(location));
            }
        } catch (Exception e) {
            e.printStackTrace();
           return null;
        }
        return annotation;
    }

    //解析离子类型以及断裂位置,不包含离子碎片的带电量
    public static void parseAnnotations(FragmentInfo fi, String annotations){
        fi.setAnnotations(annotations);
        Annotation annotation = parseAnnotation(annotations);
        if(annotation != null){
            fi.setAnnotation(annotation);
            if (fi.getCharge() == null){
                fi.setCharge(annotation.getCharge());
            }
            fi.setCutInfo(annotation.toCutInfo());
        }
    }
}
