/*
 * Copyright 2018 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.android.jsonlube.compiler;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * A utility class that helps with {@link TypeMirror}s.
 *
 * Created by shangjie on 2018/1/31.
 */
public final class TypesUtils {

    public enum CollectionType {
        NOT_A_COLLECTION,
        LIST,
        ARRAYLIST,
        HASHMAP,
        QUEUE,
        SET,
    }

    private static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
    private static final String JAVA_LANG_INTEGER = "java.lang.Integer";
    private static final String JAVA_LANG_LONG = "java.lang.Long";
    private static final String JAVA_LANG_FLOAT = "java.lang.Float";
    private static final String JAVA_LANG_DOUBLE = "java.lang.Double";
    private static final String JAVA_UTIL_LIST = "java.util.List<?>";
    private static final String JAVA_UTIL_LIST_UNTYPED = "java.util.List";
    private static final String JAVA_UTIL_ARRAYLIST = "java.util.ArrayList<?>";
    private static final String JAVA_UTIL_ARRAYLIST_UNTYPED = "java.util.ArrayList";
    private static final String JAVA_UTIL_HASHMAP = "java.util.HashMap<?,?>";
    private static final String JAVA_UTIL_MAP = "java.util.Map<?,?>";
    private static final String JAVA_UTIL_HASHMAP_UNTYPED = "java.util.HashMap";
    private static final String JAVA_UTIL_QUEUE = "java.util.Queue<?>";
    private static final String JAVA_UTIL_QUEUE_UNTYPED = "java.util.Queue";
    private static final String JAVA_UTIL_SET = "java.util.Set<?>";
    private static final String JAVA_UTIL_SET_UNTYPED = "java.util.Set";
    private static final String JAVA_LANG_ENUM = "java.lang.Enum<?>";
    public static final ClassName ANDROID_JSON_OBJECT = ClassName.get("org.json", "JSONObject");
    public static final ClassName ANDROID_JSON_ARRAY = ClassName.get("org.json", "JSONArray");
    public static final ClassName ANDROID_JSON_EXCEPTION = ClassName.get("org.json", "JSONException");
    public static final ClassName FASTJSON_OBJECT = ClassName.get("com.alibaba.fastjson", "JSONObject");
    public static final ClassName FASTJSON_ARRAY = ClassName.get("com.alibaba.fastjson", "JSONArray");


    // Class cannot be instantiated
    private TypesUtils() {
        throw new AssertionError("Class TypesUtils cannot be instantiated.");
    }

    public static boolean isMapType(CollectionType type) {
        return type == CollectionType.HASHMAP;
    }

    public static boolean isListType(Types typeUtil, TypeMirror typeMirror) {
        CollectionType type = getCollectionType(typeUtil, typeMirror);
        return type == CollectionType.ARRAYLIST || type == CollectionType.LIST;
    }

    public static boolean isArrayType(TypeMirror typeMirror) {
        return TypeKind.ARRAY.equals(typeMirror.getKind());
    }

    public static boolean isHashMapType(Types typeUtil, TypeMirror typeMirror) {
        CollectionType type = getCollectionType(typeUtil, typeMirror);
        return type == CollectionType.HASHMAP;
    }

    public static CollectionType getCollectionType(Types typeUtil, TypeMirror typeMirror) {
        String erasedType = typeUtil.erasure(typeMirror).toString();
        if (JAVA_UTIL_LIST_UNTYPED.equals(erasedType)) {
            return CollectionType.LIST;
        } else if (JAVA_UTIL_ARRAYLIST_UNTYPED.equals(erasedType)) {
            return CollectionType.ARRAYLIST;
        } else if (JAVA_UTIL_HASHMAP_UNTYPED.equals(erasedType)) {
            return CollectionType.HASHMAP;
        } else if (JAVA_UTIL_QUEUE_UNTYPED.equals(erasedType)) {
            return CollectionType.QUEUE;
        } else if (JAVA_UTIL_SET_UNTYPED.equals(erasedType)) {
            return CollectionType.SET;
        }
        return CollectionType.NOT_A_COLLECTION;
    }

    /**
     * If {@code typeMirror} represents a list type ({@link java.util.List}), attempt to divine the
     * type of the contents.
     * <p>
     * Returns null if {@code typeMirror} does not represent a list type or if we cannot divine the
     * type of the contents.
     */
    public static TypeMirror getCollectionParameterizedType(TypeMirror typeMirror) {
        if (!(typeMirror instanceof DeclaredType)) {
            return null;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return null;
        }
        TypeElement typeElement = (TypeElement) element;
        List<? extends TypeParameterElement> typeParameterElements = typeElement.getTypeParameters();
        List<TypeMirror> typeArguments = (List<TypeMirror>) declaredType.getTypeArguments();

        if (JAVA_UTIL_QUEUE.equals(getCanonicalTypeName(declaredType)) ||
                JAVA_UTIL_LIST.equals(getCanonicalTypeName(declaredType)) ||
                JAVA_UTIL_ARRAYLIST.equals(getCanonicalTypeName(declaredType)) ||
                JAVA_UTIL_SET.equals(getCanonicalTypeName(declaredType))) {
            // sanity check.
            if (typeParameterElements.size() != 1) {
                throw new IllegalStateException(
                        String.format("%s is not expected generic type", declaredType));
            }
            return typeArguments.get(0);
        } else if (JAVA_UTIL_HASHMAP.equals(getCanonicalTypeName(declaredType))) {
            // sanity check.
            if (typeParameterElements.size() != 2) {
                throw new IllegalStateException(
                        String.format("%s is not expected generic type", declaredType));
            }
            TypeMirror keyType = typeArguments.get(0);
            TypeMirror valueType = typeArguments.get(1);
            if (!JAVA_LANG_STRING.equals(keyType.toString())) {
                throw new IllegalStateException("Only String keys are supported for map types");
            }
            return valueType;
        }
        return null;
    }

    /**
     * Returns a string with type parameters replaced with wildcards.  This is slightly different from
     * {@link Types#erasure(javax.lang.model.type.TypeMirror)}, which removes all type parameter data.
     * <p>
     * For instance, if there is a field with type List&lt;String&gt;, this returns a string
     * List&lt;?&gt;.
     */
    private static String getCanonicalTypeName(DeclaredType declaredType) {
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (!typeArguments.isEmpty()) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');

            return typeString.toString();
        } else {
            return declaredType.toString();
        }
    }


    /**
     * Gets the fully qualified name for a provided type. It returns an empty name if type is an
     * anonymous type.
     *
     * @param type the declared type
     * @return the name corresponding to that type
     */
    public static Name getQualifiedName(DeclaredType type) {
        TypeElement element = (TypeElement) type.asElement();
        return element.getQualifiedName();
    }

    /**
     * Checks if the type represents a java.lang.String declared type. TODO: it would be cleaner to
     * use String.class.getCanonicalName(), but the two existing methods above don't do that, I
     * guess for performance reasons.
     *
     * @param type the type
     * @return true iff type represents java.lang.String
     */
    public static boolean isString(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.String");
    }

    /**
     * Checks if the type represents a boolean type, that is either boolean (primitive type) or
     * java.lang.Boolean.
     *
     * @param type the type to test
     * @return true iff type represents a boolean type
     */
    public static boolean isBooleanType(TypeMirror type) {
        return isDeclaredOfName(type, "java.lang.Boolean")
                || type.getKind().equals(TypeKind.BOOLEAN);
    }

    /**
     * Check if the type represents a declared type of the given qualified name.
     *
     * @param type the type
     * @return type iff type represents a declared type of the qualified name
     */
    public static boolean isDeclaredOfName(TypeMirror type, CharSequence qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
                && getQualifiedName((DeclaredType) type).contentEquals(qualifiedName);
    }


    /**
     * Returns true iff the argument is a primitive type.
     *
     * @return whether the argument is a primitive type
     */
    public static boolean isPrimitive(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否是Android原生的JsonObject对象
     */
    public static boolean isAndroidJsonObject(TypeMirror typeMirror, Elements elements, Types types) {
        Element element = elements.getTypeElement(ANDROID_JSON_OBJECT.reflectionName());
        TypeMirror androidJsonObjectType = element.asType();
        return types.isAssignable(typeMirror, androidJsonObjectType);
    }

    /**
     * 是否是Android原生的JsonArray对象
     */
    public static boolean isAndroidJsonArray(TypeMirror typeMirror, Elements elements, Types types) {
        Element element = elements.getTypeElement(ANDROID_JSON_ARRAY.reflectionName());
        TypeMirror androidJsonObjectType = element.asType();
        return types.isAssignable(typeMirror, androidJsonObjectType);
    }

    /**
     * 是否是FastJson的JsonObject对象
     */
    public static boolean isFastJsonObject(TypeMirror typeMirror, Elements elements, Types types) {
        Element element = elements.getTypeElement(FASTJSON_OBJECT.reflectionName());
        if (element == null) {
            return false;
        }
        TypeMirror androidJsonObjectType = element.asType();
        return types.isAssignable(typeMirror, androidJsonObjectType);
    }

    /**
     * 是否是FastJson的JsonArray对象
     * @param typeMirror
     * @param elements
     * @param types
     * @return
     */
    public static boolean isFastJsonArray(TypeMirror typeMirror, Elements elements, Types types) {
        Element element = elements.getTypeElement(FASTJSON_ARRAY.reflectionName());
        if (element == null) {
            return false;
        }
        TypeMirror androidJsonObjectType = element.asType();
        return types.isAssignable(typeMirror, androidJsonObjectType);
    }



}