package com.westlake.air.propro.algorithm.learner;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import com.westlake.air.propro.constants.Classifier;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.airus.*;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.utils.*;
import ml.dmlc.xgboost4j.java.Booster;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:25
 */
@Component
public class Airus {

    public final Logger logger = LoggerFactory.getLogger(Airus.class);

    @Autowired
    SemiSupervised semiSupervised;
    @Autowired
    LDALearner ldaLearner;
    @Autowired
    Stats stats;
    @Autowired
    ScoreService scoreService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    XGBoostLearner xgBoostLearner;

    public FinalResult doAirus(String overviewId, AirusParams airusParams) {
        String type = "";
        FinalResult finalResult = new FinalResult();
        logger.info("开始清理已识别的肽段数目");
        ResultDO<AnalyseOverviewDO> overviewDOResultDO = analyseOverviewService.getById(overviewId);
        if (overviewDOResultDO.isFailed()) {
            finalResult.setErrorInfo(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return finalResult;
        }
        AnalyseOverviewDO overviewDO = overviewDOResultDO.getModel();
        overviewDO.setClassifier(airusParams.getClassifier().name());
        if (overviewDO.getMatchedPeptideCount() != null) {
            overviewDO.setMatchedPeptideCount(null);
        }
        analyseOverviewService.update(overviewDO);
        type = overviewDO.getType();
        logger.info("开始获取打分数据");

        if (airusParams.getScoreTypes()==null){
            airusParams.setScoreTypes(overviewDO.getScoreTypes());
        }
        List<SimpleScores> scores = analyseDataService.getSimpleScoresByOverviewId(overviewId);
        ResultDO resultDO = checkData(scores);
        if (resultDO.isFailed()) {
            finalResult.setErrorInfo(resultDO.getMsgInfo());
            return finalResult;
        }
        if (type.equals(Constants.EXP_TYPE_PRM)) {
            cleanScore(scores, overviewDO.getScoreTypes());
        }
        HashMap<String, Double> weightsMap = new HashMap<>();
        HashMap<String, Integer> peptideHitMap = new HashMap<>();
        if (airusParams.getClassifier().equals(Classifier.lda)) {
            logger.info("开始训练学习数据权重");
            weightsMap = LDALearn(scores, peptideHitMap, airusParams, overviewDO.getScoreTypes(), type);
            logger.info("开始计算合并打分");
            ldaLearner.score(scores, weightsMap, airusParams.getScoreTypes());
            finalResult.setWeightsMap(weightsMap);
        } else if (airusParams.getClassifier().equals(Classifier.xgboost)) {
            XGBLearn(scores, overviewDO.getScoreTypes(), airusParams);
        }
        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), overviewDO.getScoreTypes(), false);
        int hit = 0, count = 0;
        if (type.equals(Constants.EXP_TYPE_PRM)) {
            double maxDecoy = Double.MIN_VALUE;
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
                    maxDecoy = simpleFeatureScores.getMainScore();
                }
            }
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy && simpleFeatureScores.getThresholdPassed()) {
                    simpleFeatureScores.setFdr(0d);
                    count++;
                } else {
                    simpleFeatureScores.setFdr(1d);
                }
            }

        } else {
            ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
            finalResult.setAllInfo(errorStat);
            count = AirusUtil.checkFdr(finalResult);
        }
        //对于最终的打分结果和选峰结果保存到数据库中
        logger.info("将合并打分及定量结果反馈更新到数据库中,总计:" + featureScoresList.size() + "条数据");
        giveDecoyFdr(featureScoresList);
        MongoClient mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress("localhost", 27017))))
                        .build());
        MongoDatabase database = mongoClient.getDatabase("propro");
        MongoCollection<Document> collection = database.getCollection("analyseData");
        List<UpdateOneModel<Document>> documentList = new ArrayList<>();
        Long getTime = 0L, updateTime = 0L, insertTime = 0L,startTime = 0L;
        for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
            //Long startTime = System.currentTimeMillis();
            Document updateCon = new Document();
            Document searchQuery = new Document();
            Document updateQuery = new Document("$set",updateCon);
            //AnalyseDataDO dataDO = analyseDataService.getByOverviewIdAndPeptideRefAndIsDecoy(overviewId, simpleFeatureScores.getPeptideRef(), simpleFeatureScores.getIsDecoy());
            getTime += System.currentTimeMillis() - startTime;
//            dataDO.setBestRt(simpleFeatureScores.getRt());
//            dataDO.setIntensitySum(simpleFeatureScores.getIntensitySum());
//            dataDO.setFragIntFeature(simpleFeatureScores.getFragIntFeature());
//            dataDO.setFdr(simpleFeatureScores.getFdr());
//            dataDO.setQValue(simpleFeatureScores.getQValue());
            searchQuery.append("overviewId", overviewId);
            searchQuery.append("peptideRef",simpleFeatureScores.getPeptideRef());
            searchQuery.append("isDeoy",simpleFeatureScores.getIsDecoy());
            updateCon.append("bestRT",simpleFeatureScores.getRt());
            updateCon.append("intensitySum",simpleFeatureScores.getIntensitySum());
            updateCon.append("fragIntFeature",simpleFeatureScores.getFragIntFeature());
            updateCon.append("fdr",simpleFeatureScores.getFdr());
            updateCon.append("qValue",simpleFeatureScores.getQValue());
            if (!simpleFeatureScores.getIsDecoy()) {
                //投票策略
                if (simpleFeatureScores.getFdr() <= 0.01) {
                    Integer hitCount = peptideHitMap.get(simpleFeatureScores.getPeptideRef());
                    if (hitCount != null && hitCount >= airusParams.getTrainTimes() / 2) {
                        hit++;
                    }
                    //dataDO.setIdentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
                    updateCon.append("identifiedStatus",AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
                } else {
                    //dataDO.setIdentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
                    updateCon.append("identifiedStatus",AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
                }
            }
            documentList.add(new UpdateOneModel<>(searchQuery,updateQuery));
            //startTime = System.currentTimeMillis();
            //ResultDO r = analyseDataService.update(dataDO);
            //dataDO_list.add(dataDO);
            //updateTime += System.currentTimeMillis() - startTime;
//            if (r.isFailed()) {
//                logger.error(r.getMsgInfo());
//            }
        }
        startTime = System.currentTimeMillis();
        collection.bulkWrite(documentList, new BulkWriteOptions().ordered(false));
        insertTime += System.currentTimeMillis() - startTime;
        logger.info("插入数据库一共用时："+insertTime);
        logger.info("采用加权法获得的肽段数目为:" + hit);
        logger.info("打分反馈更新完毕");
        finalResult.setMatchedPeptideCount(count);
        overviewDO.setWeights(weightsMap);
        overviewDO.setMatchedPeptideCount(count);
        analyseOverviewService.update(overviewDO);
        
        logger.info("合并打分完成,共找到新肽段" + count + "个");
        return finalResult;
    }

    private ResultDO checkData(List<SimpleScores> scores) {
        boolean isAllDecoy = true;
        boolean isAllReal = true;
        for (SimpleScores score : scores) {
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

    /**
     *
     * @param scores
     * @param peptideHitMap
     * @param airusParams
     * @param type 实验类型,PRM还是其他
     * @return
     */
    public HashMap<String, Double> LDALearn(List<SimpleScores> scores, HashMap<String, Integer> peptideHitMap, AirusParams airusParams, List<String> scoreTypes, String type) {
        // adjust AirusParams iter times, too much iterations result to deviation
        if(scores.size() < 500){
            airusParams.setXevalNumIter(10);
        }
        int neval = airusParams.getTrainTimes();
//        test(scores);
        List<HashMap<String, Double>> weightsMapList = new ArrayList<>();
        for (int i = 0; i < neval; i++) {
            logger.info("开始第" + i + "轮尝试,总计"+neval+"轮");
            LDALearnData ldaLearnData = semiSupervised.learnRandomized(scores, airusParams);
            if (ldaLearnData == null) {
                logger.info("跳过本轮训练");
                continue;
            }
            ldaLearner.score(scores, ldaLearnData.getWeightsMap(), scoreTypes);
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), scoreTypes, false);
            int count = 0;
            if (type.equals(Constants.EXP_TYPE_PRM)) {
                double maxDecoy = Double.MIN_VALUE;
                for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                    if (simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
                        maxDecoy = simpleFeatureScores.getMainScore();
                    }
                }
                for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                    if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy && simpleFeatureScores.getThresholdPassed()) {
                        count++;
                        simpleFeatureScores.setFdr(0d);
                    } else {
                        simpleFeatureScores.setFdr(1d);
                    }
                }
            } else {
                ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
                count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
            }
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getFdr() <= 0.01) {
                    Integer hitCount = peptideHitMap.get(simpleFeatureScores.getPeptideRef());
                    if (hitCount == null) {
                        hitCount = 0;
                    }
                    hitCount++;
                    peptideHitMap.put(simpleFeatureScores.getPeptideRef(), hitCount);
                }
            }

            if (count > 0) {
                logger.info("本轮尝试有效果:检测结果:" + count + "个");
            }
            weightsMapList.add(ldaLearnData.getWeightsMap());
            if (airusParams.isDebug()) {
                break;
            }
        }

        return AirusUtil.averagedWeights(weightsMapList);
    }

    public void XGBLearn(List<SimpleScores> scores,List<String> scoreTypes, AirusParams airusParams) {
        logger.info("开始训练Booster");
        Booster booster = semiSupervised.learnRandomizedXGB(scores, airusParams);
        try {
            logger.info("开始最终打分");
            xgBoostLearner.predictAll(booster, scores, ScoreType.MainScore.getTypeName(), airusParams.getScoreTypes());
        } catch (Exception e) {
            logger.error("XGBooster Predict All Fail.\n");
            e.printStackTrace();
        }
        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), scoreTypes, false);
        ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
        int count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
        if (count > 0) {
            logger.info("XGBooster:检测结果:" + count + "个.");
        }
    }

    private ErrorStat finalErrorTable(ErrorStat errorStat, AirusParams airusParams) {

        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] cutOffs = errorStat.getCutoff();
        Double[] cutOffsSort = cutOffs.clone();
        Arrays.sort(cutOffsSort);
        double minCutOff = cutOffsSort[0];
        double maxCutOff = cutOffsSort[cutOffs.length - 1];
        double margin = (maxCutOff - minCutOff) * 0.05;
        Double[] sampledCutoffs = MathUtil.linspace(minCutOff - margin, maxCutOff - margin, airusParams.getNumCutOffs());
        Integer[] ix = MathUtil.findNearestMatches(cutOffs, sampledCutoffs, airusParams.getUseSortOrders());

        ErrorStat sampleErrorStat = new ErrorStat();
        sampleErrorStat.setCutoff(sampledCutoffs);
        sampleErrorStat.setQvalue(ArrayUtil.extractRow(errorStat.getQvalue(), ix));
        sampleErrorStat.setPvalue(ArrayUtil.extractRow(errorStat.getPvalue(), ix));

        StatMetrics sampleStatMatric = new StatMetrics();
        sampleStatMatric.setSvalue(ArrayUtil.extractRow(statMetrics.getSvalue(), ix));
        sampleStatMatric.setFn(ArrayUtil.extractRow(statMetrics.getFn(), ix));
        sampleStatMatric.setFnr(ArrayUtil.extractRow(statMetrics.getFnr(), ix));
        sampleStatMatric.setFdr(ArrayUtil.extractRow(statMetrics.getFdr(), ix));
        sampleStatMatric.setFp(ArrayUtil.extractRow(statMetrics.getFp(), ix));
        sampleStatMatric.setFpr(ArrayUtil.extractRow(statMetrics.getFpr(), ix));
        sampleStatMatric.setTn(ArrayUtil.extractRow(statMetrics.getTn(), ix));
        sampleStatMatric.setTp(ArrayUtil.extractRow(statMetrics.getTp(), ix));

        sampleErrorStat.setStatMetrics(sampleStatMatric);

        return sampleErrorStat;
    }

    private ErrorStat summaryErrorTable(ErrorStat errorStat, AirusParams airusParams) {
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] qvalues = airusParams.getQvalues();
        Integer[] ix = MathUtil.findNearestMatches(errorStat.getQvalue(), qvalues, airusParams.getUseSortOrders());

        ErrorStat subErrorStat = new ErrorStat();
        subErrorStat.setCutoff(ArrayUtil.extractRow(errorStat.getCutoff(), ix));
        subErrorStat.setQvalue(qvalues);
        subErrorStat.setPvalue(ArrayUtil.extractRow(errorStat.getPvalue(), ix));

        StatMetrics subStatMatric = new StatMetrics();
        subStatMatric.setSvalue(ArrayUtil.extractRow(statMetrics.getSvalue(), ix));
        subStatMatric.setFn(ArrayUtil.extractRow(statMetrics.getFn(), ix));
        subStatMatric.setFnr(ArrayUtil.extractRow(statMetrics.getFnr(), ix));
        subStatMatric.setFdr(ArrayUtil.extractRow(statMetrics.getFdr(), ix));
        subStatMatric.setFp(ArrayUtil.extractRow(statMetrics.getFp(), ix));
        subStatMatric.setFpr(ArrayUtil.extractRow(statMetrics.getFpr(), ix));
        subStatMatric.setTn(ArrayUtil.extractRow(statMetrics.getTn(), ix));
        subStatMatric.setTp(ArrayUtil.extractRow(statMetrics.getTp(), ix));

        subErrorStat.setStatMetrics(subStatMatric);

        return subErrorStat;
    }

    /**
     * Dscore: Normalize clfScores with clfScores' TopDecoyPeaks.
     */
    private Double[] calculateDscore(Double[] weights, ScoreData scoreData) {
        Double[][] scores = AirusUtil.getFeatureMatrix(scoreData.getScoreData(), true);
        Double[] classifierScore = MathUtil.dot(scores, weights);
        Double[] classifierTopDecoyPeaks = AirusUtil.getTopDecoyPeaks(classifierScore, scoreData.getIsDecoy(), AirusUtil.findTopIndex(classifierScore, AirusUtil.getGroupNumId(scoreData.getGroupId())));
        return MathUtil.normalize(classifierScore, classifierTopDecoyPeaks);
    }

    private void fixMainScore(Double[][] scores) {
        for (int i = 0; i < scores.length; i++) {
            logger.info("原始分数:" + scores[i][0]);
            scores[i][0] =
                    scores[i][1] * -0.19011762 +
                            scores[i][2] * 2.47298914 +
                            scores[i][7] * 5.63906731 +
                            scores[i][11] * -0.62640133 +
                            scores[i][12] * 0.36006925 +
                            scores[i][13] * 0.08814003 +
                            scores[i][3] * 0.13978311 +
                            scores[i][5] * -1.16475032 +
                            scores[i][16] * -0.19267813 +
                            scores[i][9] * -0.61712054;
            logger.info("事后分数:" + scores[i][0]);
        }
    }

    //TODO 王瑞敏 MainScore有时候会是空的
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

    private void cleanScore(List<SimpleScores> scoresList, List<String> scoreTypes) {
        for (SimpleScores simpleScores : scoresList) {
            if (simpleScores.getIsDecoy()) {
                continue;
            }
            for (FeatureScores featureScores : simpleScores.getFeatureScoresList()) {
                int count = 0;
                if (featureScores.get(ScoreType.NormRtScore.getTypeName(), scoreTypes) > 8) {
                    count++;
                }
                if (featureScores.get(ScoreType.LogSnScore.getTypeName(), scoreTypes) < 3) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeCorrelationScore.getTypeName(), scoreTypes) < 0.8) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeOverlapScore.getTypeName(), scoreTypes) > 0.2) {
                    count++;
                }
                if (featureScores.get(ScoreType.MassdevScoreWeighted.getTypeName(), scoreTypes) > 15) {
                    count++;
                }
                if (featureScores.get(ScoreType.BseriesScore.getTypeName(), scoreTypes) < 1) {
                    count++;
                }
                if (featureScores.get(ScoreType.YseriesScore.getTypeName(), scoreTypes) < 5) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShapeWeighted.getTypeName(), scoreTypes) < 0.6) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShape.getTypeName(), scoreTypes) < 0.5) {
                    count++;
                }

                if (count > 3) {
                    featureScores.setThresholdPassed(false);
                }
            }
        }
    }
}
