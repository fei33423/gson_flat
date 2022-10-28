package com.javedemo.gson.jsonAdapter;

import com.google.gson.Gson;
import com.javedemo.gson.typeAdapter.Address;
import com.javedemo.gson.typeAdapter.FlattenUserSubscription;

/**
 *
 *
 * 正确演示 java - GSON flat down map to other fields - Stack Overflow 自定义注解 @flat
 *https://stackoverflow.com/questions/70411051/gson-flat-down-map-to-other-fields
 *
 * 错误演示 java - Make a "flat" JSON using Gson() - Stack Overflow
 * https://www.zadmei.com/gtgjzdyf.html
 *
 * 正确演示 不如 Jackson 的自定义注解java - How to serialize a List content to a flat JSON object with Jackson? - Stack Overflow
 *
 */
public class JsonAdapterDemo {



    public static void main(String[] args) {
        // JsonAdapterUserSubscription 类中使用了 JsonAdapter(AddressSerializer.class)
        FlattenUserSubscription subscription=new FlattenUserSubscription();
        Address m=new Address();
        m.setCity("yiwu");
        subscription.setAddress(m);
        subscription.setName("loufei");
        Gson gson = new Gson();
        String fullJSON = gson.toJson(subscription);
        System.out.println(fullJSON);
    }
}
