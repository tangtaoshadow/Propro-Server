package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import org.springframework.stereotype.Component;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 16-28
 */
@Component("gaussFilter")
public class GaussFilter {

    /**
     * @param pairs
     * @param sigmaSpacing
     * @return
     */
    public RtIntensityPairsDouble filter(RtIntensityPairsDouble pairs, SigmaSpacing sigmaSpacing) {

        Double spacing = sigmaSpacing.getSpacingDouble();
        //coeffs: 以0为中心，sigma为标准差的正态分布参数
        double[] coeffs = sigmaSpacing.getCoeffs();

        //startPosition & endPosition
        int middle = sigmaSpacing.getRightNum();
        int listSize = pairs.getRtArray().length;
        double startPosition, endPosition;
        double minRt = pairs.getRtArray()[0];
        double maxRt = pairs.getRtArray()[listSize - 1];

        //begin integrate
        RtIntensityPairsDouble pairsFiltered = new RtIntensityPairsDouble(pairs);
        Double[] rtArray = pairsFiltered.getRtArray();
        Double[] intArray = pairsFiltered.getIntensityArray();
        Double[] newIntArray = new Double[intArray.length];
        Float distanceInGaussianFloat;
        Double distanceInGaussian;
        int leftPosition;
        int rightPosition;
        double residualPercent;
        double coeffRight;
        double coeffLeft;
        double norm = 0;
        double v = 0;

        for (int i = 0; i < listSize; i++) {

            norm = 0;
            v = 0;
            //startPosition
            if ((rtArray[i] - middle * spacing) > minRt) {
                startPosition = Math.round((rtArray[i] - middle * spacing) * Constants.MATH_ROUND_PRECISION) / Constants.MATH_ROUND_PRECISION;
            } else {
                startPosition = minRt;
            }

            //endPostion
            if ((rtArray[i] + middle * spacing) < maxRt) {
                endPosition = Math.round((rtArray[i] + middle * spacing) * Constants.MATH_ROUND_PRECISION) / Constants.MATH_ROUND_PRECISION;
            } else {
                endPosition = maxRt;
            }

            //help index
            int j = i;

            // left side of i
            while (j > 0 && rtArray[j-1] > startPosition) {

//                distanceInGaussian = Math.abs(rtArray[i] - rtArray[j]);
                distanceInGaussian = Math.round((rtArray[i] - rtArray[j]) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION;
                leftPosition = (int)(Math.round((distanceInGaussian / spacing) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffRight = coeffs[leftPosition];
                }


                distanceInGaussian = Math.round((rtArray[i] - rtArray[j-1]) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION;
                leftPosition = (int)(Math.round((distanceInGaussian / spacing) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing - distanceInGaussian)) / spacing;
                if (rightPosition < middle) {
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffLeft = coeffs[leftPosition];
                }

                norm += Math.abs(rtArray[j-1] - rtArray[j]) * (coeffRight + coeffLeft) / 2.0;
                v += Math.abs(rtArray[j-1] - rtArray[j]) * (intArray[j-1] * coeffLeft + intArray[j] * coeffRight) / 2.0;

                j--;

            }

            j = i;

            // right side of i
            while (j < listSize - 1 && rtArray[j + 1] < endPosition) {
//                distanceInGaussianFloat = (float)(Math.round(Math.abs(rtArray[i]  - rtArray[j]) * 10000)) / 10000;
//                distanceInGaussian = Double.parseDouble(distanceInGaussianFloat.toString());
                distanceInGaussian = Math.round((rtArray[j] - rtArray[i]) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION;
                leftPosition = (int)(Math.round((distanceInGaussian / spacing) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffLeft = coeffs[leftPosition];
                }

                //(float)(Math.round(a*100))/100
//                distanceInGaussianFloat = (float)(Math.round(Math.abs(rtArray[i]  - rtArray[j+1]) * 10000)) / 10000;
//                distanceInGaussian = Double.parseDouble(distanceInGaussianFloat.toString());
                distanceInGaussian = Math.round((rtArray[j+1] - rtArray[i]) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION;
                leftPosition = (int)(Math.round((distanceInGaussian / spacing) * Constants.MATH_ROUND_PRECISION)/Constants.MATH_ROUND_PRECISION);
                rightPosition = leftPosition + 1;

                residualPercent = (Math.abs(leftPosition * spacing - distanceInGaussian)) / spacing;
                if (rightPosition < middle) {
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffRight = coeffs[leftPosition];
                }

                norm += Math.abs(rtArray[j+1] - rtArray[j]) * (coeffLeft + coeffRight) / 2.0;
                v += Math.abs(rtArray[j+1] - rtArray[j] ) * (intArray[j] * coeffLeft + intArray[j+1] * coeffRight) / 2.0;

                j++;

            }

            if (v > 0) {
                newIntArray[i] = v / norm;
            } else {
                newIntArray[i] = 0d;
            }


        }
        pairsFiltered.setIntensityArray(newIntArray);
        return pairsFiltered;
    }
}
