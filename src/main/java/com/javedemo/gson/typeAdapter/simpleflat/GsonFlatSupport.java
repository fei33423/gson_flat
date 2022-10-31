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
import com.javedemo.gson.jsonAdapter.NonPrivateTypeAdapterRuntimeTypeWrapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Same as {@link ReflectiveTypeAdapterFactory}
 * Reflectively inject itself into existing {@link Gson} by replacing {@link ReflectiveTypeAdapterFactory} there.
 *
 * Usage:
 * <pre>
 *     Gson gson = new Gson();
 *     GsonFlatSupport.injectInto(gson);
 * </pre>
 */
@SuppressWarnings("WeakerAccess")
public class GsonFlatSupport implements TypeAdapterFactory {
    protected final ConstructorConstructor constructorConstructor;
    protected final FieldNamingStrategy fieldNamingPolicy;
    protected final Excluder excluder;
    List<TypeAdapterFactory> oldFactories;
    Gson originalGson;
    /**
     * Injects a new instance of {@link GsonFlatSupport} into given {@link Gson} instance
     * with use of reflection.
     *
     * @param gson  instance to inject to
     */
    public static void injectInto(Gson gson) {
        new GsonFlatSupport(gson);
    }

    @SuppressWarnings("unchecked")
    protected GsonFlatSupport(Gson gson) {
        /*
         * Dirty work goes here.
         */
        try {
            FieldNamingPolicy fieldNamingPolicy = null;
            Excluder excluder = null;

            Field factoriesField = Gson.class.getDeclaredField("factories");
            factoriesField.setAccessible(true);


            List<TypeAdapterFactory> factories = (List<TypeAdapterFactory>) factoriesField.get(gson);
            List<TypeAdapterFactory>  replacementFactories=new ArrayList<>();
            oldFactories=factories;
            originalGson=new Gson();
            factoriesField.set(originalGson,factories);;
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

            if (fieldNamingPolicy == null) throw new RuntimeException("reflective injection failed: no fieldNamingPolicy found");
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
        return new Adapter<>(constructor, getBoundFields(gson, type, Collections.emptyList()));
    }

    protected BoundField createBoundField(final Gson context, final List<Field> fieldPath, final String name, boolean serialize, boolean deserialize) {
        // special casing primitives here saves ~5% on Android...
        List<String> prefix = Lists.newArrayList();
        for (Field field : fieldPath) {
            FieldNamePrefix annotation = field.getAnnotation(FieldNamePrefix.class);
            if(annotation!=null) {
                prefix.add(annotation.value());
            }
        }
        prefix.add(name);
        return new ObjectPathBoundedField(context, fieldPath, Joiner.on(".").join(prefix), serialize, deserialize);
    }

    protected Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, List<Field> fieldPath) {
        Map<String, BoundField> result = new LinkedHashMap<>();
        Class<?> raw = type.getRawType();




        if (raw.isInterface()) {
            return result;
        }

        for (Field field : fieldPath) {
            if(field.getDeclaringClass().isAssignableFrom(raw)){
               throw new RuntimeException("circle depend pre="+field+",now="+field );
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
                TypeAdapter<?> adapter = originalGson.getAdapter(declaringClass);

                if (adapter instanceof ReflectiveTypeAdapterFactory.Adapter) {
                    for(Map.Entry<String, BoundField> entry : getBoundFields(context, fieldTypeToken, fieldsPath).entrySet()) {
                        BoundField previous = result.put(entry.getKey(), entry.getValue());
                        if (previous != null)
                            throw new IllegalArgumentException(declaredType + " with flat path "
                                    + fieldPath.stream().map(Field::getName).collect(Collectors.joining("."))
                                    + " multiple JSON fields named , fieldName=" + previous.getName()+",entry.getKey()="+entry.getKey());
                    }
                } else {
                    BoundField boundField = createBoundField(context, fieldsPath, getFieldName(field), serialize, deserialize);

                    BoundField previous = result.put(boundField.getName(), boundField);
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



    protected static abstract class BoundField {
        protected final String name;
        protected final boolean serialized;
        protected final boolean deserialized;

        protected BoundField(String name, boolean serialized, boolean deserialized) {
            this.name=name;
            this.serialized = serialized;
            this.deserialized = deserialized;
        }

        public String getName() {
            return name;
        }

        public boolean isSerialized() {
            return serialized;
        }

        public boolean isDeserialized() {
            return deserialized;
        }

        protected abstract void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException;
        protected abstract void read(JsonReader reader, Object value) throws IOException, IllegalAccessException;
    }

    protected static class Adapter<T> extends TypeAdapter<T> {
        protected final ObjectConstructor<T> constructor;
        protected final Map<String, BoundField> boundFields;

        protected Adapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
            this.constructor = constructor;
            this.boundFields = boundFields;
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
                    BoundField field = boundFields.get(name);
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

            out.beginObject();
            try {
                for (BoundField boundField : boundFields.values()) {
                    if (boundField.serialized) {
                        out.name(boundField.getName());
                        boundField.write(out, value);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
            out.endObject();
        }
    }

    protected class ObjectPathBoundedField extends BoundField {
        protected final Gson context;
        protected final List<Field> fieldPath;
        protected final Type resolvedType;
        protected final boolean isPrimitive;
        protected final TypeAdapter<?> typeAdapter;

        protected ObjectPathBoundedField(Gson context, List<Field> fieldPath, String name, boolean serialize, boolean deserialize) {
            super(name, serialize, deserialize);
            this.context = context;
            Field lastField = fieldPath.get(fieldPath.size() - 1);
            this.resolvedType = $Gson$Types.resolve(lastField.getDeclaringClass(), lastField.getDeclaringClass(), lastField.getType());
            TypeToken<?> fieldType = TypeToken.get(resolvedType);
            this.fieldPath = fieldPath;
            this.isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
            this.typeAdapter = context.getAdapter(fieldType);
        }

        @SuppressWarnings({"unchecked", "rawtypes"}) // the type adapter and field type always agree
        @Override
        protected void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
            // find needed object by path. 层层推进,更换value,直到获取到该字段的真正的持有对象.
            for (Field field : fieldPath) {
                value = field.get(value);
                if (value == null) break;
            }
            TypeAdapter t = new NonPrivateTypeAdapterRuntimeTypeWrapper(context, this.typeAdapter, resolvedType);
            t.write(writer, value);
        }

        @Override
        protected void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
            Object fieldValue = typeAdapter.read(reader);
            for (Field field : fieldPath.subList(0, fieldPath.size() - 1)) {
                Object child = field.get(value);
                if (child == null) {
                    child = constructorConstructor.get(TypeToken.get(field.getType())).construct();
                    field.set(value, child);
                }
                value = child;
            }
            if (fieldValue != null || !isPrimitive) {
                fieldPath.get(fieldPath.size() - 1).set(value, fieldValue);
            }
        }
    }
}
