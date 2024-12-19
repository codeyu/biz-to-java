package com.example.model;

public class GeneratedType1JavaInfo {
    private String matchedComment;    // 匹配到的注释，如 "生活／仕事　会社コード"
    private String matchedValue;      // 匹配到的值，如 "ブランク" 或 "０"
    private FieldInfo matchedField;   // 匹配到的字段信息，可能为null

    public GeneratedType1JavaInfo(String matchedComment, String matchedValue) {
        this.matchedComment = matchedComment;
        this.matchedValue = matchedValue;
    }

    public String getMatchedComment() {
        return matchedComment;
    }

    public String getMatchedValue() {
        return matchedValue;
    }

    public FieldInfo getMatchedField() {
        return matchedField;
    }

    public void setMatchedField(FieldInfo matchedField) {
        this.matchedField = matchedField;
    }

    public String generateCode() {
        if (matchedField == null) {
            return null;
        }

        String setValue;
        if (matchedValue.equals("ブランク")) {
            setValue = "\"\"";
        } else if (matchedValue.equals("０") || matchedValue.matches("[0-9]+")) {
            setValue = matchedValue.equals("０") ? "0" : matchedValue;
        } else {
            return null;
        }

        return String.format("testTable1BaseEntity.%s(%s);", 
            matchedField.getSetMethod(), setValue);
    }
} 