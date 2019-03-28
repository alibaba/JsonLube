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

import com.alibaba.android.jsonlube.JsonLubeField;
import com.alibaba.android.jsonlube.ProguardKeep;
import com.alibaba.fastjson.annotation.JSONField;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.Serializable;
import java.util.List;

public abstract class AbstractGenerator {
    protected ClassFileWriter mClassFileWriter;
    protected Elements mElementUtils;
    protected Types mTypeUtils;

    /**
     * 初始化
     * @param writer        负责写Class文件
     * @param elements      操作Element对象的工具类
     * @param typeUtils     操作Type对象的工具类
     */
    public void init(ClassFileWriter writer, Elements elements, Types typeUtils) {
        mElementUtils = elements;
        mTypeUtils = typeUtils;
        mClassFileWriter = writer;
    }

    /**
     * 根据Type生成Class基本属性
     * @param type
     * @return
     */
    public ClassName generateClass(TypeElement type) {
        String name = generateClassName(type);
        String packageName = ElementUtils.enclosingPackage(type).toString();
        ClassName className = ClassName.get(packageName, name);

        if (mClassFileWriter.isExist(className.toString())) {
            return className;
        }

        TypeSpec parseClass = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(ProguardKeep.class)
                .addSuperinterface(Serializable.class)
                .addMethod(generateMethod(type))
                .build();

        mClassFileWriter.append(parseClass, className);
        return className;
    }

    /**
     * 生成Class的类名
     */
    protected abstract String generateClassName(TypeElement type);

    /**
     * 生成Method
     * @param clazz
     * @return
     */
    protected abstract MethodSpec generateMethod(TypeElement clazz);

    /**
     * 判断无效的成员变量，包括：final修饰的成员，static成员，以及非public成员
     */
    protected boolean invalidField(VariableElement field) {

        if (ElementUtils.isFinal(field) || ElementUtils.isStatic(field) || !ElementUtils.isPublic(field)) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否有效的setter函数
     */
    protected boolean isValidSetter(TypeElement type, ExecutableElement method) {
        if (!isValidGetterSetterName(method, "set")) {
            return false;
        }

        List<? extends VariableElement> params = method.getParameters();
        if (params == null || params.isEmpty() || params.size() > 1) {
            return false;
        }

        String name = getNameFromGetterSetter(method);
        VariableElement field = ElementUtils.findFieldIn(type, name, mElementUtils);
        if (field != null && ElementUtils.isPublic(field)) {
            return false;
        }

        return true;
    }

    /**
     * 判断是否有效的getter函数
     */
    protected boolean isValidGetter(TypeElement type, ExecutableElement method) {
        if (!isValidGetterSetterName(method, "get")) {
            return false;
        }

        List<? extends VariableElement> elements = method.getParameters();
        if (elements != null && !elements.isEmpty()) {
            return false;
        }

        String name = getNameFromGetterSetter(method);
        VariableElement field = ElementUtils.findFieldIn(type, name, mElementUtils);
        if (field != null && ElementUtils.isPublic(field)) {
            return false;
        }

        TypeMirror returnType = method.getReturnType();

        return !isInvalidKind(returnType.getKind());
    }

    private boolean isInvalidKind(TypeKind kind) {
        return kind == TypeKind.VOID || kind == TypeKind.ERROR || kind == TypeKind.NONE || kind == TypeKind.NULL;
    }

    private boolean isValidGetterSetterName(ExecutableElement method, String getterOrSetter) {
        String name = method.getSimpleName().toString();

        if (StringUtil.isEmpty(name) || name.length() <= 3 || !name.startsWith(getterOrSetter)) {
            return false;
        }

        char c = name.charAt(3);
        return Character.isUpperCase(c);
    }

    /**
     * 获取成员名称
     * @param field
     * @return
     */
    protected String getFieldName(VariableElement field) {
        return field.getSimpleName().toString();
    }

    /**
     * 根据注解的name定义生成Json属性名。
     * @param field
     * @return
     */
    protected String getJsonName(VariableElement field) {
        String jsonNameFromAnnotation = ElementUtils.getFieldAnnotationValue(field, JSONField.class, "name");
        if (!StringUtil.isEmpty(jsonNameFromAnnotation)) {
            return jsonNameFromAnnotation;
        }

        jsonNameFromAnnotation = ElementUtils.getFieldAnnotationValue(field, JsonLubeField.class, "name");
        if (!StringUtil.isEmpty(jsonNameFromAnnotation)) {
            return jsonNameFromAnnotation;
        }

        return field.getSimpleName().toString();
    }

    /**
     * 根据getter和setter函数生成Json属性名
     * @param method
     * @return
     */
    protected String getNameFromGetterSetter(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        methodName = getNameFromGetterSetter(methodName);
        return methodName.substring(0);
    }

    /**
     * 生成json属性名
     * @param method
     * @return
     */
    protected String getJsonName(ExecutableElement method) {
        String jsonNameFromAnnotation = ElementUtils.getFieldAnnotationValue(method, JSONField.class, "name");
        if (!StringUtil.isEmpty(jsonNameFromAnnotation)) {
            return jsonNameFromAnnotation;
        }

        jsonNameFromAnnotation = ElementUtils.getFieldAnnotationValue(method, JsonLubeField.class, "name");
        if (!StringUtil.isEmpty(jsonNameFromAnnotation)) {
            return jsonNameFromAnnotation;
        }

        return getNameFromGetterSetter(method);
    }

    private String getNameFromGetterSetter(String methodName) {
        String name = methodName.substring(3);
        return StringUtil.lowercaseFirstChar(name);
    }
}
