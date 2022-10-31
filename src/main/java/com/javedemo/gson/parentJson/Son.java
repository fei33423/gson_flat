package com.javedemo.gson.parentJson;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Son extends ParentClazz{
    private String sonName;

    public void setSonName(String sonName) {
        this.sonName = sonName;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
