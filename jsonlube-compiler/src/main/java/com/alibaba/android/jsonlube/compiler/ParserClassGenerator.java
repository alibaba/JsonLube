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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import static com.alibaba.android.jsonlube.compiler.TypesUtils.ANDROID_JSON_ARRAY;
import static com.alibaba.android.jsonlube.compiler.TypesUtils.ANDROID_JSON_OBJECT;

/**
 * 自动生成反序列化代码
 */
public class ParserClassGenerator extends AbstractGenerator {

    @Override
    protected String generateClassName(TypeElement type) {
        Name name = type.getSimpleName();
        return name.toString() + "_JsonLubeParser";
    }

    @Override
    protected MethodSpec generateMethod(TypeElement clazz) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("parse")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(ClassName.get(clazz))
            .addParameter(ANDROID_JSON_OBJECT, "data")
            .beginControlFlow("if (data == null)")
            .addStatement("return null")
            .endControlFlow()
            .addStatement("$T bean = new $T()", clazz, clazz);

        generateFromField(clazz, builder);
        generateFromSetter(clazz, builder);

        builder.addStatement("return bean");
        return builder.build();
    }

    private void generateFromField(TypeElement clazz, MethodSpec.Builder builder) {
        List<VariableElement> fieldList = ElementUtils.getAllFieldsIn(clazz, mElementUtils);
        for (VariableElement field : fieldList) {

            if (invalidField(field)) {
                continue;
            }

            String fieldName = getFieldName(field);
            String jsonName = getJsonName(field);
            TypeMirror type = field.asType();

            generateMethodBody(builder, type, fieldName, jsonName, false, null, null);
        }
    }

    private void generateFromSetter(TypeElement clazz, MethodSpec.Builder builder) {
        List<ExecutableElement> methodList = ElementUtils.getAllMethodsIn(clazz, mElementUtils);

        for (ExecutableElement method : methodList) {
            if (!isValidSetter(clazz, method)) {
                continue;
            }

            String fieldName = getNameFromGetterSetter(method);
            String jsonName = getJsonName(method);
            List<? extends VariableElement> params = method.getParameters();
            VariableElement field = params.get(0);
            TypeMirror type = field.asType();
            ExecutableElement getter = ElementUtils.findGetter(methodList, fieldName, type, mTypeUtils);

            if (getter == null) {
                continue;
            }

            System.out.println("Valid setter: " + method.getSimpleName());
            generateMethodBody(builder, type, fieldName, jsonName, true, method, getter);
        }
    }

    private void generateMethodBody(MethodSpec.Builder builder, TypeMirror type, String fieldName, String jsonName,
                                    boolean isGetterSetter, ExecutableElement setter, ExecutableElement getter) {
        if (isPrimaryOrString(type)) {
            tryAddPrimaryAndStringStatement(builder, type, fieldName, jsonName, isGetterSetter, setter, getter);
        } else if (TypesUtils.isHashMapType(mTypeUtils, type)){
            TypeMirror genericType = TypesUtils.getCollectionParameterizedType(type);
            TypeElement element = (TypeElement) mTypeUtils.asElement(genericType);
            ClassName className = ClassName.get(element);

            builder.addStatement("$T $LAndroidJson = data.optJSONObject($S)", ANDROID_JSON_OBJECT, fieldName, jsonName);
            builder.beginControlFlow("if ($LAndroidJson != null)", fieldName);
            builder.addStatement("java.util.Iterator<String> keys = $LAndroidJson.keys();", fieldName);

            builder.addStatement("$T<String, $T> $LMap = new $T<String, $T>()", HashMap.class, className, fieldName, HashMap.class, className);
            builder.beginControlFlow("while (keys.hasNext())");
            builder.addStatement("String key = keys.next();");

            if (isPrimary(genericType) || isString(genericType)) {
                tryGetPrimaryAndStringStatement(builder, genericType, fieldName);
            } else {
                ClassName parseClass = generateClass(element);
                builder.addStatement("$T item = $T.parse($LAndroidJson.optJSONObject(key))", element, parseClass, fieldName);
            }
            builder.addStatement("$LMap.put(key, item)", fieldName);
            builder.endControlFlow();
            setFieldValue(builder, fieldName, isGetterSetter, setter, "Map");
            builder.endControlFlow();
        } else if (TypesUtils.isListType(mTypeUtils, type)) {
            TypeMirror genericType = TypesUtils.getCollectionParameterizedType(type);
            TypeElement element = (TypeElement) mTypeUtils.asElement(genericType);
            ClassName className = ClassName.get(element);

            builder.addStatement("$T $LJsonArray = data.optJSONArray($S)", ANDROID_JSON_ARRAY, fieldName, jsonName);
            builder.beginControlFlow("if ($LJsonArray != null)", fieldName);
            builder.addStatement("int len = $LJsonArray.length()", fieldName);
            builder.addStatement("$T<$T> $LList = new $T<$T>(len)", ArrayList.class, className, fieldName, ArrayList.class, className);
            builder.beginControlFlow("for (int i = 0; i < len; i ++)");

            if (isPrimary(genericType) || isString(genericType)) {
                tryGetPrimaryAndStringFromArray(builder, genericType, fieldName);
            } else {
                ClassName parseClass = generateClass(element);
                builder.addStatement("$T item = $T.parse($LJsonArray.optJSONObject(i))", element, parseClass, fieldName);
            }
            builder.addStatement("$LList.add(item)", fieldName);
            builder.endControlFlow();
            setFieldValue(builder, fieldName, isGetterSetter, setter, "List");
            builder.endControlFlow();
        } else if (TypesUtils.isArrayType(type)) {
            ArrayType arrayType = (ArrayType) type;
            TypeMirror componentType = arrayType.getComponentType();
            ClassName className = ClassName.get((TypeElement) mTypeUtils.asElement(componentType));

            builder.addStatement("$T $LJsonArray = data.optJSONArray($S)", ANDROID_JSON_ARRAY, fieldName, jsonName);
            builder.beginControlFlow("if ($LJsonArray != null)", fieldName);
            builder.addStatement("int len = $LJsonArray.length()", fieldName);
            builder.addStatement("$T[] $LArray = new $T[len]", className, fieldName, className);
            builder.beginControlFlow("for (int i = 0; i < len; i ++)");

            if (isPrimary(componentType) || isString(componentType)) {
                tryGetPrimaryAndStringFromArray(builder, componentType, fieldName);
            } else {
                ClassName parseClass = generateClass((TypeElement) mTypeUtils.asElement(componentType));
                builder.addStatement("$T item = $T.parse($LJsonArray.optJSONObject(i))", className, parseClass, fieldName);
            }
            builder.addStatement("$LArray[i] = item", fieldName);
            builder.endControlFlow();
            setFieldValue(builder, fieldName, isGetterSetter, setter, "Array");
            builder.endControlFlow();

        } else if (isAndroidJsonObject(type)) {
            addObjectStatement(builder, fieldName, jsonName, "JSONObject", isGetterSetter, setter, getter);
        } else if (isFastJsonObject(type)) {
            builder.addStatement("$T $LAndroidJson = data.optJSONObject($S)", ANDROID_JSON_OBJECT, fieldName, jsonName);
            builder.beginControlFlow("if ($LAndroidJson != null)", fieldName);
            builder.addStatement("$T $LStr = $LAndroidJson.toString()", String.class, fieldName, fieldName);
            builder.addStatement("$T $LFastJson = $T.parseObject($LStr)", com.alibaba.fastjson.JSONObject.class, fieldName, com.alibaba.fastjson.JSON.class,
                fieldName);
            setFieldValue(builder, fieldName, isGetterSetter, setter, "FastJson");
            builder.endControlFlow();
        } else {
            ClassName parseClass = generateClass((TypeElement) mTypeUtils.asElement(type));
            if (!isGetterSetter) {
                builder.addStatement("bean.$L = $T.parse(data.optJSONObject($S))", fieldName, parseClass, jsonName);
            } else {
                builder.addStatement("bean.$L($T.parse(data.optJSONObject($S)))", setter.getSimpleName().toString(), parseClass, jsonName);
            }
        }
    }

    private void setFieldValue(MethodSpec.Builder builder, String fieldName, boolean isGetterSetter, ExecutableElement setter, String suffix) {
        if (!isGetterSetter) {
            builder.addStatement("bean.$L = $L" + suffix, fieldName, fieldName);
        } else {
            builder.addStatement("bean.$L($L)", setter.getSimpleName().toString(), fieldName + suffix);
        }
    }

    private boolean isAndroidJsonObject(TypeMirror typeMirror) {
        return TypesUtils.isAndroidJsonObject(typeMirror, mElementUtils, mTypeUtils);
    }

    private boolean isFastJsonObject(TypeMirror typeMirror) {
        return TypesUtils.isFastJsonObject(typeMirror, mElementUtils, mTypeUtils);
    }

    private boolean isPrimaryOrString(TypeMirror typeMirror) {
        TypeKind kind = typeMirror.getKind();
        return kind == TypeKind.INT || TypesUtils.isString(typeMirror) || kind == TypeKind.LONG || kind == TypeKind.DOUBLE
            || kind == TypeKind.FLOAT || kind == TypeKind.BYTE || TypesUtils.isBooleanType(typeMirror);
    }

    private boolean tryAddPrimaryAndStringStatement(MethodSpec.Builder builder, TypeMirror fieldClass, String fieldName, String jsonName,
                                                    boolean isGetterSetter, ExecutableElement setter, ExecutableElement getter) {
        if (fieldClass.getKind() == TypeKind.INT) {
            addPrimaryStatement(builder, fieldName, jsonName, "Int", isGetterSetter, setter, getter);
        } else if (TypesUtils.isString(fieldClass)) {
            addPrimaryStatement(builder, fieldName, jsonName, "String", isGetterSetter, setter, getter);
        } else if (fieldClass.getKind() == TypeKind.LONG) {
            addPrimaryStatement(builder, fieldName, jsonName, "Long", isGetterSetter, setter, getter);
        } else if (fieldClass.getKind() == TypeKind.DOUBLE) {
            addPrimaryStatement(builder, fieldName, jsonName, "Double", isGetterSetter, setter, getter);
        } else if (fieldClass.getKind() == TypeKind.FLOAT) {
            addPrimaryStatementWithCast(builder, float.class, fieldName, jsonName, "Double", isGetterSetter, setter, getter);
        } else if (fieldClass.getKind() == TypeKind.BYTE) {
            addPrimaryStatementWithCast(builder, float.class, fieldName, jsonName, "Int", isGetterSetter, setter, getter);
        } else if (TypesUtils.isBooleanType(fieldClass)) {
            addPrimaryStatement(builder, fieldName, jsonName, "Boolean", isGetterSetter, setter, getter);
        } else {
            return false;
        }

        return true;
    }

    private boolean tryGetPrimaryAndStringStatement(MethodSpec.Builder builder, TypeMirror fieldClass, String fieldName) {
        if (fieldClass.getKind() == TypeKind.INT) {
            getPrimary(builder, int.class, fieldName, "Int");
        } else if (TypesUtils.isString(fieldClass)) {
            getPrimary(builder, String.class, fieldName, "String");
        } else if (fieldClass.getKind() == TypeKind.LONG) {
            getPrimary(builder, long.class, fieldName, "Long");
        } else if (fieldClass.getKind() == TypeKind.DOUBLE) {
            getPrimary(builder, double.class, fieldName, "Double");
        } else if (fieldClass.getKind() == TypeKind.FLOAT) {
            getPrimary(builder, float.class, fieldName, "Double");
        } else if (fieldClass.getKind() == TypeKind.BYTE) {
            getPrimary(builder, float.class, fieldName, "Int");
        } else if (TypesUtils.isBooleanType(fieldClass)) {
            getPrimary(builder, boolean.class, fieldName, "Boolean");
        } else {
            return false;
        }

        return true;
    }

    private boolean tryGetPrimaryAndStringFromArray(MethodSpec.Builder builder, TypeMirror fieldClass, String fieldName) {
        if (fieldClass.getKind() == TypeKind.INT) {
            getPrimaryFromArray(builder, int.class, fieldName, "Int");
        } else if (TypesUtils.isString(fieldClass)) {
            getPrimaryFromArray(builder, String.class, fieldName, "String");
        } else if (fieldClass.getKind() == TypeKind.LONG) {
            getPrimaryFromArray(builder, long.class, fieldName, "Long");
        } else if (fieldClass.getKind() == TypeKind.DOUBLE) {
            getPrimaryFromArray(builder, double.class, fieldName, "Double");
        } else if (fieldClass.getKind() == TypeKind.FLOAT) {
            getPrimaryFromArray(builder, float.class, fieldName, "Double");
        } else if (TypesUtils.isBooleanType(fieldClass)) {
            getPrimaryFromArray(builder, boolean.class, fieldName, "Boolean");
        } else {
            return false;
        }

        return true;
    }

    private void getPrimaryFromArray(MethodSpec.Builder builder, Class<?> type, String fieldName, String typeName) {
        builder.addStatement("$T item = $LJsonArray.opt$L(i)", type, fieldName, typeName);
    }

    private void getPrimary(MethodSpec.Builder builder, Class<?> type, String fieldName, String typeName) {
        builder.addStatement("$T item = $LAndroidJson.opt$L(key)", type, fieldName, typeName);
    }

    private void addPrimaryStatement(MethodSpec.Builder builder, String fieldName, String jsonName, String typeName, boolean isGetterSetter,
                                     ExecutableElement setter, ExecutableElement getter) {
        if (!isGetterSetter) {
            builder.addStatement("bean.$L = data.opt$L($S, bean.$L)", fieldName, typeName, jsonName, fieldName);
        } else {
            if (getter != null) {
                builder.addStatement("bean.$L(data.opt$L($S, bean.$L()))", setter.getSimpleName(), typeName, jsonName, getter.getSimpleName().toString());
            } else {
                builder.addStatement("bean.$L(data.opt$L($S))", setter.getSimpleName(), typeName, jsonName);
            }
        }
    }

    private void addObjectStatement(MethodSpec.Builder builder, String fieldName, String jsonName, String typeName, boolean isGetterSetter,
                                    ExecutableElement setter, ExecutableElement getter) {
        if (!isGetterSetter) {
            builder.addStatement("bean.$L = data.opt$L($S)", fieldName, typeName, jsonName);
        } else {
            if (getter != null) {
                builder.addStatement("bean.$L(data.opt$L($S))", setter.getSimpleName(), typeName, jsonName);
            } else {
                builder.addStatement("bean.$L(data.opt$L($S))", setter.getSimpleName(), typeName, jsonName);
            }
        }
    }

    private void addPrimaryStatementWithCast(MethodSpec.Builder builder, Class<?> castType, String fieldName, String jsonName, String typeName,
                                             boolean isGetterSetter, ExecutableElement setter, ExecutableElement getter) {
        if (!isGetterSetter) {
            builder.addStatement("bean.$L = ($T) data.opt$L($S, bean.$L)", fieldName, castType, typeName, jsonName, fieldName);
        } else {
            if (getter != null) {
                builder.addStatement("bean.$L(($T) data.opt$L($S, bean.$L()))", setter.getSimpleName().toString(), castType, typeName, jsonName,
                    getter.getSimpleName().toString());
            } else {
                builder.addStatement("bean.$L(($T) data.opt$L($S))", setter.getSimpleName().toString(), castType, typeName, jsonName);
            }
        }
    }

    private boolean isPrimary(TypeMirror type) {
        return TypesUtils.isPrimitive(type);
    }

    private boolean isString(TypeMirror type) {
        return TypesUtils.isString(type);
    }

}
