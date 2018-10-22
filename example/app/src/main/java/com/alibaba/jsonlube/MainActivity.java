package com.alibaba.jsonlube;

import java.io.IOException;
import java.io.InputStream;

import com.alibaba.android.jsonlube.JsonLube;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String TAG = "JsonLubeExample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String jsonStr = loadJSONFromAsset();
            JSONObject androidJson = new JSONObject(jsonStr);
            Teacher teacher = JsonLube.fromJson(androidJson, Teacher.class);

            Log.d(TAG, "Teacher Name: " + teacher.name + "  Age: " + teacher.age + "  Sex: " + teacher.getSex() +"  Best Student: " + teacher.bestStudent.name);

            for (Student student : teacher.students) {
                Log.d(TAG, "Student Name: " + student.name + "  Age: " + student.age + "  Sex: " + student.getSex() + "  Favorite Subject: " + student
                    .getFavoriteSubject().name);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("sample.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
