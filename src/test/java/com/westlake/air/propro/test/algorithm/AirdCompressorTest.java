package com.westlake.air.propro.test.algorithm;

import com.westlake.air.propro.parser.MzXMLParser;
import com.westlake.air.propro.test.BaseTest;
import com.westlake.air.propro.utils.CompressUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AirdCompressorTest extends BaseTest {

    @Autowired
    MzXMLParser mzXMLParser;

    @Test
    public void testSortedIntegerCompress(){

        int[] testArray = new int[10];
        for(int i=0;i<testArray.length;i++){
            testArray[i] = i*3;
        }

        testArray = CompressUtil.compressForSortedInt(testArray);
        testArray = CompressUtil.decompressForSortedInt(testArray);
        System.out.println(testArray.length);
        for(int j =0;j<testArray.length;j++){
            assert testArray[j] == j*3;
        }
    }
}