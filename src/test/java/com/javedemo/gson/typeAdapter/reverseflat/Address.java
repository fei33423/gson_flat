package com.javedemo.gson.typeAdapter.reverseflat;

public class Address {
    String city;
    String address;
    Home home;

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

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
    }

    @Override
    public String toString() {
        return "Address{" +
                "city='" + city + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
