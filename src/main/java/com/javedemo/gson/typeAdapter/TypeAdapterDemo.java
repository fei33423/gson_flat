package com.javedemo.gson.typeAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TypeAdapterDemo {

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
