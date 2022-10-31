package com.javedemo.gson.parentJson;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Container {
    private ParentClazz parent;;

    public void setParent(ParentClazz parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
