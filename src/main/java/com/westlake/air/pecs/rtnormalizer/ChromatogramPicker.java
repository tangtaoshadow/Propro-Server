package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import com.westlake.air.pecs.utils.MathUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 20：26
 */
@Component("chromatogramPicker")
public class ChromatogramPicker {

    public IntensityRtLeftRtRightPairs pickChromatogram(RtIntensityPairs rtIntensityPairs, RtIntensityPairs smoothedRtIntensityPairs, float[] signalToNoise, RtIntensityPairs maxPeakPairs) {
        int maxPeakSize = maxPeakPairs.getRtArray().length;
        int[][] leftRight = new int[maxPeakSize][2];
        Float[] leftRt = new Float[maxPeakSize];
        Float[] rightRt = new Float[maxPeakSize];
        int leftIndex, rightIndex;

        int closestPeakIndex;
        for (int i = 0; i < maxPeakSize; i++) {
            closestPeakIndex = findClosestPeak(smoothedRtIntensityPairs, maxPeakPairs.getRtArray()[i]);

            //to the left
            leftIndex = closestPeakIndex;
            while(leftIndex > 0 &&
                    smoothedRtIntensityPairs.getIntensityArray()[leftIndex - 1] < smoothedRtIntensityPairs.getIntensityArray()[leftIndex] &&
                    signalToNoise[leftIndex] >= Constants.SIGNAL_TO_NOISE_LIMIT){
                leftIndex--;
            }

            //to the right
            rightIndex = closestPeakIndex;
            while(rightIndex <smoothedRtIntensityPairs.getIntensityArray().length - 1 &&
                    smoothedRtIntensityPairs.getIntensityArray()[rightIndex + 1] < smoothedRtIntensityPairs.getIntensityArray()[rightIndex] &&
                    signalToNoise[rightIndex] >= Constants.SIGNAL_TO_NOISE_LIMIT){
                rightIndex++;
            }

            leftRight[i][0] = leftIndex;
            leftRight[i][1] = rightIndex;
            leftRt[i] = smoothedRtIntensityPairs.getRtArray()[leftIndex];
            rightRt[i] = smoothedRtIntensityPairs.getRtArray()[rightIndex];

        }

        Float[] intensity = integratePeaks(rtIntensityPairs, leftRight);

        return new IntensityRtLeftRtRightPairs(intensity, leftRt, rightRt);
    }

    private int findClosestPeak(RtIntensityPairs rtIntensityPairs, float rt) {

        //bisection
        int low = MathUtil.bisection(rtIntensityPairs, rt);
        int high = rtIntensityPairs.getRtArray().length - 1;


        if( Math.abs(rtIntensityPairs.getRtArray()[low] - rt) < Math.abs(rtIntensityPairs.getRtArray()[high] - rt)){
            return low;
        }else {
            return high;
        }
    }

    private Float[] integratePeaks(RtIntensityPairs rtIntensityPairs, int[][] leftRight){
        int leftIndex, rightIndex, size;
        Float[] intensity = new Float[leftRight.length];
        for(int i = 0; i< leftRight.length; i++){
            intensity[i] = 0f;
            leftIndex = leftRight[i][0];
            rightIndex = leftRight[i][1];
            for(int j=leftIndex; j<= rightIndex; j++){
                intensity[i] += rtIntensityPairs.getIntensityArray()[j];
            }
        }
        return intensity;
    }
}
