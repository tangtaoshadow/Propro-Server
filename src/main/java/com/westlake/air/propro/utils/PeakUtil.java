package com.westlake.air.propro.utils;

import com.westlake.air.propro.domain.bean.math.BisectionLowHigh;

/**
 * Created by Nico Wang
 * Time: 2019-04-12 22:15
 */
public class PeakUtil {

    public static int findNearestIndex(Double[] x, double value){
        BisectionLowHigh bisectionLowHigh = MathUtil.bisection(x, value);
        if(x[bisectionLowHigh.getHigh()] - value > value - x[bisectionLowHigh.getLow()]){
            return bisectionLowHigh.getLow();
        }else {
            return bisectionLowHigh.getHigh();
        }
    }

    public static int findNearestIndex(Float[] x, float value){
        BisectionLowHigh bisectionLowHigh = MathUtil.bisection(x, value);
        if(x[bisectionLowHigh.getHigh()] - value > value - x[bisectionLowHigh.getLow()]){
            return bisectionLowHigh.getLow();
        }else {
            return bisectionLowHigh.getHigh();
        }
    }
}
