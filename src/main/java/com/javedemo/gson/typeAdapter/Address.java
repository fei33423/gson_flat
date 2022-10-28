package com.javedemo.gson.typeAdapter;

public class Address {
    String city;
    String address;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Address{" +
                "city='" + city + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
