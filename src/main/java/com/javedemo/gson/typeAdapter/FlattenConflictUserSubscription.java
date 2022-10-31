package com.javedemo.gson.typeAdapter;

public class FlattenConflictUserSubscription {
    String name;
    String city;

    @Flatten
    Address address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }


}
