package com.javedemo.gson.typeAdapter.reverseflat;

import com.google.gson.Gson;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Created by Tishka17 on 20.05.2016.
 */
public class SerializerOneTest {
    private class ClassFlat {
        @ReverseFlatten("x::y")
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

        String res = gson.toJson(one);
//        simpleBeanToJson={"x":{"y":13}}
        System.out.println("simpleBeanToJson="+res);
        assertNotNull(res);
        assertNotEquals("", res);
        ClassComplex complex = gson.fromJson(res, ClassComplex.class);
        assertNotNull(complex.x);
        assertEquals(complex.x.y, one.test);
        assertNull(complex.test);
        assertNull(complex.y);
        String s = gson.toJson(complex);
//        complexBeanToComplexJson={"x":{"y":13}}
        System.out.println("complexBeanToComplexJson="+s);

        ClassFlat classFlat = gson.fromJson(s, ClassFlat.class);
// complexJsonToSimpleBean=ClassFlat{test=13}
        System.out.println("complexJsonToSimpleBean="+classFlat);
    }
}
