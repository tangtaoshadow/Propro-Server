package com.westlake.air.swathplatform.service;

import com.mongodb.BasicDBObject;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.TargetTransition;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.domain.query.TransitionQuery;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:56
 */
public interface TransitionService {

    List<TransitionDO> getAllByLibraryId(String libraryId);

//    List<TransitionDO> getSimpleAllByLibraryId(String libraryId);

    List<TransitionDO> getAllByLibraryIdAndIsDecoy(String libraryId,boolean isDecoy);

    Long count(TransitionQuery query);

    ResultDO<List<TransitionDO>> getList(TransitionQuery transitionQuery);

    ResultDO insert(TransitionDO transitionDO);

    ResultDO insertAll(List<TransitionDO> transitions, boolean isDeleteOld);

    ResultDO deleteAllByLibraryId(String libraryId);

    ResultDO deleteAllDecoyByLibraryId(String libraryId);

    ResultDO<TransitionDO> getById(String id);

    Long countByProteinName(String libraryId);

    Long countByPeptideSequence(String libraryId);

    HashMap<Integer,List<TargetTransition>> buildMS(String libraryId, double extraction_windows);

}
