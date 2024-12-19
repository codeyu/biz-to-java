package com.example.model;

import com.example.util.StringCompareUtil;

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
            return String.format("//TODO: [%s]", matchedComment);
        }

        String setValue;
        if (matchedValue.equals("ブランク")) {
            setValue = "\"\"";
        } else if (isNumericValue(matchedValue)) {
            // 转换为半角数字
            setValue = convertToHalfWidth(matchedValue);
        } else {
            return String.format("//TODO: [%s] - Unsupported value: %s", 
                matchedComment, matchedValue);
        }

        return String.format("// %s\ntestTable1BaseEntity.%s(%s);", 
            matchedComment,
            matchedField.getSetMethod(), 
            setValue);
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