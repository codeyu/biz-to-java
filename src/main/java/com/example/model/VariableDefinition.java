package com.example.model;

public class VariableDefinition {
    private String name;
    private String type;
    private String defaultValue;

    public VariableDefinition(String name, String type, String defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public String getDefaultValue() { return defaultValue; }

    public boolean isNumericType() {
        return type != null && (
            type.equals("Integer") || 
            type.equals("int") || 
            type.equals("Double") || 
            type.equals("double") ||
            type.equals("Float") || 
            type.equals("float")
        );
    }

    public boolean isStringType() {
        return type != null && type.equals("String");
    }
} 