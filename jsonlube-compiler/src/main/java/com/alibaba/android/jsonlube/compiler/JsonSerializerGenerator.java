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
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

import java.util.List;

import static com.alibaba.android.jsonlube.compiler.TypesUtils.ANDROID_JSON_OBJECT;
import static com.alibaba.android.jsonlube.compiler.TypesUtils.ANDROID_JSON_ARRAY;
import static com.alibaba.android.jsonlube.compiler.TypesUtils.ANDROID_JSON_EXCEPTION;

/**
 * 自动生成序列化代码
 */
public class JsonSerializerGenerator extends AbstractGenerator {


    @Override
    protected String generateClassName(TypeElement type) {
        Name name = type.getSimpleName();
        return name.toString() + "_JsonLubeSerializer";
    }

    @Override
    protected MethodSpec generateMethod(TypeElement type) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ANDROID_JSON_OBJECT)
                .addParameter(ClassName.get(type), "bean")
                .beginControlFlow("if (bean == null)")
                .addStatement("return null")
                .endControlFlow()
                .addException(ANDROID_JSON_EXCEPTION)
                .addStatement("$T data = new $T()", ANDROID_JSON_OBJECT, ANDROID_JSON_OBJECT);

        generateFromField(type, builder);
        generateFromGetter(type, builder);

        builder.addStatement("return data");
        return builder.build();
    }

    private void generateFromField(TypeElement type, MethodSpec.Builder builder) {
        List<VariableElement> fieldList = ElementUtils.getAllFieldsIn(type, mElementUtils);

        for (VariableElement field : fieldList) {
            if (invalidField(field)) {
                continue;
            }

            String fieldName = getFieldName(field);
            String jsonName = getJsonName(field);
            TypeMirror fieldType = field.asType();

            generateMethodBody(builder, fieldType, fieldName, jsonName, false, null);
        }
    }

    private void  generateFromGetter(TypeElement type, MethodSpec.Builder builder) {
        List<ExecutableElement> methodList = ElementUtils.getAllMethodsIn(type, mElementUtils);

        for (ExecutableElement method : methodList) {
            if (!isValidGetter(type, method)) {
                continue;
            }

            String fieldName = getNameFromGetterSetter(method);
            String jsonName = getJsonName(method);
            TypeMirror returnType = method.getReturnType();

            ExecutableElement setter = ElementUtils.findSetter(methodList, fieldName, returnType, mTypeUtils);
            if (setter == null) {
                continue;
            }

            System.out.println("Valid getter: " + method.getSimpleName());
            generateMethodBody(builder, returnType, fieldName, jsonName, true, method);
        }
    }

    private void generateMethodBody(MethodSpec.Builder builder, TypeMirror fieldType, String fieldName, String jsonName,
                                    boolean isGetterSetter, ExecutableElement getter){
        if (TypesUtils.isPrimitive(fieldType) || TypesUtils.isAndroidJsonObject(fieldType, mElementUtils, mTypeUtils)
                || TypesUtils.isAndroidJsonArray(fieldType, mElementUtils, mTypeUtils) || TypesUtils.isString(fieldType)) {
            if (!isGetterSetter) {
                builder.addStatement("data.put($S, bean.$L)", jsonName, fieldName);
            } else {
                builder.addStatement("data.put($S, bean.$L)", jsonName, getter);
            }
        } else if (TypesUtils.isHashMapType(mTypeUtils, fieldType)){
            TypeMirror genericType = TypesUtils.getCollectionParameterizedType(fieldType);
            TypeElement element = (TypeElement) mTypeUtils.asElement(genericType);
            ClassName className = ClassName.get(element);

            serializerHashMap(builder, fieldName, jsonName, isGetterSetter, getter, genericType, className);
        }else if (TypesUtils.isListType(mTypeUtils, fieldType)) {
            TypeMirror genericType = TypesUtils.getCollectionParameterizedType(fieldType);
            TypeElement element = (TypeElement) mTypeUtils.asElement(genericType);
            ClassName className = ClassName.get(element);

            serializerListAndArray(builder, fieldName, jsonName, isGetterSetter, getter, genericType, className);
        } else if (TypesUtils.isArrayType(fieldType)) {
            ArrayType arrayType = (ArrayType) fieldType;
            TypeMirror componentType = arrayType.getComponentType();
            ClassName className = ClassName.get((TypeElement) mTypeUtils.asElement(componentType));

            serializerListAndArray(builder, fieldName, jsonName, isGetterSetter, getter, componentType, className);
        } else if (TypesUtils.isFastJsonObject(fieldType, mElementUtils, mTypeUtils)) {
            if (!isGetterSetter) {
                builder.beginControlFlow("if (bean.$L != null)", fieldName);
                builder.addStatement("$T content = bean.$L.toString()", String.class, fieldName);

            } else {
                builder.beginControlFlow("if (bean.$L != null)", getter);
                builder.addStatement("$T content = bean.$L.toString()", String.class, getter);
            }

            builder.addStatement("$T $LAndroidJsonObject = new $T(content)", ANDROID_JSON_OBJECT, fieldName, ANDROID_JSON_OBJECT);
            builder.addStatement("data.put($S, $LAndroidJsonObject)", jsonName, fieldName);
            builder.endControlFlow();
        } else if (TypesUtils.isFastJsonArray(fieldType, mElementUtils, mTypeUtils)) {
            if (!isGetterSetter) {
                builder.beginControlFlow("if (bean.$L != null)", fieldName);
                builder.addStatement("$T content = bean.$L.toString()", String.class, fieldName);

            } else {
                builder.beginControlFlow("if (bean.$L != null)", getter);
                builder.addStatement("$T content = bean.$L.toString()", String.class, getter);
            }

            builder.addStatement("$T $LAndroidJsonArray = new $T(content)", ANDROID_JSON_ARRAY, fieldName, ANDROID_JSON_ARRAY);
            builder.addStatement("data.put($S, $LAndroidJsonArray)", jsonName, fieldName);
            builder.endControlFlow();
        } else {
            ClassName serializerClass = generateClass((TypeElement) mTypeUtils.asElement(fieldType));
            if (!isGetterSetter) {
                builder.addStatement("data.put($S, $T.serialize(bean.$L))", jsonName, serializerClass, fieldName);
            } else {
                builder.addStatement("data.put($S, $T.serialize(bean.$L))", jsonName, serializerClass, getter);
            }
        }
    }

    private void serializerHashMap(MethodSpec.Builder builder, String fieldName, String jsonName,
                                   boolean isGetterSetter, ExecutableElement getter, TypeMirror genericType, ClassName genericClassName) {
        if (!isGetterSetter) {
            builder.beginControlFlow("if (bean.$L != null)", fieldName);
            builder.addStatement("$T $LJsonObject = new $T()", ANDROID_JSON_OBJECT, fieldName, ANDROID_JSON_OBJECT);
            builder.beginControlFlow("for (java.util.Map.Entry<String, $T> entry : bean.$L.entrySet())", genericClassName, fieldName);
        } else {
            builder.beginControlFlow("if (bean.$L != null)", getter);
            builder.addStatement("$T $LJsonObject = new $T()", ANDROID_JSON_OBJECT, fieldName, ANDROID_JSON_OBJECT);
            builder.beginControlFlow("for (java.util.Map.Entry<String, $T> entry : bean.$L.entrySet())", genericClassName, getter);
        }

        if (TypesUtils.isPrimitive(genericType) || TypesUtils.isAndroidJsonObject(genericType, mElementUtils, mTypeUtils)
                || TypesUtils.isAndroidJsonArray(genericType, mElementUtils, mTypeUtils) || TypesUtils.isString(genericType)) {
            builder.addStatement("$LJsonObject.put(entry.getKey(), entry.getValue())", fieldName);
        } else if (TypesUtils.isFastJsonArray(genericType, mElementUtils, mTypeUtils) || TypesUtils.isFastJsonObject(genericType, mElementUtils, mTypeUtils)) {
            builder.beginControlFlow("if (entry.getValue() != null)");

            builder.addStatement("String content = entry.getValue().toString()");
            builder.addStatement("$T androidJsonObject = new $T(content)", ANDROID_JSON_OBJECT, ANDROID_JSON_OBJECT);
            builder.addStatement("$LJsonObject.put(entry.getKey(), androidJsonObject)", fieldName);

            builder.endControlFlow();
        } else if (TypesUtils.isArrayType(genericType) || TypesUtils.isListType(mTypeUtils, genericType)) {
            System.out.println("is array type --> " + genericClassName);
            // not support yet
        } else if (TypesUtils.isHashMapType(mTypeUtils, genericType)){
            System.out.println("is hashmap type --> " + genericClassName);
            //not support yet
        } else {
            builder.beginControlFlow("if (entry.getValue() != null)");
            System.out.println("Generic class in array --> " + genericClassName.simpleName());
            ClassName serializerClass = generateClass((TypeElement) mTypeUtils.asElement(genericType));

            builder.addStatement("$LJsonObject.put(entry.getKey(), $T.serialize(entry.getValue()))", fieldName, serializerClass);

            builder.endControlFlow();
        }

        builder.endControlFlow();
        builder.addStatement("data.put($S, $LJsonObject)", jsonName, fieldName);
        builder.endControlFlow();

    }
    private void serializerListAndArray(MethodSpec.Builder builder, String fieldName, String jsonName,
                                        boolean isGetterSetter, ExecutableElement getter, TypeMirror genericType,  ClassName genericClassName) {
        if (!isGetterSetter) {
            builder.beginControlFlow("if (bean.$L != null)", fieldName);
            builder.addStatement("$T $LJsonArray = new $T()", ANDROID_JSON_ARRAY, fieldName, ANDROID_JSON_ARRAY);
            builder.beginControlFlow("for ($T item : bean.$L)", genericClassName, fieldName);
        } else {
            builder.beginControlFlow("if (bean.$L != null)", getter);
            builder.addStatement("$T $LJsonArray = new $T()", ANDROID_JSON_ARRAY, fieldName, ANDROID_JSON_ARRAY);
            builder.beginControlFlow("for ($T item : bean.$L)", genericClassName, getter);
        }

        if (TypesUtils.isPrimitive(genericType) || TypesUtils.isAndroidJsonObject(genericType, mElementUtils, mTypeUtils)
                || TypesUtils.isAndroidJsonArray(genericType, mElementUtils, mTypeUtils) || TypesUtils.isString(genericType)) {
            builder.addStatement("$LJsonArray.put(item)", fieldName);
        } else if (TypesUtils.isFastJsonArray(genericType, mElementUtils, mTypeUtils) || TypesUtils.isFastJsonObject(genericType, mElementUtils, mTypeUtils)) {
            builder.beginControlFlow("if (item != null)");

            builder.addStatement("String content = item.toString()");
            builder.addStatement("$T androidJsonObject = new $T(content)", ANDROID_JSON_OBJECT, ANDROID_JSON_OBJECT);
            builder.addStatement("$LJsonArray.put(androidJsonObject)", fieldName);

            builder.endControlFlow();
        } else if (TypesUtils.isArrayType(genericType) || TypesUtils.isListType(mTypeUtils, genericType)) {
            System.out.println("is array type --> " + genericClassName);
            // not support yet
        } else if (TypesUtils.isHashMapType(mTypeUtils, genericType)){
            System.out.println("is hashmap type --> " + genericClassName);
            //not support yet
        } else {
            builder.beginControlFlow("if (item != null)");
            System.out.println("Generic class in array --> " + genericClassName.simpleName());
            ClassName serializerClass = generateClass((TypeElement) mTypeUtils.asElement(genericType));

            builder.addStatement("$LJsonArray.put($T.serialize(item))", fieldName, serializerClass);

            builder.endControlFlow();
        }

        builder.endControlFlow();
        builder.addStatement("data.put($S, $LJsonArray)", jsonName, fieldName);
        builder.endControlFlow();
    }
}
