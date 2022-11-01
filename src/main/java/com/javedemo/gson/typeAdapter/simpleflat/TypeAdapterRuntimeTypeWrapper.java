package com.javedemo.gson.typeAdapter.simpleflat;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * A copy of {@code com.google.gson.internal.bind.TypeAdapterRuntimeTypeWrapper} class that is accessible from here.
 * @param <T>    type of object to read
 */
public class TypeAdapterRuntimeTypeWrapper<T> extends TypeAdapter<T> {
    private final Gson context;
    private final TypeAdapter<T> delegate;
    private final Type type;

    public TypeAdapterRuntimeTypeWrapper(Gson context, TypeAdapter<T> delegate, Type type) {
        this.context = context;
        this.delegate = delegate;
        this.type = type;
    }

    @Override
    public T read(JsonReader in) throws IOException {
        return delegate.read(in);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(JsonWriter out, T value) throws IOException {
        // Order of preference for choosing type adapters
        // 第一选择First preference: a type adapter registered for the runtime type
        // 第二选择Second preference: a type adapter registered for the declared type
        // 第三选择Third preference: reflective type adapter for the runtime type (if it is a sub class of the declared type)
        // 第四选择Fourth preference: reflective type adapter for the declared type

        TypeAdapter chosen = delegate;
        Type runtimeType = getRuntimeTypeIfMoreSpecific(type, value);

        // 运行期类型 和 静态类型不符合, 常见与接口或者定义父类,运行期子类赋值
        if (runtimeType != type) {
            TypeAdapter runtimeTypeAdapter = context.getAdapter(TypeToken.get(runtimeType));
            // 优先使用运行期的类去判断TypeAdapter
            if (!(runtimeTypeAdapter instanceof FlatReflectionTypeAdapterFactory.Adapter)) {
                // The user registered a type adapter for the runtime type, so we will use that
                chosen = runtimeTypeAdapter;
            } else if (!(delegate instanceof FlatReflectionTypeAdapterFactory.Adapter)) {
                // The user registered a type adapter for Base class, so we prefer it over the
                // reflective type adapter for the runtime type
                chosen = delegate;
            } else {
                // Use the type adapter for runtime type
                chosen = runtimeTypeAdapter;
            }
        }
        chosen.write(out, value);
    }

    /**
     * Finds a compatible runtime type if it is more specific
     */
    private Type getRuntimeTypeIfMoreSpecific(Type type, Object value) {
        boolean canBeMoreSpecific = type == Object.class || type instanceof TypeVariable<?> || type instanceof Class<?>;
        return value != null && canBeMoreSpecific ? value.getClass() : type;
    }
}
