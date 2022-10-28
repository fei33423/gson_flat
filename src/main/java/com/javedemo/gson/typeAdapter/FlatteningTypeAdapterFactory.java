package com.javedemo.gson.typeAdapter;

import com.google.gson.*;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 *   由于Gson将利用注解@jsonAdapter某个类拉平本身无法实现.
 *   详见JsonAdapterDemo类和
 *   但是可以实现包含了需要拉平的类的 组装改变.
 *   动态adapt生成.
 *   故需使用到TypeAdapterFactory
 */
public class FlatteningTypeAdapterFactory implements TypeAdapterFactory {

    private FlatteningTypeAdapterFactory() {
    }

    private static final TypeAdapterFactory instance = new FlatteningTypeAdapterFactory();

    private static final String[] emptyStringArray = {};

    public static TypeAdapterFactory getInstance() {
        return instance;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {

        final Class<?> rawType = typeToken.getRawType();
        // if the class to be serialized or deserialized is known to never contain @Flatten-annotated elements
        if (rawType == Object.class
                || rawType == Void.class
                || rawType.isPrimitive()
                || rawType.isArray()
                || rawType.isInterface()
                || rawType.isAnnotation()
                || rawType.isEnum()
                || rawType.isSynthetic()) {
            // then just skip it
            return null;
        }
        // otherwise traverse the given class up to java.lang.Object and collect all of its fields
        // that are annotated with @Flatten having their names transformed using FieldNamingStrategy
        // in order to support some Gson built-ins like @SerializedName
        final FieldNamingStrategy fieldNamingStrategy = gson.fieldNamingStrategy();
        final Excluder excluder = gson.excluder();
        final Collection<String> propertiesToFlatten = new HashSet<>();
        for (Class<?> c = rawType; c != Object.class; c = c.getSuperclass()) {
            for (final Field f : c.getDeclaredFields()) {
                // only support @Flatten-annotated fields that aren't excluded by Gson (static or transient fields, are excluded by default)
                if (f.isAnnotationPresent(Flatten.class) && !excluder.excludeField(f, true)) {
                    // and collect their names as they appear from the Gson perspective (see how @SerializedName works)
                    propertiesToFlatten.add(fieldNamingStrategy.translateName(f));
                }
            }
        }
        // if nothing collected, obviously, consider we have nothing to do
        if (propertiesToFlatten.isEmpty()) {
            return null;
        }
        // 动态的adpter. 解析一个类,如果内部属性中含@flat注解,就将外部类 typeAdpter.
        return new TypeAdapter<T>() {
            private final TypeAdapter<T> delegate = gson.getDelegateAdapter(FlatteningTypeAdapterFactory.this, typeToken);

            @Override
            public void write(final JsonWriter out, final T value)
                    throws IOException {
                // on write, buffer the given value into a JSON tree (it costs but it's easy)
                final JsonElement outerElement = delegate.toJsonTree(value);
                if (outerElement.isJsonObject()) {
                    final JsonObject outerObject = outerElement.getAsJsonObject();
                    // and if the intermediate JSON tree is a JSON object, iterate over each its property
                    for (final String innerFlatProperty : propertiesToFlatten) {
                        final JsonElement innerElement = outerObject.get(innerFlatProperty);
                        outerObject.remove(innerFlatProperty);

                        if (innerElement == null || !innerElement.isJsonObject()) {
                            continue;
                        }
                        // do the flattening here
                        final JsonObject innerObject = innerElement.getAsJsonObject();
                        Iterator<Map.Entry<String, JsonElement>> iterator = innerObject.entrySet().iterator();
                        switch (innerObject.size()) {
                            case 0:
                                // do nothing obviously
                                break;
                            default:
                                // graft each inner property to the outer object
                                while (iterator.hasNext()) {
                                    Map.Entry<String, JsonElement> entry=    iterator.next();
                                    if (outerObject.has(entry.getKey())){
                                        throw new RuntimeException("outclass "+value.getClass().getSimpleName()+" already has key, conflict key="+entry.getKey()+ ", propertyName="+innerFlatProperty);
                                    }
                                    outerObject.add(entry.getKey(), entry.getValue());
                                    iterator.remove();
                                }
                                break;
                        }
                        // detach the object to be flattened because we grafter the result to upper level already
                    }
                }
                // write the result
                TypeAdapters.JSON_ELEMENT.write(out, outerElement);
            }

            @Override
            public T read(final JsonReader jsonReader) {
                System.out.println(jsonReader.toString());
                throw new UnsupportedOperationException();
            }
        }
                .nullSafe();
    }
}
