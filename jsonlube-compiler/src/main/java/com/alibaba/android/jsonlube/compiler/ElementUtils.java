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


import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * A Utility class for analyzing {@code Element}s.
 *
 * Created by shangjie on 2018/1/31.
 */
public class ElementUtils {

    // Class cannot be instantiated.
    private ElementUtils() {
        throw new AssertionError("Class ElementUtils cannot be instantiated.");
    }


    /**
     * Returns the innermost package element enclosing the given element. The same effect as {@link
     * javax.lang.model.util.Elements#getPackageOf(Element)}. Returns the element itself if it is a
     * package.
     *
     * @param elem the enclosed element of a package
     * @return the innermost package element
     */
    public static PackageElement enclosingPackage(final Element elem) {
        Element result = elem;
        while (result != null && result.getKind() != ElementKind.PACKAGE) {
            /*@Nullable*/ Element encl = result.getEnclosingElement();
            result = encl;
        }
        return (PackageElement) result;
    }

    /**
     * Returns true if the element is a static element: whether it is a static field, static method,
     * or static class
     *
     * @return true if element is static
     */
    public static boolean isStatic(Element element) {
        return element.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * Returns true if the element is a final element: a final field, final method, or final class
     *
     * @return true if the element is final
     */
    public static boolean isFinal(Element element) {
        return element.getModifiers().contains(Modifier.FINAL);
    }

    /**
     * 是否是private成员
     */
    public static boolean isPrivate(Element element) {
        return element.getModifiers().contains(Modifier.PRIVATE);
    }

    /**
     *
     * 是否是public成员
     */
    public static boolean isPublic(Element element) {
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    /**
     * 是否是protected成员
     */
    public static boolean isProtected(Element element) {
        return element.getModifiers().contains(Modifier.PROTECTED);
    }


    /**
     * Return all fields declared in the given type or any superclass/interface.
     */
    public static List<VariableElement> getAllFieldsIn(TypeElement type, Elements elements) {
        List<? extends Element> alltypes = elements.getAllMembers(type);
        List<VariableElement> fields = new ArrayList<VariableElement>(ElementFilter.fieldsIn(alltypes));
        return Collections.unmodifiableList(fields);
    }

    /**
     * 从类中查找对应的成员信息
     */
    public static VariableElement findFieldIn(TypeElement type, String fieldName, Elements elements) {
        List<VariableElement> allFields = getAllFieldsIn(type, elements);

        for (VariableElement element : allFields) {
            if (StringUtil.equals(fieldName, element.getSimpleName().toString())) {
                return element;
            }
        }

        return null;
    }

    /**
     * Return all methods declared in the given type or any superclass/interface. Note that no
     * constructors will be returned.
     */
    public static List<ExecutableElement> getAllMethodsIn(TypeElement type, Elements elements) {
        List<? extends Element> alltypes = elements.getAllMembers(type);
        List<ExecutableElement> meths = new ArrayList<ExecutableElement>(ElementFilter.methodsIn(alltypes));
        return Collections.unmodifiableList(meths);
    }

    /**
     * 查找setter函数
     */
    public static ExecutableElement findSetter(TypeElement type, VariableElement field, Elements elements, Types typeUtil) {
        List<ExecutableElement> methods = getAllMethodsIn(type, elements);
        return findSetter(methods, field, typeUtil);
    }

    /**
     * 查找setter函数
     */
    public static ExecutableElement findSetter(List<ExecutableElement> methods, VariableElement field, Types typeUtil) {
        for (ExecutableElement method : methods) {
            String fieldName = field.getSimpleName().toString();
            String setterName = "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);

            String methodName = method.getSimpleName().toString();
            if (StringUtil.isEmpty(methodName) || !StringUtil.equals(setterName, methodName)) {
                continue;
            }

            if (!ElementUtils.isPublic(method)) {
                continue;
            }

            TypeMirror fieldType = field.asType();

            List<? extends VariableElement> params = method.getParameters();
            if (params == null || params.size() != 1) {
                continue;
            }

            VariableElement param = params.get(0);
            TypeMirror paramType = param.asType();


            if (typeUtil.isSameType(paramType, fieldType)) {
                return method;
            }
        }

        return null;
    }

    /**
     * 查找setter函数
     */
    public static ExecutableElement findSetter(List<ExecutableElement> methods, String fieldName, TypeMirror fieldType, Types typeUtil) {
        for (ExecutableElement method : methods) {
            String getterName = "set" + StringUtil.uppercaseFirstChar(fieldName);

            String methodName = method.getSimpleName().toString();
            if (StringUtil.isEmpty(methodName) || !StringUtil.equals(getterName, methodName)) {
                continue;
            }

            if (!ElementUtils.isPublic(method)) {
                continue;
            }

            List<? extends VariableElement> params = method.getParameters();
            if (params == null || params.size() != 1) {
                continue;
            }

            VariableElement param = params.get(0);
            TypeMirror paramType = param.asType();

            if (typeUtil.isSameType(paramType, fieldType)) {
                return method;
            }
        }

        return null;
    }

    /**
     * 查找setter函数名称
     */
    public static String findSetterName(List<ExecutableElement> methods, VariableElement field, Types typeUtil) {
        ExecutableElement method = findSetter(methods, field, typeUtil);

        if (method == null) {
            return "";
        }

        return method.getSimpleName().toString();
    }

    /**
     * 查找getter函数
     */
    public static ExecutableElement findGetter(List<ExecutableElement> methods, VariableElement field, Types typeUtil) {
        for (ExecutableElement method : methods) {
            String fieldName = field.getSimpleName().toString();
            String getterName = "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);

            String methodName = method.getSimpleName().toString();
            if (StringUtil.isEmpty(methodName) || !StringUtil.equals(getterName, methodName)) {
                continue;
            }

            if (!ElementUtils.isPublic(method)) {
                continue;
            }

            TypeMirror returnType = method.getReturnType();
            TypeMirror fieldType = field.asType();
            if (typeUtil.isSameType(returnType, fieldType)) {
                return method;
            }
        }

        return null;
    }

    /**
     * 查找getter函数
     */
    public static ExecutableElement findGetter(List<ExecutableElement> methods, String fieldName, TypeMirror fieldType, Types typeUtil) {
        for (ExecutableElement method : methods) {
            String getterName = "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);

            String methodName = method.getSimpleName().toString();
            if (StringUtil.isEmpty(methodName) || !StringUtil.equals(getterName, methodName)) {
                continue;
            }

            if (!ElementUtils.isPublic(method)) {
                continue;
            }

            TypeMirror returnType = method.getReturnType();
            if (typeUtil.isSameType(returnType, fieldType)) {
                return method;
            }
        }

        return null;
    }

    /**
     * 查找getter函数名称
     */
    public static String findGetterName(List<ExecutableElement> methods, VariableElement field, Types typeUtil) {
        ExecutableElement method = findGetter(methods, field, typeUtil);

        if (method == null) {
            return "";
        }

        return method.getSimpleName().toString();
    }

    /**
     * 获取成员变量Annotation的值
     */
    public static String getFieldAnnotationValue(Element classType, Class annotationType, String attributeName) {
        String value = null;

        Annotation annotation = classType.getAnnotation(annotationType);
        if (annotation != null) {
            try {
                value = (String) annotation.annotationType().getMethod(attributeName).invoke(annotation);
            } catch (Exception ex) {
            }
        }

        return value;
    }
}