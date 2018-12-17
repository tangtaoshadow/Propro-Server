package com.westlake.air.pecs.domain.db.simple;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-05 15:57
 */
@Data
public class IntensityGroup {

    String proteinName;

    String peptideRef;

    /**
     * 对应肽段的mz
     */
    Float mz;

    /**
     * 对应肽段序列,如果是伪肽段,则为对应的伪肽段的序列(不包含UniModId)
     */
    String sequence;

    /**
     * key为cutInfo, value为对应的fragment的intensity值
     */
    HashMap<String, Float> intensityMap = new HashMap<>();

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof IntensityGroup) {
            IntensityGroup target = (IntensityGroup) obj;
            if (this.getPeptideRef().equals(target.getPeptideRef())) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (peptideRef).hashCode();
    }
}
