package com.westlake.air.propro.algorithm.learner;

import com.westlake.air.propro.algorithm.learner.classifier.LdaClassifier;
import com.westlake.air.propro.algorithm.learner.classifier.XgboostClassifier;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.airus.*;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.simple.PeptideScores;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.utils.AirusUtil;
import com.westlake.air.propro.utils.SortUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:25
 */
@Component
public class SemiSupervise {

    public final Logger logger = LoggerFactory.getLogger(SemiSupervise.class);

    @Autowired
    LdaClassifier ldaClassifier;
    @Autowired
    XgboostClassifier xgboostClassifier;
    @Autowired
    Statistics statistics;
    @Autowired
    ScoreService scoreService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;

    public FinalResult doSemiSupervise(String overviewId, AirusParams airusParams) {
        FinalResult finalResult = new FinalResult();

        //Step1. 数据预处理
        logger.info("数据预处理");
        ResultDO<AnalyseOverviewDO> overviewDOResultDO = analyseOverviewService.getById(overviewId);
        if (overviewDOResultDO.isFailed()) {
            finalResult.setErrorInfo(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return finalResult;
        }
        AnalyseOverviewDO overviewDO = overviewDOResultDO.getModel();
        overviewDO.setClassifier(airusParams.getClassifier().name());
        if (airusParams.getScoreTypes() == null) {
            airusParams.setScoreTypes(overviewDO.getScoreTypes());
        }

        if (overviewDO.getMatchedPeptideCount() != null) {
            overviewDO.setMatchedPeptideCount(null);
        }
        analyseOverviewService.update(overviewDO);
        airusParams.setType(overviewDO.getType());

        //Step2. 从数据库读取全部打分数据
        logger.info("开始获取打分数据");
        List<PeptideScores> scores = analyseDataService.getSimpleScoresByOverviewId(overviewId);
        ResultDO resultDO = check(scores);
        if (resultDO.isFailed()) {
            finalResult.setErrorInfo(resultDO.getMsgInfo());
            return finalResult;
        }
        if (airusParams.getType().equals(Constants.EXP_TYPE_PRM)) {
            cleanScore(scores, overviewDO.getScoreTypes());
        }

        //Step3. 开始训练数据集
        HashMap<String, Double> weightsMap = new HashMap<>();
        switch (airusParams.getClassifier()) {
            case lda:
                weightsMap = ldaClassifier.classifier(scores, airusParams, overviewDO.getScoreTypes());
                ldaClassifier.score(scores, weightsMap, airusParams.getScoreTypes());
                finalResult.setWeightsMap(weightsMap);
                break;

            case xgboost:
                xgboostClassifier.classifier(scores, overviewDO.getScoreTypes(), airusParams);
                break;

            default:
                break;
        }

        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), overviewDO.getScoreTypes(), false);
        int count = 0;
        if (airusParams.getType().equals(Constants.EXP_TYPE_PRM)) {
            double maxDecoy = Double.MIN_VALUE;
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
                    maxDecoy = simpleFeatureScores.getMainScore();
                }
            }
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
                    simpleFeatureScores.setFdr(0d);
                    count++;
                } else {
                    simpleFeatureScores.setFdr(1d);
                }
            }

        } else {
            ErrorStat errorStat = statistics.errorStatistics(featureScoresList, airusParams);
            finalResult.setAllInfo(errorStat);
            count = AirusUtil.checkFdr(finalResult, airusParams.getFdr());
        }

        //Step4. 对于最终的打分结果和选峰结果保存到数据库中
        logger.info("将合并打分及定量结果反馈更新到数据库中,总计:" + featureScoresList.size() + "条数据");
        giveDecoyFdr(featureScoresList);
        analyseDataService.removeMultiDecoy(overviewId, featureScoresList, airusParams.getFdr());
        long  start = System.currentTimeMillis();
        analyseDataService.updateMulti(overviewDO.getId(), featureScoresList);
        logger.info("更新数据" + featureScoresList.size() + "条一共用时：" + (System.currentTimeMillis() - start));

        logger.info("最终鉴定肽段数目为:" + count + ",打分反馈更新完毕");
        finalResult.setMatchedPeptideCount(count);
        overviewDO.setWeights(weightsMap);
        overviewDO.setMatchedPeptideCount(count);
        analyseOverviewService.update(overviewDO);

        logger.info("合并打分完成,共找到新肽段" + count + "个");
        return finalResult;
    }

    private ResultDO check(List<PeptideScores> scores) {
        boolean isAllDecoy = true;
        boolean isAllReal = true;
        for (PeptideScores score : scores) {
            if (score.getIsDecoy()) {
                isAllReal = false;
            } else {
                isAllDecoy = false;
            }
        }
        if (isAllDecoy) {
            return ResultDO.buildError(ResultCode.ALL_SCORE_DATA_ARE_DECOY);
        }
        if (isAllReal) {
            return ResultDO.buildError(ResultCode.ALL_SCORE_DATA_ARE_REAL);
        }
        return new ResultDO(true);
    }

    private void giveDecoyFdr(List<SimpleFeatureScores> featureScoresList) {

        List<SimpleFeatureScores> sortedAll = SortUtil.sortByMainScore(featureScoresList, false);
        SimpleFeatureScores leftFeatureScore = null;
        SimpleFeatureScores rightFeatureScore;
        List<SimpleFeatureScores> decoyPartList = new ArrayList<>();
        for (SimpleFeatureScores simpleFeatureScores : sortedAll) {
            if (simpleFeatureScores.getIsDecoy()) {
                decoyPartList.add(simpleFeatureScores);
            } else {
                rightFeatureScore = simpleFeatureScores;
                if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
                    for (SimpleFeatureScores decoy : decoyPartList) {
                        if (decoy.getMainScore() - leftFeatureScore.getMainScore() < rightFeatureScore.getMainScore() - decoy.getMainScore()) {
                            decoy.setFdr(leftFeatureScore.getFdr());
                            decoy.setQValue(leftFeatureScore.getQValue());
                        } else {
                            decoy.setFdr(rightFeatureScore.getFdr());
                            decoy.setQValue(rightFeatureScore.getQValue());
                        }
                    }
                }
                leftFeatureScore = rightFeatureScore;
                decoyPartList.clear();
            }
        }
        if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
            for (SimpleFeatureScores decoy : decoyPartList) {
                decoy.setFdr(leftFeatureScore.getFdr());
                decoy.setQValue(leftFeatureScore.getQValue());
            }
        }
    }

    private void cleanScore(List<PeptideScores> scoresList, List<String> scoreTypes) {
        for (PeptideScores peptideScores : scoresList) {
            if (peptideScores.getIsDecoy()) {
                continue;
            }
            for (FeatureScores featureScores : peptideScores.getFeatureScoresList()) {
                int count = 0;
                if (featureScores.get(ScoreType.NormRtScore, scoreTypes) != null && featureScores.get(ScoreType.NormRtScore, scoreTypes) > 8) {
                    count++;
                }
                if (featureScores.get(ScoreType.LogSnScore, scoreTypes) != null && featureScores.get(ScoreType.LogSnScore, scoreTypes) < 3) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeCorrelationScore, scoreTypes) != null && featureScores.get(ScoreType.IsotopeCorrelationScore, scoreTypes) < 0.8) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeOverlapScore, scoreTypes) != null && featureScores.get(ScoreType.IsotopeOverlapScore, scoreTypes) > 0.2) {
                    count++;
                }
                if (featureScores.get(ScoreType.MassdevScoreWeighted, scoreTypes) != null && featureScores.get(ScoreType.MassdevScoreWeighted, scoreTypes) > 15) {
                    count++;
                }
                if (featureScores.get(ScoreType.BseriesScore, scoreTypes) != null && featureScores.get(ScoreType.BseriesScore, scoreTypes) < 1) {
                    count++;
                }
                if (featureScores.get(ScoreType.YseriesScore, scoreTypes) != null && featureScores.get(ScoreType.YseriesScore, scoreTypes) < 5) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShapeWeighted, scoreTypes) != null && featureScores.get(ScoreType.XcorrShapeWeighted, scoreTypes) < 0.6) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShape, scoreTypes) != null && featureScores.get(ScoreType.XcorrShape, scoreTypes) < 0.5) {
                    count++;
                }

                if (count > 3) {
                    featureScores.setThresholdPassed(false);
                }
            }
        }
    }
}