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

import com.alibaba.android.jsonlube.JsonLubeField;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * 使用 getter / setter的示例
 *
 * Created by shangjie on 2018/2/27.
 */

public class Student extends People {
    private int grade;

    private List<Subject> subjects;

    private Subject favoriteSubject;

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JSONField(name = "grade")
    public int getGradeX() {
        return grade;
    }

    @JSONField(name = "grade")
    public void setGradeX(int grade) {
        this.grade = grade;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public Subject getFavoriteSubject() {
        return favoriteSubject;
    }

    public void setFavoriteSubject(Subject favoriteSubject) {
        this.favoriteSubject = favoriteSubject;
    }
}
