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
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by shangjie on 2018/1/31.
 */
public class ClassFileWriter {
    private Map<String, Holder> mPendingMap = new HashMap<>();

    public TypeSpec append(TypeSpec classFile, ClassName className) {
        if (!isExist(className.toString())) {
            addToPendingMap(classFile, className);
        }

        return classFile;
    }

    public void flush(Filer filer) {
        System.out.println("flush generated class");
        for (Map.Entry<String, Holder> entry : mPendingMap.entrySet()) {
            Holder holder = entry.getValue();
            writeToFile(holder.className.packageName(), holder.classFile, filer);
        }
    }

    public boolean isExist(String name) {
        return mPendingMap.containsKey(name);
    }

    private void writeToFile(String packageName, TypeSpec classFile, Filer filer) {
        JavaFile javaFile = JavaFile.builder(packageName, classFile)
                .addFileComment("自动生成的代码，切勿修改")
                .build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            //根据需要可以打开调试，默认情况下关掉，以免打包日志被干扰
            //e.printStackTrace();
        }
    }

    private void printClassFile(String packageName, TypeSpec classFile) {
        JavaFile javaFile = JavaFile.builder(packageName, classFile)
                .addFileComment("自动生成的代码，切勿修改")
                .build();

        try {
            javaFile.writeTo(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToPendingMap(TypeSpec classFile, ClassName className) {
        Holder holder = new Holder();
        holder.classFile = classFile;
        holder.className = className;
        mPendingMap.put(className.toString(), holder);
    }

    private static class Holder{
        TypeSpec classFile;
        ClassName className;
    }

}
