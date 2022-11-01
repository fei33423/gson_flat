package com.javedemo.gson.typeAdapter.simpleflat;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.*;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.javedemo.gson.jsonAdapter.FieldNamePrefix;

import java.io.IOException;
import java.lang.reflect.Field;
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

    Map<Class,InterfaceFieldParser> interfaceFieldParsers;

    /**
     * Injects a new instance of {@link SimpleGsonFlatSupport} into given {@link Gson} instance
     * with use of reflection.
     *
     * @param gson instance to inject to
     */
    public static void injectInto(Gson gson, Map<Class,InterfaceFieldParser> interfaceFieldParsers) {
        new FlatReflectionTypeAdapterFactory(gson,interfaceFieldParsers);
    }

    @SuppressWarnings("unchecked")
    protected FlatReflectionTypeAdapterFactory(Gson gson, Map<Class,InterfaceFieldParser> interfaceFieldParsers) {
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
           this.interfaceFieldParsers= interfaceFieldParsers;
            this.gson= gson;
//            factoriesField.set(originalGson, factories);
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
        return new Adapter<>(constructor, buildBoundFields(gson, type, Collections.emptyList()),this);
    }

    protected ObjectPathBoundedField createBoundField( final List<Field> fieldPath, final String name, boolean serialize, boolean deserialize) {
        // special casing primitives here saves ~5% on Android...
        List<String> prefix = Lists.newArrayList();
        for (Field field : fieldPath) {
            FieldNamePrefix annotation = field.getAnnotation(FieldNamePrefix.class);
            if (annotation != null) {
                prefix.add(annotation.value());
            }
        }
        prefix.add(name);
        String join = Joiner.on(".").join(prefix);
        return new ObjectPathBoundedField(gson, fieldPath, join, serialize, deserialize);
    }

    /***
     * 非常依赖bean的解析能力.
     * @param type
     * @param fieldPath
     * @return
     */
    protected Map<String, ObjectPathBoundedField> buildBoundFields( Gson gson,TypeToken<?> type, List<Field> fieldPath) {
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
                TypeAdapter<?> adapter = gson.getAdapter(declaringClass);

                if (adapter instanceof ReflectiveTypeAdapterFactory.Adapter && !field.getType().isInterface()) {
                    for (Map.Entry<String, ObjectPathBoundedField> entry : buildBoundFields(gson, fieldTypeToken, fieldsPath).entrySet()) {
                        ObjectPathBoundedField previous = result.put(entry.getKey(), entry.getValue());
                        if (previous != null)
                            throw new IllegalArgumentException(declaredType + " with flat path "
                                    + fieldPath.stream().map(Field::getName).collect(Collectors.joining("."))
                                    + " multiple JSON fields named , fieldName=" + previous.getName() + ",entry.getKey()=" + entry.getKey());
                    }
                } else {
                    ObjectPathBoundedField boundField = createBoundField( fieldsPath, getFieldName(field), serialize, deserialize);

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



    protected static class Adapter<T> extends TypeAdapter<T> {
        protected final ObjectConstructor<T> constructor;
        protected final Map<String, ObjectPathBoundedField> boundFields;
        protected final FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory;

        protected Adapter(ObjectConstructor<T> constructor, Map<String, ObjectPathBoundedField> boundFields,  FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory) {
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
                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    ObjectPathBoundedField field = boundFields.get(name);
                    if (field == null || !field.deserialized) {
                        in.skipValue();
                    } else {

                        field.read(in, instance);

                    }
                }
            } catch (IllegalStateException e) {
                throw new JsonSyntaxException(e);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            in.endObject();
            return instance;
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
                for (ObjectPathBoundedField boundField : boundFields.values()) {
                    if (boundField.serialized) {
                        if (boundField.lastField.getType().isInterface()) {

                            InterfaceFieldParser interfaceFieldParser = flatReflectionTypeAdapterFactory.interfaceFieldParsers.get(boundField.lastField.getType());
                            InterfaceBoundedField boundedField= interfaceFieldParser.getBoundedFildsForWrite(flatReflectionTypeAdapterFactory);
                            out.name(boundedField.typeName).value(boundedField.typeValue);
                            // 填充字段
                            for (ObjectPathBoundedField interfaceField : boundedField.objectPathBoundedFields) {
                                interfaceField.write(out,value);
                            }
                        } else {
                            out.name(boundField.getName());
                            boundField.write(out, value);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
            out.endObject();
        }
    }


    public  interface  InterfaceFieldParser{

        InterfaceBoundedField getBoundedFildsForWrite(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory);
        Map<String,InterfaceBoundedField> getBoundedFildsForRead(FlatReflectionTypeAdapterFactory flatReflectionTypeAdapterFactory);

    }

   public static class InterfaceBoundedField {

        String typeName;
        String typeValue;
        List<ObjectPathBoundedField> objectPathBoundedFields;

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

       public List<ObjectPathBoundedField> getObjectPathBoundedFields() {
           return objectPathBoundedFields;
       }

       public void setObjectPathBoundedFields(List<ObjectPathBoundedField> objectPathBoundedFields) {
           this.objectPathBoundedFields = objectPathBoundedFields;
       }
   }

    protected class ObjectPathBoundedField {
        protected final String name;
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
        protected final TypeAdapter<?> typeAdapter;

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
        protected void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
            // find needed object by path. 层层推进,更换value,直到获取到该字段的真正的持有对象.
            for (Field field : fieldPath) {
                value = field.get(value);
                if (value == null) break;
            }
            TypeAdapter t = new TypeAdapterRuntimeTypeWrapper(context, this.typeAdapter, resolvedType);
            t.write(writer, value);
        }


        protected void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {

            //            步步推进,初始化每层的object.
            for (Field field : fieldPath.subList(0, fieldPath.size() - 1)) {
                Object child = field.get(value);
                if (child == null) {
                    child = constructorConstructor.get(TypeToken.get(field.getType())).construct();
                    field.set(value, child);
                }
                value = child;
            }

            TypeAdapter<?> choose = null;

            //如果是接口,静态时无法解析出真正的Adapter,反序列化时,动态更换成真正的type.
            if (lastField.getType().isInterface()) {
                choose = context.getAdapter(lastField.getType());
            } else {
                // 否则使用静态分析的typeAdapter
                choose = typeAdapter;
            }

            // 使用静态解析好的typeAdapter读取值.
            Object fieldValue = choose.read(reader);
            if (fieldValue != null || !isPrimitive) {
                fieldPath.get(fieldPath.size() - 1).set(value, fieldValue);
            }
        }
    }
}
