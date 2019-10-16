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

package com.alibaba.android.jsonlube.testbean;

import java.util.HashMap;
import java.util.List;

import com.alibaba.android.jsonlube.FromJson;
import com.alibaba.android.jsonlube.ToJson;

import org.json.JSONObject;

/**
 * Created by shangjie on 2018/2/27.
 */

@FromJson
@ToJson
public class Teacher extends People {
    private long id;
    protected long salary;

    public List<Student> students;

    public Student bestStudent;

    public HashMap<String, Student> studentMap;

    private HashMap<String, String> names;

    public HashMap<String, String> getNames() {
        return names;
    }

    public void setNames(HashMap<String, String> names) {
        this.names = names;
    }

    public JSONObject params;
}
