package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicOperatorPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LogicOperatorPostProcessor.class);
    
    private static final Pattern IF_PATTERN = Pattern.compile("if\\s*\\((.+?)\\)\\s*\\{");
    private static final Pattern CONDITION_PATTERN = 
        Pattern.compile("([^=!<>\\s]+)\\s*(==|!=|>=|<=|>|<)\\s*([^\\s&|]+)");

    public String process(String code) {
        logger.debug("Processing code: {}", code);
        if (!code.trim().startsWith("if")) {
            logger.debug("Not an if statement, returning original code");
            return code;
        }

        Matcher ifMatcher = IF_PATTERN.matcher(code);
        if (!ifMatcher.find()) {
            logger.debug("No if condition found, returning original code");
            return code;
        }

        String condition = ifMatcher.group(1);
        String processedCondition = processCondition(condition);
        
        // 只有当条件确实被修改时才返回新代码
        if (!condition.equals(processedCondition)) {
            String newCode = code.replace(condition, processedCondition);
            logger.info("Converted condition from [{}] to [{}]", condition, processedCondition);
            return newCode;
        }
        
        return code;
    }

    private String processCondition(String condition) {
        logger.debug("Processing condition: {}", condition);
        StringBuffer result = new StringBuffer();
        Matcher matcher = CONDITION_PATTERN.matcher(condition);
        
        while (matcher.find()) {
            String left = matcher.group(1).trim();
            String operator = matcher.group(2);
            String right = matcher.group(3).trim();
            
            logger.debug("Found comparison: left=[{}], operator=[{}], right=[{}]", left, operator, right);
            String replacement = convertComparison(left, operator, right);
            logger.debug("Converted to: {}", replacement);
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    private String convertComparison(String left, String operator, String right) {
        // 1. 空字符串比较
        if (right.equals("\"\"")) {
            if (operator.equals("==")) {
                return String.format("StrUtil.isEmpty(%s)", left);
            } else if (operator.equals("!=")) {
                return String.format("!StrUtil.isEmpty(%s)", left);
            }
        }
        
        // 2. 数字比较
        if (right.matches("[0-9]+")) {
            switch (operator) {
                case "==": return String.format("NumUtil.eq(%s, %s)", left, right);
                case "!=": return String.format("!NumUtil.eq(%s, %s)", left, right);
                case ">": return String.format("NumUtil.gt(%s, %s)", left, right);
                case ">=": return String.format("NumUtil.ge(%s, %s)", left, right);
                case "<": return String.format("NumUtil.lt(%s, %s)", left, right);
                case "<=": return String.format("NumUtil.le(%s, %s)", left, right);
            }
        }
        
        // 3. 字符串比较
        if (right.startsWith("\"") && right.endsWith("\"")) {
            if (operator.equals("==")) {
                return String.format("StrUtil.eq(%s, %s)", left, right);
            } else if (operator.equals("!=")) {
                return String.format("!StrUtil.eq(%s, %s)", left, right);
            }
        }
        
        // 如果没有匹配任何规则，返回原始比较
        return String.format("%s %s %s", left, operator, right);
    }
} 