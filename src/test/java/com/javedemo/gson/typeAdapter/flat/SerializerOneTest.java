package com.javedemo.gson.typeAdapter.flat;

import com.google.gson.Gson;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by Tishka17 on 20.05.2016.
 */
public class SerializerOneTest {
    private class ClassFlat {
        @Flatten("x::y")
        int test;

        @Override
        public String toString() {
            return "ClassFlat{" +
                    "test=" + test +
                    '}';
        }
    }

    private class ClassInner {
        int y;

        @Override
        public String toString() {
            return "ClassInner{" +
                    "y=" + y +
                    '}';
        }
    }

    private class ClassComplex {
        Integer y;
        Integer test;
        ClassInner x;

        @Override
        public String toString() {
            return "ClassComplex{" +
                    "y=" + y +
                    ", test=" + test +
                    ", x=" + x +
                    '}';
        }
    }

    @Test
    public void test_serialize_one() {
        ClassFlat one = new ClassFlat();
        one.test = 13;

        final Gson gson = Helper.createFlatteningGson();
        final Gson gson_default = Helper.createDefaultGson();

        String res = gson.toJson(one);
        System.out.println("res="+res);
        assertNotNull(res);
        assertNotEquals("", res);
        ClassComplex complex = gson_default.fromJson(res, ClassComplex.class);
        assertNotNull(complex.x);
        assertEquals(complex.x.y, one.test);
        assertNull(complex.test);
        assertNull(complex.y);
        String s = gson_default.toJson(complex);
        System.out.println("complex="+s);
        System.out.println("complex="+gson.toJson(complex));

        ClassFlat classFlat = gson.fromJson(s, ClassFlat.class);

        System.out.println("classFat="+classFlat);
    }
}
