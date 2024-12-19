package com.example.model;

import com.example.util.StringCompareUtil;

public class GeneratedType1JavaInfo {
    private String classIdentifier;   // 类标识符，如 "手袋(Ｌ０１)"
    private String className;         // 实体类名，如 "TestTable1BaseEntity"
    private String matchedComment;    // 匹配到的注释
    private String matchedValue;      // 匹配到的值
    private FieldInfo matchedField;   // 匹配到的字段信息

    public GeneratedType1JavaInfo(String classIdentifier, String matchedComment, String matchedValue) {
        this.classIdentifier = classIdentifier;
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
        if (matchedField == null || className == null) {
            return String.format("//TODO: [%s] %s", classIdentifier, matchedComment);
        }

        String setValue;
        if (matchedValue.equals("ブランク")) {
            setValue = "\"\"";
        } else if (isNumericValue(matchedValue)) {
            setValue = convertToHalfWidth(matchedValue);
        } else {
            return String.format("//TODO: [%s] %s - Unsupported value: %s", 
                classIdentifier, matchedComment, matchedValue);
        }

        return String.format("// %s\n%s.%s(%s);", 
            matchedComment,
            getInstanceName(),
            matchedField.getSetMethod(), 
            setValue);
    }

    private String getInstanceName() {
        // 将类名转换为实例名（首字母小写）
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private boolean isNumericValue(String value) {
        // 移除所有全角和半角数字，如果是空字符串，说明原字符串只包含数字
        return value.equals("０") || 
               value.replaceAll("[0-9０-９]", "").isEmpty();
    }

    private String convertToHalfWidth(String value) {
        if (value.equals("０")) {
            return "0";
        }
        // 将全角数字转换为半角数字
        StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= '０' && c <= '９') {
                result.append((char) (c - '０' + '0'));
            } else if (c >= '0' && c <= '9') {
                result.append(c);
            }
        }
        return result.toString();
    }
} 