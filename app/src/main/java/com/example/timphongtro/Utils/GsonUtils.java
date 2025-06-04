package com.example.timphongtro.Utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class GsonUtils {
    private static final Gson gson = new Gson();

    /**
     * Convert object to JSON string
     * @param object Object to serialize
     * @return JSON string or null if error
     */
    public static String toJson(Object object) {
        try {
            if (object == null) return null;
            return gson.toJson(object);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert JSON string to object
     * @param json JSON string
     * @param classType Class type to deserialize
     * @return Object or null if error
     */
    public static <T> T fromJson(String json, Class<T> classType) {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            return gson.fromJson(json, classType);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Convert JSON string to object with TypeToken
     * @param json JSON string
     * @param typeToken Type token for complex types
     * @return Object or null if error
     */
    public static <T> T fromJson(String json, Type typeToken) {
        try {
            if (json == null || json.trim().isEmpty()) return null;
            return gson.fromJson(json, typeToken);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if JSON string is valid
     * @param json JSON string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidJson(String json) {
        try {
            if (json == null) return false;
            gson.fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * Create TypeToken for generic types
     * Example: new TypeToken<List<String>>(){}.getType()
     */
    public static <T> Type getListType(Class<T> clazz) {
        return TypeToken.getParameterized(java.util.List.class, clazz).getType();
    }
}