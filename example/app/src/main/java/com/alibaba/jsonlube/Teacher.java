package com.alibaba.jsonlube;

import java.util.List;

import com.alibaba.android.jsonlube.FromJson;
import com.alibaba.android.jsonlube.ToJson;

/**
 * Created by shangjie on 2018/2/27.
 */

@FromJson
@ToJson
public class Teacher extends People {

    public List<Student> students;

    public Student bestStudent;

}
