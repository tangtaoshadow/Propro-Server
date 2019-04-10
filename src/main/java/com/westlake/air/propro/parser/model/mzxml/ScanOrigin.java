package com.westlake.air.propro.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-06 09:18
 */
@Data
public class ScanOrigin {

    @XStreamAsAttribute
    String parentFileID;

    @XStreamAsAttribute
    Long num;
}