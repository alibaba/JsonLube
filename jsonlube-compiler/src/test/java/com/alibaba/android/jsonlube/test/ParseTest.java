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

package com.alibaba.android.jsonlube.test;

import com.alibaba.android.jsonlube.compiler.StringUtil;
import com.alibaba.android.jsonlube.testbean.Teacher;
import com.alibaba.fastjson.JSON;
import com.google.common.io.Resources;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;

public class ParseTest {

    @Test
    public void parseTest() {
        System.out.println("parse test start ---- zsl");


        try {
        String jsonString = loadJSONFromAsset();
            org.json.JSONObject androidJson = new org.json.JSONObject(jsonString);
            Teacher androidBean = parseJson(androidJson, Teacher.class);
            Teacher fastBean = JSON.parseObject(jsonString, Teacher.class);

            Assert.assertTrue(isEqual(androidBean, fastBean));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void serializeTest() {
        try {
            String jsonString = loadJSONFromAsset();
            Teacher fastBean = JSON.parseObject(jsonString, Teacher.class);

            JSONObject androidJsonObj = toJson(fastBean);
            String androidString = androidJsonObj.toString();

            Teacher fastBean2 = JSON.parseObject(androidString, Teacher.class);
            Assert.assertTrue(isEqual(fastBean, fastBean2));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String loadJSONFromAsset() {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = null;
        try {
            File file = new File(Resources.getResource("sample.json").toURI());
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                buffer.append(mLine);
            }
        } catch (Exception e) {
            //log the exception
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    e.printStackTrace();
                }
            }
        }
        return buffer.toString();
    }

    private boolean isEqual(Teacher left, Teacher right) {
        String leftStr = JSON.toJSONString(left);
        String rightStr = JSON.toJSONString(right);

        return !StringUtil.isEmpty(leftStr) && StringUtil.equals(leftStr, rightStr);
    }

    public final <T> T parseJson(JSONObject json, Class<T> clazz){
        String parserClassName = getParserClassName(clazz);

        try {
            Class<?> parserClass = Class.forName(parserClassName);
            Method parseMethod = parserClass.getMethod("parse", JSONObject.class);
            return (T)parseMethod.invoke(null, json);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getParserClassName(Class<?> beanClass) {
        String name = beanClass.getCanonicalName();
        return name + "_JsonLubeParser";
    }

    public final  <T> JSONObject toJson(T bean) {
        String serializerClassName = getSerializerClassName(bean.getClass());

        try {
            Class<?> serializerClass = Class.forName(serializerClassName);
            Method serializeMethod = serializerClass.getMethod("serialize", bean.getClass());
            return (JSONObject) serializeMethod.invoke(null, bean);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getSerializerClassName(Class<?> beanClass) {
        String name = beanClass.getCanonicalName();
        return name + "_JsonLubeSerializer";
    }

    private void write(String fileName, String content) {
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            writer.println(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
