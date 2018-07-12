package com.alibaba.jsonlube;

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

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
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
