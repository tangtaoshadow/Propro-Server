package com.westlake.air.pecs.domain.bean;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import lombok.Data;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-05 15:57
 */
@Data
public class TransitionGroup {

    String proteinName;

    String peptideRef;

    List<AnalyseDataDO> dataList;

}
