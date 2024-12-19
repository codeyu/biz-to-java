package com.example.model;

import com.example.util.StringCompareUtil;
import java.util.ArrayList;
import java.util.List;

public class ClassInfo {
    private String className;
    private String classComment;
    private List<FieldInfo> fields;

    public ClassInfo() {
        this.fields = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassComment() {
        return classComment;
    }

    public void setClassComment(String classComment) {
        this.classComment = classComment;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void addField(FieldInfo field) {
        this.fields.add(field);
    }

    public FieldInfo findFieldByComment(String comment) {
        return fields.stream()
            .filter(field -> StringCompareUtil.compareJapaneseString(
                field.getFieldComment(), comment))
            .findFirst()
            .orElse(null);
    }
} 