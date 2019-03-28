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

/**
 * 字符串操作类
 */
public class StringUtil {
    /**
     * 是否为空
     */
    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    /**
     * 两个字符串是否相等
     */
    public static boolean equals(String arg1, String arg2) {
        if (arg1 == null) {
            return arg1 == arg2;
        }

        return arg1.equals(arg2);
    }

    /**
     * 将字符串第一个字符改成小写
     */
    public static String lowercaseFirstChar(String string) {
        char c[] = string.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        return new String(c);
    }

    /**
     * 将字符串第一个字符改成大写
     */
    public static String uppercaseFirstChar(String string) {
        char c[] = string.toCharArray();
        c[0] = Character.toUpperCase(c[0]);
        return new String(c);
    }
}
