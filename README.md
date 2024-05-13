
# CodeGenerator.java
CodeGenerator is a simple translator that translate mermaid code to java code, but not all data type could be translated.

## Usage
```
javac CodeGenerator.java
java CodeGenerator <mermaidCodeFile_that_want_to_be_translated>
```

## Example
Given a mermaid code file "mermaid.txt":
```
classDiagram
    class Person 
    Person : +introduceSelf(String name) void

    class Student {
        +String studentID
        +study() void
    }
    
    class Teacher {
        +String teacherID
        +teach() void
    }
    Person : -int age
    Person : -String name

    class Student {
        -int number
        -Teacher coorespondingTeacher
    }
```

```
java CodeGenerator mermaid.txt
```
This program will translate the above to three files 

Person.java
```
public class Person {
    public void introduceSelf(String name) {;}
    private int age;
    private String name;
}
```

Student.java
```
public class Student {
    private String studentID;
    public void study() {;}
    private int number;
    private Teacher coorespondingTeacher;
}
```

Teacher.java
```
public class Teacher {
    public String teacherID;
    public void teach() {;}
}
```

