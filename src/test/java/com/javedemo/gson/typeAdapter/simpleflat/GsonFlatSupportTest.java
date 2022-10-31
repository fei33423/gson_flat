package com.javedemo.gson.typeAdapter.simpleflat;

import com.google.gson.*;
import com.javedemo.gson.jsonAdapter.FieldNamePrefix;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Type;

public class GsonFlatSupportTest {
    private final Gson gson = new GsonBuilder().registerTypeAdapter(IItem.class, new InterfaceAdapter()).create();

    {
        GsonFlatSupport.injectInto(gson);
    }


    private final String expectedJsonOriginal = "{\"name\":\"Douglas\",\"bag\":{\"itemName\":\"Brush\",\"part\":{\"partName\":\"Battery\"}}}";


    @Test
    public void testPersonToJsonByOriginalGson() {
        String actual = new Gson().toJson(getPerson());
        System.out.println("actual=" + actual);

        Assert.assertEquals(actual, expectedJsonOriginal);
    }


    @Test
    public void testPerson() {
        Person person = getPerson();

        String expectedJson = getExpectedJson();


        String actual = gson.toJson(person);
        System.out.println("ObjectToJson person=" + person + ",\nactual=" + actual);
        Assert.assertEquals(actual, expectedJson, "ObjectToJson check error");


        Person actualPerson = gson.fromJson(expectedJson, Person.class);
        System.out.println("JsonToObject actualPerson=" + actualPerson);
        Assert.assertEquals(actualPerson.toString(), person.toString(), "JsonToObject check error");
    }

    private String getExpectedJson() {
        return "{\"name\":\"Douglas\",\"itemName\":\"Brush\",\"partName\":\"Battery\"}";
    }

    private Person getPerson() {
        Person person = new Person();


        person.name = "Douglas";
        person.bag = new Item();
        person.bag.itemName = "Brush";
        person.bag.part = new Part();
        person.bag.part.partName = "Battery";
//        person.bag2=person.bag;
        return person;
    }


    @Test
    public void testPersonWithTwoBag() {


        Person person = getPersonWithTwoBag();


        String expectedJson = "{\"name\":\"Douglas\",\"itemName\":\"Brush\",\"partName\":\"Battery\",\"bag2.itemName\":\"Brush\",\"bag2.partName\":\"Battery\",\"bag3.bag3\":{\"itemName\":\"Brush\",\"partName\":\"Battery\"}}";


        String actual = gson.toJson(person);
        System.out.println("JsonToObject actual=" + actual+"\n person=" + person );
        Assert.assertEquals(actual, expectedJson, "JsonToObject checkError ");


        Person actualPerson = gson.fromJson(expectedJson, Person.class);
        System.out.println("ObjectTOJson actual=" + actualPerson);
        Assert.assertEquals(person.toString(), person.toString(), "ObjectTOJson");
    }

    private Person getPersonWithTwoBag() {
        Person personWithTwoBag = new Person();


        personWithTwoBag.name = "Douglas";
        personWithTwoBag.bag = new Item();
        personWithTwoBag.bag.itemName = "Brush";
        personWithTwoBag.bag.part = new Part();
        personWithTwoBag.bag.part.partName = "Battery";
        personWithTwoBag.bag2 = personWithTwoBag.bag;
        personWithTwoBag.bag3 = personWithTwoBag.bag;

        return personWithTwoBag;
    }

    public static class PersonCircle {
        public CircleSon circleSon;
    }

    public static class CircleSon {
        public PersonCircle circleSon;
    }

    @Test
    public void testCircleObjectPerson() {


        PersonCircle circleObjectPerson = new PersonCircle();


        String expectedCircleObjectJson = "{\"name\":\"Douglas\",\"itemName\":\"Brush\",\"partName\":\"Battery\"}";

        try {
            String actual = gson.toJson(circleObjectPerson);


            System.out.println("ObjectToJson acutal=" + actual);
            Assert.assertEquals(actual, expectedCircleObjectJson, "ObjectToJson check error ");

            Person actualPerson = gson.fromJson(expectedCircleObjectJson, Person.class);
            System.out.println("JsonToObject actualPerson=" + actualPerson);
            Assert.assertEquals(actualPerson.toString(), circleObjectPerson.toString(), " JsonToObject check error ");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("circle depend pre="), "is not circle depend  exception");
        }
    }


    /**
     * 如果包含了clazz循环依赖. 那就需要将依赖的对象转换成id.依赖id即可 而不是依赖bean.
     * 相当于变成了1对多的模式.
     * //或者拉平到外面来.
     */
    @Test
    public void testCircleClazzPerson() {

        PersonCircle circleClassPerson = new PersonCircle();

        try {

            String expectedCircleClazzJson = "{\"name\":\"Douglas\",\"itemName\":\"Brush\",\"partName\":\"Battery\"}";


            String actual = gson.toJson(circleClassPerson);
            System.out.println("ObjectToJson actual=" + actual);
            Assert.assertEquals(actual, expectedCircleClazzJson, "ObjectToJson check error");


            Person actualPerson = gson.fromJson(expectedCircleClazzJson, Person.class);
            System.out.println("JsonToObject actualPerson=" + actualPerson);
            Assert.assertEquals(actualPerson.toString(), circleClassPerson.toString(), "JsonToObject check error");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("circle depend pre="), "is not circle depend  exception");
        }
    }


    @Test
    public void testFlatPerson() {

        PersonFlattened personFlat = new PersonFlattened();
        personFlat.name = "Douglas";
        personFlat.itemName = "Brush";
        personFlat.partName = "Battery";


        String expectedJson = getExpectedJson();
        Assert.assertEquals(expectedJson, gson.toJson(personFlat), " ObjectToJson check error ");

        PersonFlattened personFlattened = gson.fromJson(expectedJson, PersonFlattened.class);
        Assert.assertEquals(personFlat.toString(), personFlattened.toString(), "JsonToObject check error");

    }


    private static class Person {
        protected String name;
        protected Item bag;
        @FieldNamePrefix(value = "bag2")
        protected Item bag2;
        @FieldNamePrefix(value = "bag3")
        private IItem bag3;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }

    }

    private static interface IItem {

    }

    final class InterfaceAdapter implements JsonSerializer<IItem>, JsonDeserializer<IItem> {

        @Override
        public IItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonElement= (JsonObject) json;
            JsonElement iItemType = jsonElement.get("IItemType");
            return context.deserialize(json,Item.class);
        }

        @Override
        public JsonElement serialize(IItem src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonElement=new JsonObject();
            jsonElement.addProperty( "IItemType",src.getClass().getSimpleName());
            JsonObject jsonElement1 = (JsonObject) context.serialize(src);
            jsonElement1.entrySet().stream().forEach(item->{ jsonElement.add(item.getKey(),item.getValue());});

            return jsonElement1;
        }
    }


    private static class Item  implements IItem{
        protected String itemName;
        protected Part part;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }


    private static class Part {
        protected String partName;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

    /**
     * Should be analoguous to {@link Person} since
     */

    private static class PersonFlattened {
        protected String name;
        protected String itemName;
        protected String partName;

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }
}
