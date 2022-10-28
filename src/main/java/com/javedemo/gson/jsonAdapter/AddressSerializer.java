package com.javedemo.gson.jsonAdapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.javedemo.gson.typeAdapter.Address;

import java.lang.reflect.Type;

public class AddressSerializer implements JsonSerializer<Address> {
    @Override
    public JsonElement serialize(Address src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonMerchant = new JsonObject();

        jsonMerchant.addProperty("addressCity" , src.getCity());

        return jsonMerchant;
    }
}
