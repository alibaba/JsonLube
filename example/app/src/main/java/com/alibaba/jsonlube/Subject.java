package com.alibaba.jsonlube;

import com.alibaba.android.jsonlube.JsonLubeField;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * 使用注解兼容成员变量名和Json字符命名的不一致。此处兼容FastJson的{@link JSONField}注解
 *
 * Created by shangjie on 2018/2/27.
 */

public class Subject {

    @JSONField(name = "subjectName")
    public String name;


    @JsonLubeField(name = "subjectScore")
    public float score;
}
