package com.westlake.air.propro.algorithm.parser.model.mzxml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;

@Data
public class NameValue {

    @XStreamAsAttribute
    String name;

    @XStreamAsAttribute
    String value;
}
