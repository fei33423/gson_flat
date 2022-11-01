package com.javedemo.gson.typeAdapter.simpleflat;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.javedemo.gson.jsonAdapter.FieldNamePrefix;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FlatReflectionTypeAdapterFactoryTest {
    private final Gson gson = new GsonBuilder().create();
//    private final Gson gson = new GsonBuilder().create();

    {
        Map<Class, FlatReflectionTypeAdapterFactory.InterfaceFieldParser> map=new HashMap();
        map.put(IWindow.class,new IPartFieldParser());
        map.put(IMaterial.class,new IMaterialFieldParser());

        FlatReflectionTypeAdapterFactory.injectInto(gson,map );
    }


    private final String expectedJsonOriginal = "{\"name\":\"Douglas\",\"bag\":{\"itemName\":\"Brush\",\"part\":{\"partName\":\"Battery\"}}}";

    public static class IPartFieldParser implements FlatReflectionTypeAdapterFactory.InterfaceFieldParser{

        @Override
        public FlatReflectionTypeAdapterFactory.InterfaceBoundedField getBoundedFildsForWrite(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory) {

            FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField boundedFildsForRead = getBoundedFildsForRead(flatReflectionTypeAdapterFactory);
            return boundedFildsForRead.getMap().get("window_实现1");
        }


        @Override
        public FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField getBoundedFildsForRead(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory) {
            FlatReflectionTypeAdapterFactory.InterfaceBoundedField partField=new FlatReflectionTypeAdapterFactory.InterfaceBoundedField();
            Map<String, FlatReflectionTypeAdapterFactory.ObjectPathBoundedField> stringObjectPathBoundedFieldMap =
                    flatReflectionTypeAdapterFactory.buildBoundFields(flatReflectionTypeAdapterFactory.gson, TypeToken.get(Window.class), new ArrayList<>());
            String typeName = "IWindowType";
            partField.setTypeName(typeName);
            partField.setClazz(Window.class);
            partField.setTypeValue("window_实现1");
            partField.setObjectPathBoundedFields(stringObjectPathBoundedFieldMap);

            Map<String, FlatReflectionTypeAdapterFactory.InterfaceBoundedField> map=new HashMap<>();
            map.put(partField.getTypeValue(),partField);
            FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField dynamicTypeInterfaceBounded=new FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField();
            dynamicTypeInterfaceBounded.setTypeName(typeName);
            dynamicTypeInterfaceBounded.setMap(map);

            return dynamicTypeInterfaceBounded;
        }
    }

    public static class IMaterialFieldParser implements FlatReflectionTypeAdapterFactory.InterfaceFieldParser{

        @Override
        public FlatReflectionTypeAdapterFactory.InterfaceBoundedField getBoundedFildsForWrite(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory) {

            FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField boundedFildsForRead = getBoundedFildsForRead(flatReflectionTypeAdapterFactory);
            return boundedFildsForRead.getMap().get("纯天然材料");
        }


        @Override
        public FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField getBoundedFildsForRead(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory) {
            FlatReflectionTypeAdapterFactory.InterfaceBoundedField partField=new FlatReflectionTypeAdapterFactory.InterfaceBoundedField();
            Map<String, FlatReflectionTypeAdapterFactory.ObjectPathBoundedField> stringObjectPathBoundedFieldMap =
                    flatReflectionTypeAdapterFactory.buildBoundFields(flatReflectionTypeAdapterFactory.gson, TypeToken.get(NatureMaterial.class), new ArrayList<>());
            String typeName = "IMaterialType";
            partField.setTypeName(typeName);
            partField.setClazz(NatureMaterial.class);
            partField.setTypeValue("纯天然材料");
            partField.setObjectPathBoundedFields(stringObjectPathBoundedFieldMap);

            Map<String, FlatReflectionTypeAdapterFactory.InterfaceBoundedField> map=new HashMap<>();
            map.put(partField.getTypeValue(),partField);
            FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField dynamicTypeInterfaceBounded=new FlatReflectionTypeAdapterFactory.DynamicTypeInterfaceBoundedField();
            dynamicTypeInterfaceBounded.setTypeName(typeName);
            dynamicTypeInterfaceBounded.setMap(map);

            return dynamicTypeInterfaceBounded;
        }
    }

    @Test
    public void testPersonToJsonByOriginalGson() {
        String actual = new Gson().toJson(getPerson());
        System.out.println("actual=" + actual);

        Assert.assertEquals(actual, expectedJsonOriginal);
    }


    @Test
    public void testPerson() {
        ClassRoom person = getPerson();

        String expectedJson = getExpectedJson();


        String actual = gson.toJson(person);
        System.out.println("ObjectToJson person=" + person + ",\nactual=" + actual);
        Assert.assertEquals(actual, expectedJson, "ObjectToJson check error");


        ClassRoom actualPerson = gson.fromJson(expectedJson, ClassRoom.class);
        System.out.println("JsonToObject actualPerson=" + actualPerson);
        Assert.assertEquals(actualPerson.toString(), person.toString(), "JsonToObject check error");
    }

    private String getExpectedJson() {
        return "{\"name\":\"Douglas\",\"itemName\":\"Brush\",\"partName\":\"Battery\"}";
    }

    private ClassRoom getPerson() {
        ClassRoom person = new ClassRoom();


        person.name = "Douglas";
        person.frontDoor = new Door();
        person.frontDoor.doorName = "Brush";
        person.frontDoor.upperWindow = new Window();
        person.frontDoor.upperWindow.windowName = "Battery";
//        person.bag2=person.bag;
        return person;
    }


    /**
     * SimpleGsonFlatSupport的问题是 无法对接口进行平铺.
     * 主要原因是 静态分析时无法进一步对接口解析出字段. 只能中断在接口这里.
     * 把接口作为Path.
     */
    @Test
    public void testPersonWithTwoDoorsAndInterface() {


        ClassRoom classRoom = getClassRoomWithTwoDoors();


        String expectedJson = "{\"name\":\"第15班\",\"doorName\":\"前门\",\"windowName\":\"前门上玻璃\",\"backDoorPrefix.doorName\":\"后门\",\"backDoorPrefix.windowName\":\"后门上玻璃\",\"backDoorPrefix.lowerWindowPrefix.IWindowType\":\"window_实现1\",\"backDoorPrefix.lowerWindowPrefix.windowName\":\"后门下玻璃\",\"backDoorPrefix.lowerWindowPrefix.iMaterialPrefix.IMaterialType\":\"纯天然材料\",\"backDoorPrefix.lowerWindowPrefix.iMaterialPrefix.materialName\":\"后门下玻璃纯天然材料\"}";


        String actual = gson.toJson(classRoom);
        System.out.println("ObjectTOJson actual=" + actual );
        System.out.println("ObjectTOJson expect=" + expectedJson );

        Assert.assertEquals(actual, expectedJson, "ObjectTOJson checkError not equal ");


        ClassRoom actualPerson = gson.fromJson(expectedJson, ClassRoom.class);
        System.out.println("JsonToObject actual=" + actualPerson);
        System.out.println("JsonToObject expect=" + classRoom);

        Assert.assertEquals(actualPerson, classRoom, "JsonToObject check error");
    }

    private ClassRoom getClassRoomWithTwoDoors() {
        ClassRoom personWithTwoBag = new ClassRoom();


        personWithTwoBag.name = "第15班";
        personWithTwoBag.frontDoor = new Door();
        personWithTwoBag.frontDoor.doorName = "前门";
        personWithTwoBag.frontDoor.upperWindow = new Window();
        personWithTwoBag.frontDoor.upperWindow.windowName = "前门上玻璃";
        personWithTwoBag.backDoor = new Door();
        personWithTwoBag.backDoor.doorName="后门";
        personWithTwoBag.backDoor.upperWindow = new Window();
        personWithTwoBag.backDoor.upperWindow.windowName = "后门上玻璃";
        Window lowerWindow = new Window();
        lowerWindow.windowName= "后门下玻璃";
        NatureMaterial material = new NatureMaterial();
        material.materialName="后门下玻璃纯天然材料";
        lowerWindow.material= material;
        personWithTwoBag.backDoor.lowerWindow = lowerWindow;

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

            ClassRoom actualPerson = gson.fromJson(expectedCircleObjectJson, ClassRoom.class);
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


            ClassRoom actualPerson = gson.fromJson(expectedCircleClazzJson, ClassRoom.class);
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


    private static class ClassRoom {
        protected String name;
        protected Door frontDoor;
        @FieldNamePrefix(value = "backDoorPrefix")
        protected Door backDoor;


        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
        }

        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    private static interface IWindow {

    }
    public static class IPartAdapterFactory implements TypeAdapterFactory{

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

            if (!type.getRawType().equals(IWindow.class)){
                return null;
            }

            return new TypeAdapter<T>(){
                @Override
                public void write(JsonWriter out, T value) throws IOException {

                    out.name("itemType").value(value.getClass().getName());
//                    TypeAdapter adapter = gson.getAdapter(value.getClass());
//                    adapter.write(out, value);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    String name = in.nextName();
                    Class type = null;
                    if (name == Window.class.getName()){
                        type= Window.class;
                    }
                    TypeAdapter<Door> adapter = gson.getAdapter(type);
                    return (T) adapter.read(in);
                }
            };
        }
    }

    final class InterfaceAdapter implements JsonSerializer<IWindow>, JsonDeserializer<IWindow> {

        @Override
        public IWindow deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonElement= (JsonObject) json;
            JsonElement iItemType = jsonElement.get("IItemType");
            return context.deserialize(jsonElement, Door.class);
        }

        @Override
        public JsonElement serialize(IWindow src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonElement=new JsonObject();
            jsonElement.addProperty( "IItemType",src.getClass().getSimpleName());
            JsonObject jsonElement1 = (JsonObject) context.serialize(src);
            jsonElement1.entrySet().stream().forEach(item->{ jsonElement.add(item.getKey(),item.getValue());});

            return jsonElement;
        }
    }


    private static class Door {
        protected String doorName;
        // 上玻璃
        protected Window upperWindow;
        //下玻璃
        @FieldNamePrefix(value = "lowerWindowPrefix")
        private IWindow lowerWindow;
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }
    }

    public static interface IMaterial{

    }

    public static class NatureMaterial implements IMaterial{

        private String materialName;

    }

    private static class Window implements IWindow {
        protected String windowName;

        @FieldNamePrefix(value ="iMaterialPrefix")
        IMaterial material;
        public Window(){}

        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    /**
     * Should be analoguous to {@link ClassRoom} since
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
