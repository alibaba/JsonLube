# JsonLube

## 功能简述
JsonLube用于将Json对象转成JAVA Bean对象，不同于传统在运行时进行反射的方式，JsonLube采用在编译时自动生成解析Json的代码，使用方式依然简单，然而在移动平台上却可以收获更好的性能。

## 使用方式
### gradle配置
```gradle
    annotationProcessor 'com.alibaba.android:jsonlube-compiler:1.0.0.9@jar'
    compile ('com.alibaba.android:jsonlube:1.0.0.3@aar'){
        transitive=true
    }
````

### Proguard配置
```
-keep @com.alibaba.android.jsonlube.ProguardKeep public class *
```

### Json -> Java Bean 
```java
//1. 在代码中直接调用 JsonLube.fromJson()将Json对象转成Java bean对象。
Teacher teacherBean = JsonLube.fromJson(jsonData, Teacher.class);

//2. 在Teacher类的定义中加上@FromJson注解
@FromJson
public class Teacher {
	...
}


```

### Java Bean -> Json

```java

//1. 直接调用JsonLube.toJson()函数
JsonObject teacherJson = JsonLube.toJson(teacherBean);

//2. 在Teacher类的定义中加上@ToJson注解
@ToJson
public class Teacher {
	···
}

```

### 具体的bean的例子
```java
//在JsonLube.fromJson()/JsonLube.toJson()直接使用到的Java bean类需要加上@FromJson/@ToJson注解，间接引用到的bean无需添加。
//这两个注解根据需要自行添加，如果只需要做反序列化能力，则添加@FromJson一个就够了。
@FromJson
@ToJson
public class Teacher {
	public String name; //支持public成员变量

	private int age; // 支持getter / setter函数

	public int getAge(){
		return this.age;
	}

	public void setAge(int age){
		this.age = age;
	}

	@JsonLubeField(name="sex")
	public int s;


	public List<Student> students;  //支持bean的嵌套
}


public class Student {
	public String name;
	public int age;
	public int sex;
}
```