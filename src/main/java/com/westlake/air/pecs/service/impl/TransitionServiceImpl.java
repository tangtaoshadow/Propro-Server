package com.westlake.air.pecs.service.impl;

import com.google.common.collect.Ordering;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.TransitionDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.LibraryCoordinate;
import com.westlake.air.pecs.domain.bean.TargetTransition;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 20:02
 */
@Service("transitionService")
public class TransitionServiceImpl implements TransitionService {

    public final Logger logger = LoggerFactory.getLogger(TransitionServiceImpl.class);

    @Autowired
    TransitionDAO transitionDAO;

    @Override
    public List<TransitionDO> getAllByLibraryId(String libraryId) {
        return transitionDAO.getAllByLibraryId(libraryId);
    }

    @Override
    public List<TransitionDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy) {
        return transitionDAO.getAllByLibraryIdAndIsDecoy(libraryId, isDecoy);
    }

    @Override
    public Long count(TransitionQuery query) {
        return transitionDAO.count(query);
    }

    @Override
    public ResultDO<List<TransitionDO>> getList(TransitionQuery query) {

        List<TransitionDO> transitionDOList = transitionDAO.getList(query);
        long totalCount = transitionDAO.count(query);
        ResultDO<List<TransitionDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(transitionDOList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public ResultDO insert(TransitionDO transitionDO) {
        try {
            transitionDAO.insert(transitionDO);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    /**
     * 这边的代码由于时间问题写的比较简陋,先删除原有的关联数据,再插入新的关联数据,未做事务处理
     *
     * @param transitions
     * @param isDeleteOld
     * @return
     */
    @Override
    public ResultDO insertAll(List<TransitionDO> transitions, boolean isDeleteOld) {
        if (transitions == null || transitions.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            if (isDeleteOld) {
                transitionDAO.deleteAllByLibraryId(transitions.get(0).getLibraryId());
            }
            transitionDAO.insert(transitions);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllByLibraryId(String libraryId) {
        try {
            transitionDAO.deleteAllByLibraryId(libraryId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllDecoyByLibraryId(String libraryId) {
        try {
            transitionDAO.deleteAllDecoyByLibraryId(libraryId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<TransitionDO> getById(String id) {
        try {
            TransitionDO transitionDO = transitionDAO.getById(id);
            if (transitionDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<TransitionDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(transitionDO);
                return resultDO;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public Long countByProteinName(String libraryId) {
        return transitionDAO.countByProteinName(libraryId);
    }

    @Override
    public Long countByPeptideSequence(String libraryId) {
        return transitionDAO.countByFullName(libraryId);
    }

    @Override
    public LibraryCoordinate buildMS(String libraryId, float rtExtractionWindows) {
        logger.info("构建卷积MS1&MS2坐标(耗时操作)");
        long start = System.currentTimeMillis();
        List<TargetTransition> targetList = transitionDAO.getTargetTransitionsByLibraryId(libraryId);

        for (TargetTransition targetTransition : targetList) {
            targetTransition.setRtStart(targetTransition.getRt() - rtExtractionWindows / 2.0f);
            targetTransition.setRtEnd(targetTransition.getRt() + rtExtractionWindows / 2.0f);
        }
        LibraryCoordinate lc = new LibraryCoordinate();
        lc.setLibraryId(libraryId);
        lc.setRtExtractionWindow(rtExtractionWindows);
        lc.setMs2List(buildMS2(targetList));
        lc.setMs1List(buildMS1(targetList));
        logger.info("构建卷积坐标耗时:" + (System.currentTimeMillis() - start));
        return lc;
    }

    @Override
    public List<TargetTransition> buildMS1(String libraryId, float rtExtractionWindows) {
        logger.info("构建卷积MS1坐标(耗时操作)");
        long start = System.currentTimeMillis();
        List<TargetTransition> targetList = transitionDAO.getTargetTransitionsByLibraryId(libraryId);
        logger.info("读取数据库耗时:" + (System.currentTimeMillis() - start));

        for (TargetTransition targetTransition : targetList) {
            targetTransition.setRtStart(targetTransition.getRt() - rtExtractionWindows / 2.0f);
            targetTransition.setRtEnd(targetTransition.getRt() + rtExtractionWindows / 2.0f);
        }
        List<TargetTransition> list = buildMS1(targetList);
        logger.info("构建卷积坐标耗时:" + (System.currentTimeMillis() - start));
        return list;
    }

    @Override
    public List<TargetTransition> buildMS2(String libraryId, float rtExtractionWindows) {
        logger.info("构建卷积MS1坐标(耗时操作)");
        long start = System.currentTimeMillis();
        List<TargetTransition> targetList = transitionDAO.getTargetTransitionsByLibraryId(libraryId);
        logger.info("读取数据库耗时:" + (System.currentTimeMillis() - start));

        for (TargetTransition targetTransition : targetList) {
            targetTransition.setRtStart(targetTransition.getRt() - rtExtractionWindows / 2.0f);
            targetTransition.setRtEnd(targetTransition.getRt() + rtExtractionWindows / 2.0f);
        }
        List<TargetTransition> list = buildMS2(targetList);
        logger.info("构建卷积坐标耗时:" + (System.currentTimeMillis() - start));
        return list;
    }


    private List<TargetTransition> buildMS1(List<TargetTransition> targetList) {
        //存储set中从而过滤出MS1
        HashSet<TargetTransition> targetSet = new HashSet<>(targetList);
        Ordering<TargetTransition> ordering2 = Ordering.from(new Comparator<TargetTransition>() {
            @Override
            public int compare(TargetTransition o1, TargetTransition o2) {
                if (o1.getPrecursorMz() > o2.getPrecursorMz()) {
                    return 1;
                } else if (o1.getPrecursorMz() == o2.getPrecursorMz()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        return ordering2.sortedCopy(targetSet);
    }

    private List<TargetTransition> buildMS2(List<TargetTransition> targetList) {
        Ordering<TargetTransition> ordering = Ordering.from(new Comparator<TargetTransition>() {
            @Override
            public int compare(TargetTransition o1, TargetTransition o2) {
                if (o1.getProductMz() > o2.getProductMz()) {
                    return 1;
                } else if (o1.getProductMz() == o2.getProductMz()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        return ordering.sortedCopy(targetList);
    }


}