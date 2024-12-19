package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicOperatorConverter {
    private static final Logger logger = LoggerFactory.getLogger(LogicOperatorConverter.class);
    
    // 匹配 if 语句中的条件
    private static final Pattern IF_CONDITION_PATTERN = 
        Pattern.compile("if\\((.+?)\\)");
    
    // 匹配比较表达式
    private static final Pattern COMPARISON_PATTERN = 
        Pattern.compile("([^=!<>]+?)\\s*(==|!=|>=|<=|>|<)\\s*(.+?)(?=\\s*[&&||]|$)");

    public String convert(String code) {
        if (!code.startsWith("if")) {
            return code;
        }

        Matcher ifMatcher = IF_CONDITION_PATTERN.matcher(code);
        if (!ifMatcher.find()) {
            return code;
        }

        String condition = ifMatcher.group(1);
        String convertedCondition = convertCondition(condition);
        return code.replace(condition, convertedCondition);
    }

    private String convertCondition(String condition) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = COMPARISON_PATTERN.matcher(condition);
        
        while (matcher.find()) {
            String leftSide = matcher.group(1).trim();
            String operator = matcher.group(2);
            String rightSide = matcher.group(3).trim();
            
            String replacement = convertComparison(leftSide, operator, rightSide);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private String convertComparison(String leftSide, String operator, String rightSide) {
        // 1. 空字符串比较
        if (rightSide.equals("\"\"")) {
            return operator.equals("==") ? 
                   String.format("StrUtil.isEmpty(%s)", leftSide) :
                   String.format("!StrUtil.isEmpty(%s)", leftSide);
        }
        
        // 2. 数字比较
        if (rightSide.matches("[0-9]+")) {
            switch (operator) {
                case "==": return String.format("NumUtil.eq(%s, %s)", leftSide, rightSide);
                case "!=": return String.format("!NumUtil.eq(%s, %s)", leftSide, rightSide);
                case ">": return String.format("NumUtil.gt(%s, %s)", leftSide, rightSide);
                case ">=": return String.format("NumUtil.ge(%s, %s)", leftSide, rightSide);
                case "<": return String.format("NumUtil.lt(%s, %s)", leftSide, rightSide);
                case "<=": return String.format("NumUtil.le(%s, %s)", leftSide, rightSide);
            }
        }
        
        // 3. 字符串比较
        if (rightSide.startsWith("\"") && rightSide.endsWith("\"")) {
            switch (operator) {
                case "==": return String.format("StrUtil.eq(%s, %s)", leftSide, rightSide);
                case "!=": return String.format("!StrUtil.eq(%s, %s)", leftSide, rightSide);
            }
        }
        
        // 默认情况，保持原样
        return String.format("%s %s %s", leftSide, operator, rightSide);
    }
} 