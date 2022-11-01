package com.javedemo.gson.jsonAdapter;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleFlatSerializer {
//        implements JsonSerializer , JsonDeserializer {
    Logger logger = LogManager.getLogger(SimpleFlatSerializer.class);
    Gson gson;
//
//    @Override
//    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
//        if (src == null){
//            logger.info("src is null type="+typeOfSrc );
//            return null;
//        }
//
//        TypeAdapter<Object> adapter = getAdapterByReflect(gson not SimpleFlatSerializer,not ReflectiveTypeAdapterFactory.Adapter);
//
//        if(adapter!=null){
//
//            JsonElement jsonElement=   context.serialize(fieldValue,fieldType);
//           return jsonElement;
//
//
//        }
////        src.getClass();
//        JsonObject jsonObject=new JsonObject();
//        for (FlatAdater.BoundField allField : allFields) {
//            TypeAdapter<Object> adapter = getAdapterByReflect(gson not SimpleFlatSerializer,not Ref);
//            if(adapter!=null){
//                JsonElement jsonElement=   context.serialize(fieldValue,fieldType);
//                jsonObject.add(fieldName,jsonElement);
//            }
//            JsonObject jsonElement= serialize(fieldValue,fieldType,context);
//            for (Map.Entry<String, JsonElement> entry : jsonElement.entrySet()) {
//                jsonObject.add(entry.getKey(),entry.getValue());
//
//            }
//
//        }
////        JsonElement serialize = context.serialize(src, typeOfSrc);
////        serialize.
//        return jsonObject;
//    }
//
//    @Override
//    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//
//
//        try {
//            Class clazz = (Class) typeOfT;
//            Constructor<? extends Object> constructor = clazz.getConstructor();
//            Object object = constructor.newInstance();
//            // 把字段分层x段,分别属于当前object, 其中的某个field. 这个需要在前期就计算好. 这样性能好的.
//
//            Set<Map.Entry<String, JsonElement>> entries = ((JsonObject) json).entrySet();
//            Iterator<Map.Entry<String, JsonElement>> iterator = entries.iterator();
//            while (iterator.hasNext()){
//                Map.Entry<String, JsonElement>  entry= iterator.next();
//                Field field = FieldUtils.getField(clazz, entry.getKey(), true);
//
//                Class<Object> declaringClass = (Class<Object>) field.getDeclaringClass();
//                Class<Object> type = (Class<Object>) field.getType();
//
//                TypeAdapter<Object> adapter = gson.getAdapter(declaringClass);
//
//                Field typeTokenCacheField = FieldUtils.getField(clazz, "typeTokenCache", true);
//                Map<TypeToken<?>, TypeAdapter<?>> typeTokenCache = (Map<TypeToken<?>, TypeAdapter<?>>) ReflectionUtil.getFieldValue(typeTokenCacheField, gson);
//                TypeAdapter<?> typeAdapter = typeTokenCache.get(field.getDeclaringClass());
//
//
//
//                if (adapter!=null){
//                    Object fieldValue = context.deserialize(entry.getValue(), field.getType());
//                    ReflectionUti.setFieldValue(field, object,fieldValue);
//                    iterator.remove();
//                    continue;
//                }
//
//            }
//
//            //对剩余的字段进行反序列化.
//             iterator = entries.iterator();
//            while (iterator.hasNext()){
//                Map.Entry<String, JsonElement>  entry= iterator.next();
//                Field field = FieldUtils.getField(clazz, entry.getKey(), true);
//                // 剩余的字段.进一步迭代.
//                deserialize(json,field.getType(),context);
//            }
//        } catch (Exception e) {
//           throw new RuntimeException(e);
//        }
//
//
//
//        return null;
//    }
//
//
//
//
//

}
