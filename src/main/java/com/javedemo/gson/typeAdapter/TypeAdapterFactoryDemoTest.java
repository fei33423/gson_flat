package com.javedemo.gson.typeAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TypeAdapterFactoryDemoTest {

    /**
     * 使用了FlatteningTypeAdapterFactory,
     * 遇到一个类时,动态生成adapter.进行自定义的序列化和反序列化. 不会像 json 再委托给上下文.
     * @param args
     */
    public static void main(String[] args) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .disableInnerClassSerialization()
                .registerTypeAdapterFactory(FlatteningTypeAdapterFactory.getInstance())
                .create();

        FlattenUserSubscription subscription = new FlattenUserSubscription();
        Address m = new Address();
        m.setCity("yiwu");
        m.setAddress("拱墅区3号");
        subscription.setAddress(m);
        subscription.setName("loufei");

        String fullJSON = gson.toJson(subscription);
        System.out.println(fullJSON);
        FlattenUserSubscription flattenUserSubscription = gson.fromJson(fullJSON, FlattenUserSubscription.class);
        System.out.println(flattenUserSubscription);
        FlattenConflictUserSubscription conflictUserSubscription = new FlattenConflictUserSubscription();
          m = new Address();
        m.setCity("yiwu");
        m.setAddress("拱墅区3号");
        subscription.setAddress(m);
        subscription.setName("loufei");

          fullJSON = gson.toJson(conflictUserSubscription);
        System.out.println(fullJSON);
        FlattenConflictUserSubscription conflictUserSubscription1 = gson.fromJson(fullJSON, FlattenConflictUserSubscription.class);
        System.out.println(conflictUserSubscription1);

    }
}
