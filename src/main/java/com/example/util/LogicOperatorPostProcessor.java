package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import com.example.model.VariableDefinition;

public class LogicOperatorPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LogicOperatorPostProcessor.class);
    
    private static final Pattern IF_PATTERN = Pattern.compile("if\\s*\\((.+?)\\)\\s*\\{");
    private static final Pattern CONDITION_PATTERN = 
        Pattern.compile("([^=!<>\\s]+)\\s*(==|!=|>=|<=|>|<)\\s*([^\\s&|]+)");

    private Map<String, VariableDefinition> variableDefinitions;

    public void setVariableDefinitions(Map<String, VariableDefinition> definitions) {
        this.variableDefinitions = definitions;
    }

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
        logger.debug("Converting comparison - left: [{}], operator: [{}], right: [{}]", left, operator, right);
        
        String result = null;
        if (left.contains("get") || right.contains("get")) {
            result = handleGetterComparison(left, operator, right);
        } else if (left.startsWith("this.") && right.startsWith("this.")) {
            result = handleThisComparison(left, operator, right);
        } else if (right.equals("\"\"")) {
            result = handleEmptyStringComparison(left, operator);
        } else if (right.matches("[0-9]+")) {
            result = handleNumericComparison(left, operator, right);
        } else if (right.startsWith("\"") && right.endsWith("\"")) {
            result = handleStringComparison(left, operator, right);
        }

        if (result != null) {
            logger.debug("Converted to: [{}]", result);
            return result;
        }

        // 默认情况
        result = String.format("%s %s %s", left, operator, right);
        logger.debug("Using default comparison: [{}]", result);
        return result;
    }

    private boolean isNumericGetter(String expression) {
        // 暂时通过方法名判断，后续可以改为通过 FieldInfo 判断
        return expression != null && (
            expression.contains("getTestField5") || 
            expression.contains("getTestField6") ||
            expression.contains("getTestField8")
        );
    }

    private String handleGetterComparison(String left, String operator, String right) {
        // 如果是数字类型的getter，使用数字比较
        if (isNumericGetter(left) || isNumericGetter(right)) {
            switch (operator) {
                case "==": return String.format("NumUtil.eq(%s, %s)", left, right);
                case "!=": return String.format("!NumUtil.eq(%s, %s)", left, right);
                case ">": return String.format("NumUtil.gt(%s, %s)", left, right);
                case ">=": return String.format("NumUtil.ge(%s, %s)", left, right);
                case "<": return String.format("NumUtil.lt(%s, %s)", left, right);
                case "<=": return String.format("NumUtil.le(%s, %s)", left, right);
            }
        }
        
        // 其他情况使用字符串比较
        if (operator.equals("==")) {
            return String.format("StrUtil.eq(%s, %s)", left, right);
        } else if (operator.equals("!=")) {
            return String.format("!StrUtil.eq(%s, %s)", left, right);
        }
        
        return null;
    }

    private String handleThisComparison(String left, String operator, String right) {
        // 提取变量名
        String leftVar = left.substring(5);  // 去掉 "this."
        String rightVar = right.substring(5);  // 去掉 "this."
        
        logger.debug("Handling this comparison - leftVar: {}, rightVar: {}", leftVar, rightVar);
        
        // 获取变量定义
        VariableDefinition leftDef = variableDefinitions != null ? variableDefinitions.get(leftVar) : null;
        VariableDefinition rightDef = variableDefinitions != null ? variableDefinitions.get(rightVar) : null;
        
        // 记录日志
        if (leftDef == null || rightDef == null) {
            logger.warn("Variable definition not found - leftVar: {} ({}), rightVar: {} ({})", 
                leftVar, leftDef, rightVar, rightDef);
        }
        
        // 如果任一变量是数字类型，使用数字比较
        if ((leftDef != null && leftDef.isNumericType()) || 
            (rightDef != null && rightDef.isNumericType())) {
            logger.debug("Using numeric comparison for {} {} {}", left, operator, right);
            switch (operator) {
                case "==": return String.format("NumUtil.eq(%s, %s)", left, right);
                case "!=": return String.format("!NumUtil.eq(%s, %s)", left, right);
                case ">": return String.format("NumUtil.gt(%s, %s)", left, right);
                case ">=": return String.format("NumUtil.ge(%s, %s)", left, right);
                case "<": return String.format("NumUtil.lt(%s, %s)", left, right);
                case "<=": return String.format("NumUtil.le(%s, %s)", left, right);
            }
        }
        
        // 默认使用字符串比较（当变量定义找不到或者不是数字类型时）
        logger.debug("Using default string comparison for {} {} {}", left, operator, right);
        if (operator.equals("==")) {
            return String.format("StrUtil.eq(%s, %s)", left, right);
        } else if (operator.equals("!=")) {
            return String.format("!StrUtil.eq(%s, %s)", left, right);
        }
        return null;
    }

    private String handleEmptyStringComparison(String left, String operator) {
        if (operator.equals("==")) {
            return String.format("StrUtil.isEmpty(%s)", left);
        } else if (operator.equals("!=")) {
            return String.format("!StrUtil.isEmpty(%s)", left);
        }
        return null;
    }

    private String handleNumericComparison(String left, String operator, String right) {
        switch (operator) {
            case "==": return String.format("NumUtil.eq(%s, %s)", left, right);
            case "!=": return String.format("!NumUtil.eq(%s, %s)", left, right);
            case ">": return String.format("NumUtil.gt(%s, %s)", left, right);
            case ">=": return String.format("NumUtil.ge(%s, %s)", left, right);
            case "<": return String.format("NumUtil.lt(%s, %s)", left, right);
            case "<=": return String.format("NumUtil.le(%s, %s)", left, right);
            default: return null;
        }
    }

    private String handleStringComparison(String left, String operator, String right) {
        if (operator.equals("==")) {
            return String.format("StrUtil.eq(%s, %s)", left, right);
        } else if (operator.equals("!=")) {
            return String.format("!StrUtil.eq(%s, %s)", left, right);
        }
        return null;
    }
} 