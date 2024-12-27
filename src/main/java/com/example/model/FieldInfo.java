package com.example.model;

public class FieldInfo {
    private String tableFieldName;  // 表字段名 test_field_9
    private String fieldName;       // Java字段名 testField9
    private String fieldComment;    // 字段注释 常務コード
    private String setMethod;       // setter方法名 setTestField9
    private String getMethod;       // getter方法名 getTestField9
    private String fieldType;       // 字段类型 String, Integer等

    public FieldInfo(String tableFieldName, String fieldComment) {
        this.tableFieldName = tableFieldName;
        this.fieldComment = fieldComment;
        this.fieldName = convertToFieldName(tableFieldName);
        this.setMethod = "set" + capitalize(fieldName);
        this.getMethod = "get" + capitalize(fieldName);
    }

    private String convertToFieldName(String tableFieldName) {
        // 将表字段名转换为驼峰命名
        StringBuilder result = new StringBuilder();
        String[] parts = tableFieldName.split("_");
        result.append(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            result.append(capitalize(parts[i].toLowerCase()));
        }
        return result.toString();
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Getters
    public String getTableFieldName() { return tableFieldName; }
    public String getFieldName() { return fieldName; }
    public String getFieldComment() { return fieldComment; }
    public String getSetMethod() { return setMethod; }
    public String getGetMethod() { return getMethod; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public boolean isNumericType() {
        return fieldType != null && (
            fieldType.equals("Integer") || 
            fieldType.equals("int") || 
            fieldType.equals("Double") || 
            fieldType.equals("double") ||
            fieldType.equals("Float") || 
            fieldType.equals("float")
        );
    }

    public boolean isLongType() {
        return fieldType != null && (
            fieldType.equals("Long") 
        );
    }

    public boolean isStringType() {
        return fieldType != null && fieldType.equals("String");
    }

    public boolean isDateType() {
        return fieldType != null && (
            fieldType.equals("Date") || 
            fieldType.equals("LocalDate") ||
            fieldType.equals("LocalDateTime")
        );
    }
} 