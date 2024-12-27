package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import com.example.model.VariableDefinition;
import com.example.model.ClassInfo;
import com.example.model.FieldInfo;

public class LogicOperatorPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LogicOperatorPostProcessor.class);
    
    private static final Pattern IF_PATTERN = Pattern.compile("if\\s*\\((.+?)\\)\\s*\\{");
    private static final Pattern CONDITION_PATTERN = 
        Pattern.compile("([^=!<>\\s]+)\\s*(==|!=|>=|<=|>|<)\\s*([^\\s&|]+)");

    private Map<String, VariableDefinition> variableDefinitions;
    private Map<String, ClassInfo> entityInfos;

    public void setVariableDefinitions(Map<String, VariableDefinition> definitions) {
        this.variableDefinitions = definitions;
    }

    public void setEntityInfos(Map<String, ClassInfo> entityInfos) {
        this.entityInfos = entityInfos;
        logger.debug("EntityInfos set with {} entries", 
            entityInfos != null ? entityInfos.size() : 0);
    }

    public String process(String code) {
        logger.debug("Processing code: {}", code);
        if (!code.trim().startsWith("if")) {
            logger.debug("Not an if statement, returning original code");
            return code;
        }
        if(code.contains("//IFのERROR:")) {
            logger.debug("IFのERROR: found, returning error code");
            return processError(code);
        }
        Matcher ifMatcher = IF_PATTERN.matcher(code);
        if (!ifMatcher.find()) {
            logger.debug("No if condition found, returning original code");
            return "//TODO:手動で処理をお願いします\n" + code;
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

    private String processError(String code) {
        logger.debug("Processing error code: {}", code);
        
        // 首先移除所有的 //IFのERROR:
        String cleanCode = code.replaceAll("//IFのERROR:\\s*", "");
        logger.debug("Code after removing TODO: {}", cleanCode);
        
        // 按行分割
        String[] lines = cleanCode.split("\n");
        
        // 在每行前添加 //TODO:
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                result.append("//TODO: ").append(line).append("\n");
            } else {
                result.append(line).append("\n");  // 保持空行不变
            }
        }
        
        logger.debug("Final processed error code: {}", result.toString());
        return result.toString();
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
        if (expression == null) {
            return false;
        }
        
        // 从表达式中提取字段信息
        for (ClassInfo entityInfo : entityInfos.values()) {
            for (FieldInfo field : entityInfo.getFields()) {
                String getterMethod = field.getGetMethod();
                if (expression.contains(getterMethod)) {
                    return field.isNumericType();
                }
            }
        }
        return false;
    }

    private String getTypeInfo(String expression) {
        if (expression == null) {
            return "";
        }
        
        // 从表达式中提取字段信息
        for (ClassInfo entityInfo : entityInfos.values()) {
            for (FieldInfo field : entityInfo.getFields()) {
                String getterMethod = field.getGetMethod();
                if (expression.contains(getterMethod)) {
                    if(field.isNumericType()){
                        return "Number";
                    }
                    if(field.isLongType()){
                        return "Long";
                    }
                    if(field.isStringType()){
                        return "String";
                    }
                    if(field.isDateType()){
                        return "Date";
                    }
                }
            }
        }
        return "";
    }

    private String handleGetterComparison(String left, String operator, String right) {
        logger.debug("Starting handleGetterComparison - left: [{}], operator: [{}], right: [{}]", 
            left, operator, right);

        // 获取类型信息
        String leftType = getTypeInfo(left);
        logger.debug("Got left type: [{}]", leftType);

        String functionName = "";
        switch (leftType) {
            case "Number":
                functionName = "NumUtil";
                break;
            case "Long":
                functionName = "NumUtil";
                break;
            case "String":
                functionName = "StrUtil";
                break;
            case "Date":
                functionName = "DateUtil";
                break;
            default:
                logger.debug("No matching type found, using default");
                break;
        }
        logger.debug("Selected function name: [{}]", functionName);

        String functionPart = "";
        switch (operator) {
            case "==": 
                functionPart = functionName + ".eq";
                break;
            case "!=": 
                functionPart = "!" + functionName + ".eq";
                break;
            case ">": 
                functionPart = functionName + ".gt";
                break;
            case ">=": 
                functionPart = functionName + ".ge";
                break;
            case "<": 
                functionPart = functionName + ".lt";
                break;
            case "<=": 
                functionPart = functionName + ".le";
                break;
            default:
                logger.debug("Unsupported operator: [{}]", operator);
                break;
        }
        logger.debug("Generated function part: [{}]", functionPart);

        if(!functionPart.isEmpty() && !functionName.isEmpty()) {
            String result = String.format("%s(%s, %s)", functionPart, left, right);
            logger.debug("Generated comparison: [{}]", result);
            return result;
        }

        // 其他情况使用字符串比较
        logger.debug("Falling back to string comparison");
        if (operator.equals("==")) {
            String result = String.format("StrUtil.eq(%s, %s)", left, right);
            logger.debug("Generated string equals comparison: [{}]", result);
            return result;
        } else if (operator.equals("!=")) {
            String result = String.format("!StrUtil.eq(%s, %s)", left, right);
            logger.debug("Generated string not equals comparison: [{}]", result);
            return result;
        }
        
        logger.debug("No comparison generated, returning null");
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
        logger.debug("Variable definitions - left: {} ({}), right: {} ({})", 
            leftVar, leftDef != null ? leftDef.getType() : "null",
            rightVar, rightDef != null ? rightDef.getType() : "null");

        // 检查变量类型
        boolean isLeftNumeric = leftDef != null && leftDef.isNumericType();
        boolean isRightNumeric = rightDef != null && rightDef.isNumericType();
        
        // 如果两个变量都找到了定义，并且至少一个是数字类型
        if (leftDef != null && rightDef != null && (isLeftNumeric || isRightNumeric)) {
            logger.debug("Using numeric comparison because variables are numeric types");
            switch (operator) {
                case "==": return String.format("NumUtil.eq(%s, %s)", left, right);
                case "!=": return String.format("!NumUtil.eq(%s, %s)", left, right);
                case ">": return String.format("NumUtil.gt(%s, %s)", left, right);
                case ">=": return String.format("NumUtil.ge(%s, %s)", left, right);
                case "<": return String.format("NumUtil.lt(%s, %s)", left, right);
                case "<=": return String.format("NumUtil.le(%s, %s)", left, right);
            }
        }
        
        // 如果找不到变量定义或者都是字符串类型，使用字符串比较
        logger.debug("Using string comparison as fallback");
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