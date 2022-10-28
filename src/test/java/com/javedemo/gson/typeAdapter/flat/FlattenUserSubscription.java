package com.javedemo.gson.typeAdapter.flat;

public class FlattenUserSubscription {
    String name;

    // new!
//        @com.google.gson.annotations.JsonAdapter(AddressSerializer.class)
    @com.javedemo.gson.typeAdapter.flat.Flatten("12")
    Address address;

    @Flatten("12")
    Address address1;

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

    public Address getAddress1() {
        return address1;
    }

    public void setAddress1(Address address1) {
        this.address1 = address1;
    }

    @Override
    public String toString() {
        return "FlattenUserSubscription{" +
                "name='" + name + '\'' +
                ", address=" + address +
                '}';
    }
}
