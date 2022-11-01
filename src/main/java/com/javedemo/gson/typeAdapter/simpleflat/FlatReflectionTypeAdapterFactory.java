package com.javedemo.gson.typeAdapter.simpleflat;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.*;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.javedemo.gson.jsonAdapter.DynamicField;
import com.javedemo.gson.jsonAdapter.FieldNamePrefix;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Same as {@link ReflectiveTypeAdapterFactory}
 * Reflectively inject itself into existing {@link Gson} by replacing {@link ReflectiveTypeAdapterFactory} there.
 * <p>
 * Usage:
 * <pre>
 *     Gson gson = new Gson();
 *     GsonFlatSupport.injectInto(gson);
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
public class FlatReflectionTypeAdapterFactory implements TypeAdapterFactory {
    protected final ConstructorConstructor constructorConstructor;
    protected final FieldNamingStrategy fieldNamingPolicy;
    protected final Excluder excluder;
    List<TypeAdapterFactory> oldFactories;
    protected final Gson gson;
    protected final Gson originalFactoriesGson;

    Map<Class, InterfaceFieldParser> dynamicFieldParser;

    /**
     * Injects a new instance of {@link SimpleGsonFlatSupport} into given {@link Gson} instance
     * with use of reflection.
     *
     * @param gson instance to inject to
     */
    public static void injectInto(Gson gson, Map<Class, InterfaceFieldParser> interfaceFieldParsers) {
        new FlatReflectionTypeAdapterFactory(gson, interfaceFieldParsers);
    }

    @SuppressWarnings("unchecked")
    protected FlatReflectionTypeAdapterFactory(Gson gson, Map<Class, InterfaceFieldParser> dynamicFieldParser) {
        /*
         * Dirty work goes here.
         */
        try {
            FieldNamingPolicy fieldNamingPolicy = null;
            Excluder excluder = null;

            Field factoriesField = Gson.class.getDeclaredField("factories");
            factoriesField.setAccessible(true);


            List<TypeAdapterFactory> factories = (List<TypeAdapterFactory>) factoriesField.get(gson);
            List<TypeAdapterFactory> replacementFactories = new ArrayList<>();
            oldFactories = factories;
            this.dynamicFieldParser = dynamicFieldParser;
            this.gson = gson;
            originalFactoriesGson = new Gson();
            factoriesField.set(originalFactoriesGson, factories);
            ;
            for (TypeAdapterFactory factory : factories) {
                if (factory instanceof ReflectiveTypeAdapterFactory) {
                    ReflectiveTypeAdapterFactory reflectiveFactory = (ReflectiveTypeAdapterFactory) factory;
                    Field fieldNamingPolicyField = reflectiveFactory.getClass().getDeclaredField("fieldNamingPolicy");
                    fieldNamingPolicyField.setAccessible(true);
                    fieldNamingPolicy = (FieldNamingPolicy) fieldNamingPolicyField.get(reflectiveFactory);
                    // replace reflective type adapter by this one
                    factory = this;
                }
                if (factory instanceof Excluder) {
                    excluder = (Excluder) factory;
                }
                replacementFactories.add(factory);
            }
            // replace whole Gson.factories list by ours, because its unmodifiable
            factoriesField.set(gson, Collections.unmodifiableList(replacementFactories));

            Field constructorConstructorField = gson.getClass().getDeclaredField("constructorConstructor");
            constructorConstructorField.setAccessible(true);
            this.constructorConstructor = (ConstructorConstructor) constructorConstructorField.get(gson);

            if (fieldNamingPolicy == null)
                throw new RuntimeException("reflective injection failed: no fieldNamingPolicy found");
            if (excluder == null) throw new RuntimeException("reflective injection failed: no excluder found");
            this.fieldNamingPolicy = fieldNamingPolicy;
            this.excluder = excluder;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean excludeField(Field f, boolean serialize) {
        return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
    }

    protected String getFieldName(Field f) {
        SerializedName serializedName = f.getAnnotation(SerializedName.class);
        return serializedName == null ? fieldNamingPolicy.translateName(f) : serializedName.value();
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
        Class<? super T> raw = type.getRawType();

        if (!Object.class.isAssignableFrom(raw)) {
            return null; // it's a primitive!
        }

        ObjectConstructor<T> constructor = constructorConstructor.get(type);
        Map<String, ObjectPathBoundedField> boundFields = buildBoundFields(gson, type, Collections.emptyList());
        FlatReflectionTypeAdapter<T> tFlatReflectionTypeAdapter = new FlatReflectionTypeAdapter<>(constructor, boundFields, this);
        return tFlatReflectionTypeAdapter;
    }

    protected ObjectPathBoundedField createBoundField(final List<Field> fieldPath, final String name, boolean serialize, boolean deserialize) {
        // special casing primitives here saves ~5% on Android...
        List<String> prefix = Lists.newArrayList();

        for (Field field : fieldPath) {
            FieldNamePrefix fieldNamePrefix = field.getAnnotation(FieldNamePrefix.class);
            DynamicField dynamicField = field.getAnnotation(DynamicField.class);

            if (fieldNamePrefix == null && (field.getType().isInterface() || dynamicField != null)) {
                // 接口需要动态化获取 ObjectPathBoundedField , 故必须要有接口的识别字符.
                throw new RuntimeException("can not find FieldNamePrefix of interFace. fieldPath=" + fieldPath);
            }
            if (fieldNamePrefix != null) {
                prefix.add(fieldNamePrefix.value());
            }
        }
        Field lastField = fieldPath.get(fieldPath.size() - 1);
        DynamicField dynamicField = lastField.getAnnotation(DynamicField.class);
        if (!(lastField.getType().isInterface() || dynamicField != null)) {
            prefix.add(name);
        }
        String join = Joiner.on(".").join(prefix);
        return new ObjectPathBoundedField(gson, fieldPath, join, serialize, deserialize);
    }

    /***
     * 非常依赖bean的解析能力.
     * @param type
     * @param fieldPath
     * @return
     */
    protected Map<String, ObjectPathBoundedField> buildBoundFields(Gson gson, TypeToken<?> type, List<Field> fieldPath) {
        Map<String, ObjectPathBoundedField> result = new LinkedHashMap<>();
        Class<?> raw = type.getRawType();


        if (raw.isInterface()) {
            return result;
        }

        for (Field field : fieldPath) {
            if (field.getDeclaringClass().isAssignableFrom(raw)) {
                throw new RuntimeException("circle depend pre=" + field + ",now=" + field);
            }
        }

        Type declaredType = type.getType();
        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {
                boolean serialize = excludeField(field, true);
                boolean deserialize = excludeField(field, false);
                if (!serialize && !deserialize) {
                    continue;
                }
                field.setAccessible(true);

                // field path: current + processing field
                ArrayList<Field> fieldsPath = new ArrayList<>(fieldPath);
                fieldsPath.add(field);

                Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());

                TypeToken<?> fieldTypeToken = TypeToken.get(fieldType);
                Class<?> declaringClass = field.getType();
                TypeAdapter<?> adapter = originalFactoriesGson.getAdapter(declaringClass);

                if (adapter instanceof ReflectiveTypeAdapterFactory.Adapter && !field.getType().isInterface()) {
                    for (Map.Entry<String, ObjectPathBoundedField> entry : buildBoundFields(gson, fieldTypeToken, fieldsPath).entrySet()) {
                        ObjectPathBoundedField previous = result.put(entry.getKey(), entry.getValue());
                        if (previous != null)
                            throw new IllegalArgumentException(declaredType + " with flat path "
                                    + fieldPath.stream().map(Field::getName).collect(Collectors.joining("."))
                                    + " multiple JSON fields named , fieldName=" + previous.getName() + ",entry.getKey()=" + entry.getKey());
                    }
                } else {
                    ObjectPathBoundedField boundField = createBoundField(fieldsPath, getFieldName(field), serialize, deserialize);

                    ObjectPathBoundedField previous = result.put(boundField.getName(), boundField);
                    if (previous != null) {
                        throw new IllegalArgumentException(declaredType
                                + " declares multiple JSON fields named " + previous.getName());
                    }
                }
            }
            type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
            raw = type.getRawType();
        }
        return result;
    }


    protected static class FlatReflectionTypeAdapter<T> extends TypeAdapter<T> {
        protected final ObjectConstructor<T> constructor;
        protected final Map<String, ObjectPathBoundedField> boundFields;
        protected final FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory;

        protected FlatReflectionTypeAdapter(ObjectConstructor<T> constructor, Map<String, ObjectPathBoundedField> boundFields, FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory) {
            this.constructor = constructor;
            this.boundFields = boundFields;
            this.flatReflectionTypeAdapterFactory = flatReflectionTypeAdapterFactory;
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            T instance = constructor.construct();

            try {
                JsonObject jsonObject = (JsonObject) Streams.parse(in);
                readFields(boundFields,"",instance, jsonObject);
            } catch (
                    IllegalStateException e) {
                throw new JsonSyntaxException(e);
            } catch (
                    IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                throw new AssertionError(e);
            }
            return instance;
        }

        private void readFields(Map<String, ObjectPathBoundedField> boundFields,String prefix, Object  instance, JsonObject jsonObject) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
            Iterator<Map.Entry<String, JsonElement>> iterator = jsonObject.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, JsonElement> entry = iterator.next();
                String name = entry.getKey();
                JsonElement jsonElement = entry.getValue();
                //去除前缀. 特别是对多层接口对象静态嵌套.
                String key = name.replaceFirst(StringUtils.isEmpty(prefix) ? "" : prefix + "\\.", "");
                ObjectPathBoundedField pathField = boundFields.get(key);


                if (pathField == null) {
                    continue;
                } else if (!pathField.deserialized) {
                    iterator.remove();
                } else {
                    iterator.remove();
                    pathField.read(jsonElement, instance);

                }
            }
            //对剩余的字段解析
            fillRuntimeTypeObject(instance,prefix, jsonObject);
        }

        private Object fillRuntimeTypeObject(Object instance, String prefix, JsonObject jsonObject) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
            Iterator<Map.Entry<String, JsonElement>> iterator;
            iterator = jsonObject.entrySet().iterator();


            JsonObject newJsonObject = new JsonObject();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonElement> entry = iterator.next();
                String name = entry.getKey();
                name=StringUtils.isEmpty(prefix) ? name:name.replaceFirst(prefix+"\\.","") ;
                String[] split = name.split("\\.");
                String subfix = "";
                String prefixTemp = name;
                ObjectPathBoundedField pathField = null;
                for (int index = split.length - 1; index >= 0; index--) {
                    subfix = StringUtils.isEmpty(subfix) ? split[index] : split[index] + "." + subfix;
                    prefixTemp = prefixTemp.replace("." + subfix, "");
                    //在静态类解析的boundFields中,通过前缀匹配对应的 pathField
                    pathField = boundFields.get(prefixTemp);
                    if (pathField != null) {
                        break;
                    }
                }

                // 通过静态类匹配到的属性,进一步获取对应的动态类解析类
                InterfaceFieldParser interfaceFieldParser = flatReflectionTypeAdapterFactory.dynamicFieldParser.get(pathField.lastField.getType());

                // 通过解析类获取到动态类集合
                DynamicTypeInterfaceBoundedField boundedFildsForRead = interfaceFieldParser.getBoundedFildsForRead(flatReflectionTypeAdapterFactory);

                // 获取到动态类的typeName
                String typeName = boundedFildsForRead.getTypeName();
                // 通过前缀+typeName获取到 指定的 动态类型的类型值
                JsonElement typeValueJsonElement = jsonObject.get(prefixTemp + "." + typeName);
                // 通过动态类型的类型值获取到指定的 InterfaceBoundedField
                InterfaceBoundedField interfaceBoundedField = boundedFildsForRead.getMap().get(typeValueJsonElement.getAsString());

                // 根据class 构造  动态类的对象
                Object fieldValue = interfaceBoundedField.getClazz().getConstructor().newInstance();

                // 设置动态类对象到 上级对象中
                pathField.setPathValue(instance,fieldValue);

                readFields(interfaceBoundedField.objectPathBoundedFields,prefix+"."+prefixTemp ,fieldValue,jsonObject);

            }
            return instance;
        }

        /**
         * 当前仅支持接口. 非接口需要根据class
         *
         * @param field
         * @return
         */
        private boolean isDynamic(InterfaceFieldParser interfaceFieldParser, ObjectPathBoundedField field) {
            if (interfaceFieldParser != null) {
                return true;
            }
            if (field.lastField.getType().isInterface()) {
                throw new RuntimeException("can not find  InterfaceFieldParser of interface ,clazz=" + field.lastField.getType().getSimpleName());
            }
            return false;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

//            Field outField = ReflectionUtils.findField(out.getClass(), "out");
//            outField.setAccessible(true);
//            Object outInner=null;
//            try {
//                outInner = outField.get(out);
//            } catch (IllegalAccessException e) {
//                throw new RuntimeException("get out from JsonWriter error",e);
//            }
            out.beginObject();
            try {
                writeField(boundFields,"",out, value);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
            out.endObject();
        }

        public void writeField(Map<String,ObjectPathBoundedField> boundFields, String prefix,JsonWriter writer, Object value) throws IllegalAccessException, IOException {
            for (ObjectPathBoundedField boundField : boundFields.values()) {
                if (boundField.serialized) {

                    if (boundField.typeAdapter instanceof FlatReflectionTypeAdapter) {

                        // 当前仅支持接口 不支持 父子类模式. 需要改造 不通过boundFields循环而是通过value的实际类型循环.
                        InterfaceFieldParser interfaceFieldParser = flatReflectionTypeAdapterFactory.dynamicFieldParser.get(boundField.lastField.getType());

                        InterfaceBoundedField interfaceBoundedField = interfaceFieldParser.getBoundedFildsForWrite(flatReflectionTypeAdapterFactory);

                        List<String> prefixs = Lists.newArrayList();
                        for (Field field : boundField.fieldPath) {
                            FieldNamePrefix annotation = field.getAnnotation(FieldNamePrefix.class);
                            if (annotation != null) {
                                prefixs.add(annotation.value());
                            }
                        }
//                            boundField.fieldPath.addAll(boundedField.)

                        Object interfaceObject = boundField.getObject(value);

                        if (interfaceObject==null){
                            continue;
                        }
                        String tempPrefix = Joiner.on(".").join(prefixs);
                        prefix =StringUtils.isEmpty(prefix)? tempPrefix :prefix+"."+tempPrefix;


                        writer.name(StringUtils.isEmpty(prefix) ? interfaceBoundedField.typeName : prefix + "." + interfaceBoundedField.typeName);
                        TypeAdapters.STRING.write(writer, interfaceBoundedField.typeValue);


                        // 填充字段
                        Map<String, ObjectPathBoundedField> objectPathBoundedFields = interfaceBoundedField.objectPathBoundedFields;
                        writeField(objectPathBoundedFields,prefix,writer,interfaceObject);
//                        for (ObjectPathBoundedField interfaceField : objectPathBoundedFields.values()) {
//                            if (interfaceField.typeAdapter instanceof FlatReflectionTypeAdapter){
//                                Object interfaceObjectOfInterface = interfaceField.getObject(interfaceObject);
//                                String name=StringUtils.isEmpty(prefix)? interfaceField.name:prefix+"."+interfaceField.name;
//                                ((FlatReflectionTypeAdapter) interfaceField.typeAdapter).writeField(name,writer, interfaceObjectOfInterface);
//                            }else {
//                                String name = StringUtils.isEmpty(prefix) ? prefix : prefix + "." + interfaceField.getName();
//                                writer.name(name);
//                                interfaceField.write(this, writer, interfaceObject);
//                            }
//                        }
                    } else {
                        String name =StringUtils.isEmpty(prefix)? boundField.getName() :prefix+"."+boundField.getName();

                        writer.name(name);
                        boundField.write(this,writer, value);
                    }
                }
            }
        }
    }


    public interface InterfaceFieldParser {

        InterfaceBoundedField getBoundedFildsForWrite(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory);

        DynamicTypeInterfaceBoundedField getBoundedFildsForRead(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory);

    }


    public static class DynamicTypeInterfaceBoundedField {
        String typeName;
        Map<String, InterfaceBoundedField> map;

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public Map<String, InterfaceBoundedField> getMap() {
            return map;
        }

        public void setMap(Map<String, InterfaceBoundedField> map) {
            this.map = map;
        }
    }

    public static class InterfaceBoundedField {

        String typeName;
        String typeValue;
        Class clazz;
        Map<String, ObjectPathBoundedField> objectPathBoundedFields;

        public Class getClazz() {
            return clazz;
        }

        public void setClazz(Class clazz) {
            this.clazz = clazz;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getTypeValue() {
            return typeValue;
        }

        public void setTypeValue(String typeValue) {
            this.typeValue = typeValue;
        }

        public Map<String, ObjectPathBoundedField> getObjectPathBoundedFields() {
            return objectPathBoundedFields;
        }

        public void setObjectPathBoundedFields(Map<String, ObjectPathBoundedField> objectPathBoundedFields) {
            this.objectPathBoundedFields = objectPathBoundedFields;
        }
    }

    protected class ObjectPathBoundedField {
        protected final String name;
        protected final String fieldPathStr;

        protected final Field lastField;

        protected final boolean serialized;
        protected final boolean deserialized;

        protected final Gson context;
        protected final List<Field> fieldPath;
        protected final Type resolvedType;
        protected final boolean isPrimitive;
        /**
         * 静态分析出的typeAdapter
         */
        protected final TypeAdapter typeAdapter;

        protected ObjectPathBoundedField(Gson context, List<Field> fieldPath, String name, boolean serialize, boolean deserialize) {
            this.name = name;
            this.serialized = serialize;
            this.deserialized = deserialize;
            this.context = context;
            lastField = fieldPath.get(fieldPath.size() - 1);
            this.resolvedType = $Gson$Types.resolve(lastField.getDeclaringClass(), lastField.getDeclaringClass(), lastField.getType());
            TypeToken<?> fieldType = TypeToken.get(resolvedType);
            this.fieldPath = fieldPath;
            this.isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
            TypeAdapter<?> adapter = context.getAdapter(fieldType);
            this.typeAdapter = adapter;
            List<String> list=Lists.newArrayList();
            for (Field field : fieldPath) {
                list.add(field.getDeclaringClass().getSimpleName()+"."+field.getName());
            }
            fieldPathStr=Joiner.on("\n").join(list);
        }

        public String getName() {
            return name;
        }

        public Field getLastField() {
            return lastField;
        }

        public boolean isSerialized() {
            return serialized;
        }

        public boolean isDeserialized() {
            return deserialized;
        }

        @SuppressWarnings({"unchecked", "rawtypes"}) // the type adapter and field type always agree
        protected void write(FlatReflectionTypeAdapter adapter, JsonWriter writer, Object value) throws IOException, IllegalAccessException {
            value = getObject(value);

                typeAdapter.write(writer, value);

        }

        public Object getObject(Object value) throws IllegalAccessException {
            if (value == null){
                return null;
            }
            // find needed object by path. 层层推进,更换value,直到获取到该字段的真正的持有对象.
            for (Field field : fieldPath) {
                value = field.get(value);
                if (value == null) break;
            }
            return value;
        }

        protected void setPathValue( Object instanceToSetFileld,Object fieldValue) throws IllegalAccessException {
            if (instanceToSetFileld==null){
                return;
            }

            //            步步推进,初始化每层的object.
            for (Field field : fieldPath.subList(0, fieldPath.size() - 1)) {
                Object child = field.get(instanceToSetFileld);
                if (child == null) {
                    child = constructorConstructor.get(TypeToken.get(field.getType())).construct();
                    field.set(instanceToSetFileld, child);
                }
                instanceToSetFileld = child;
            }


            fieldPath.get(fieldPath.size() - 1).set(instanceToSetFileld, fieldValue);
        }

        protected void read(JsonElement jsonElemet, Object instanceToSetFileld) throws IllegalAccessException {

            //            步步推进,初始化每层的object.
            for (Field field : fieldPath.subList(0, fieldPath.size() - 1)) {
                Object child = field.get(instanceToSetFileld);
                if (child == null) {
                    child = constructorConstructor.get(TypeToken.get(field.getType())).construct();
                    field.set(instanceToSetFileld, child);
                }
                instanceToSetFileld = child;
            }


            // 使用静态解析好的typeAdapter读取值.
            Object fieldValue = typeAdapter.fromJsonTree(jsonElemet);
            if (fieldValue != null || !isPrimitive) {
                fieldPath.get(fieldPath.size() - 1).set(instanceToSetFileld, fieldValue);
            }
        }

        @Override
        public String toString() {
            return "ObjectPathBoundedField{" +
                    "name='" + name + '\'' +
                    ", fieldPathStr='" + fieldPathStr + '\'' +
                    '}';
        }
    }
}
