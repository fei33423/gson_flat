package com.javedemo.gson.typeAdapter.reverseflat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Helper {
    public static Gson createFlatteningGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(new ReverseFlattenTypeAdapterFactory())
                .create();
    }

    public static Gson createDefaultGson() {
        return new Gson();
    }
}