package com.javedemo.gson.typeAdapter;

public class FlattenUserSubscription {
    String name;

    // new!
//        @com.google.gson.annotations.JsonAdapter(AddressSerializer.class)
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

    @Override
    public String toString() {
        return "FlattenUserSubscription{" +
                "name='" + name + '\'' +
                ", address=" + address +
                '}';
    }
}
