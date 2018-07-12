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

package com.alibaba.android.jsonlube;

import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * JsonLube基于Android原生Json库生成Json序列化和反序列化代码。
 * 用户通过{@link JsonLube}调用这些自动生成的代码，而无需关心具体实现细节。
 *
 * Created by shangjie on 2018/2/5.
 */
public class JsonLube {

    /**
     * 将原生的{@link JSONObject}对象转成自定义的Java bean对象。
     *
     * @param json 原生的Json对象
     * @param clazz 目标Java bean对象
     * @param <T> 目标Java bean的泛型
     * @return 目标Java bean对象
     * @throws JsonLubeParseException
     */
    public static final <T> T fromJson(JSONObject json, Class<T> clazz) throws JsonLubeParseException{
        String parserClassName = getParserClassName(clazz);

        try {
            Class<?> parserClass = Class.forName(parserClassName);
            Method parseMethod = parserClass.getMethod("parse", JSONObject.class);
            return (T)parseMethod.invoke(null, json);
        } catch (Exception e) {
            throw new JsonLubeParseException(e);
        }

    }


    /**
     * 将Java对象转成原生{@link JSONObject}对象
     * @param bean 需要转化的Java bean对象
     * @param <T> 需要转化的Java bean对象的泛型
     * @return 生成的Json对象
     * @throws JsonLubeSerializerException
     */
    public static final  <T> JSONObject toJson(T bean) throws JsonLubeSerializerException {
        String serializerClassName = getSerializerClassName(bean.getClass());

        try {
            Class<?> serializerClass = Class.forName(serializerClassName);
            Method serializeMethod = serializerClass.getMethod("serialize", bean.getClass());
            return (JSONObject) serializeMethod.invoke(null, bean);
        } catch (Exception e) {
            throw new JsonLubeSerializerException(e);
        }
    }

    private static String getParserClassName(Class<?> beanClass) {
        String name = beanClass.getCanonicalName();
        return name + "_JsonLubeParser";
    }

    private static String getSerializerClassName(Class<?> beanClass) {
        String name = beanClass.getCanonicalName();
        return name + "_JsonLubeSerializer";
    }
}
