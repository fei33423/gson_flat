package com.javedemo.gson.parentJson;

import com.google.gson.Gson;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ParentClazzTest {

    @Test
    public void test() {
        Container container = new Container();
        Son son = new Son();
        container.setParent(son);

        son.setSonName("sName");
        son.setName("pName");
        Gson gson = new Gson();
        String s = gson.toJson(container);
        System.out.println("containerOriginal=" + container);

        System.out.println("containeJson=" + s);
        container = gson.fromJson(s, Container.class);
        System.out.println("containerAfterFromJson=" + container);

    }

}