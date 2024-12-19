package com.example.strategy;

import com.example.model.*;
import com.example.util.JavaFileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.*;
import java.io.IOException;

public class Type2TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type2TextConverter.class);
    
    private static final Pattern CONDITION_START = Pattern.compile("》【条件】(.+)のとき");
    private static final Pattern CONDITION_CONTINUE = Pattern.compile("^\\s*(または|かつ)、(.+)のとき");
    private static final Pattern ENTITY_PATTERN = Pattern.compile("項目「([^」]*)\\.(\\(([^)]+)\\))」");
    private static final Pattern DIRECT_PATTERN = Pattern.compile("項目「\\s*D\\\\([^」]+)\\s*」");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("項目「\\*([^」]+)」");
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(.+)(＝|≠)(.+)");

    private Map<String, ClassInfo> entityInfoMap;
    private Map<String, String> entityFiles;

    public void setEntityFiles(Map<String, String> entityFiles) {
        this.entityFiles = entityFiles;
        this.entityInfoMap = new HashMap<>();
    }

    @Override
    public String convertLine(String line, List<String> entityLines) {
        // 实现单行转换（如果需要）
        return null;
    }

    public List<String> convertFile(List<String> lines) {
        List<String> results = new ArrayList<>();
        GeneratedType2JavaInfo currentInfo = null;
        
        logger.info("Starting to process {} lines", lines.size());
        
        for (String line : lines) {
            line = normalizeSpaces(line);
            logger.info("Processing normalized line: {}", line);
            
            if (line.startsWith("》【条件】")) {
                logger.info("Found condition start: {}", line);
                if (currentInfo != null) {
                    String generatedCode = currentInfo.generateCode();
                    logger.info("Generated code for previous condition: {}", generatedCode);
                    results.add(generatedCode);
                }
                currentInfo = new GeneratedType2JavaInfo();
                processConditionLine(line, currentInfo);
            } else if (line.matches("^\\s*(または|かつ)、.*")) {
                logger.info("Found condition continuation with operator: {}", line);
                processContinuationLine(line, currentInfo);
            } else if (line.startsWith("項目「")) {
                logger.info("Found assignment: {}", line);
                processAssignmentLine(line, currentInfo);
            }
        }
        
        if (currentInfo != null) {
            String generatedCode = currentInfo.generateCode();
            logger.info("Generated code for final condition: {}", generatedCode);
            results.add(generatedCode);
        }
        
        logger.info("Finished processing all lines. Generated {} code blocks", results.size());
        return results;
    }

    private void processConditionLine(String line, GeneratedType2JavaInfo info) {
        logger.info("Processing condition line: {}", line);
        Matcher matcher = CONDITION_START.matcher(line);
        if (matcher.find()) {
            String condition = matcher.group(1);
            logger.info("Extracted condition: {}", condition);
            processCondition(condition, info.getCondition(), null);
        } else {
            logger.warn("Failed to match condition pattern in line: {}", line);
        }
    }

    private void processContinuationLine(String line, GeneratedType2JavaInfo info) {
        logger.info("Processing continuation line: {}", line);
        Matcher matcher = CONDITION_CONTINUE.matcher(line);
        if (matcher.find()) {
            String operator = matcher.group(1);
            String condition = matcher.group(2);
            logger.info("Extracted operator: {}, condition: {}", operator, condition);
            processCondition(condition, info.getCondition(), operator);
        } else {
            logger.warn("Failed to match continuation pattern in line: {}", line);
        }
    }

    private void processCondition(String condition, GeneratedType2JavaInfo.Condition conditionInfo, String operator) {
        logger.info("Processing condition: {}, operator: {}", condition, operator);
        if (operator != null) {
            logger.info("Setting logical operator: {}", operator);
            conditionInfo.setLogicalOperator(operator);
        }

        Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(condition);
        if (comparisonMatcher.find()) {
            String leftSide = comparisonMatcher.group(1).trim();
            String op = comparisonMatcher.group(2);
            String rightSide = comparisonMatcher.group(3).trim();
            
            logger.info("Extracted comparison - left: {}, operator: {}, right: {}", leftSide, op, rightSide);
            
            String leftValue = extractValue(leftSide);
            String rightValue = extractValue(rightSide);
            
            logger.info("Converted values - left: {}, right: {}", leftValue, rightValue);
            
            if (leftValue != null && rightValue != null) {
                conditionInfo.addPart(new GeneratedType2JavaInfo.ConditionPart(
                    leftValue,
                    op,
                    rightValue
                ));
                logger.info("Added condition part to condition info");
            } else {
                logger.warn("Failed to extract values from condition: {}", condition);
            }
        } else {
            logger.warn("Failed to match comparison pattern in condition: {}", condition);
        }
    }

    private void processAssignmentLine(String line, GeneratedType2JavaInfo info) {
        // 处理实体字段赋值
        Matcher entityMatcher = ENTITY_PATTERN.matcher(line);
        if (entityMatcher.find()) {
            String entityId = entityMatcher.group(1);
            String fieldComment = entityMatcher.group(3);
            
            // 查找目标值
            String targetValue = null;
            String targetField = getEntityFieldReference(entityId, fieldComment);
            
            // 查找第二个实体引用（如果存在）
            Matcher valueMatcher = ENTITY_PATTERN.matcher(line.substring(entityMatcher.end()));
            if (valueMatcher.find()) {
                String valueEntityId = valueMatcher.group(1);
                String valueFieldComment = valueMatcher.group(3);
                targetValue = getEntityFieldReference(valueEntityId, valueFieldComment);
            } else if (line.contains("ブランク")) {
                targetValue = "\"\"";
            }

            if (targetValue != null && targetField != null) {
                // 对于实体字段，使用setter方法
                String setterField = targetField.replace("get", "set").replace("()", "");
                info.addAssignment(new GeneratedType2JavaInfo.Assignment(
                    setterField,
                    targetValue,
                    GeneratedType2JavaInfo.Assignment.AssignmentType.ENTITY_FIELD
                ));
            }
            return;
        }

        // 处理直接字段赋值
        Matcher directMatcher = DIRECT_PATTERN.matcher(line);
        if (directMatcher.find()) {
            String fieldName = directMatcher.group(1).trim();
            info.addAssignment(new GeneratedType2JavaInfo.Assignment(
                "D" + fieldName,  // 移除反斜杠
                "ブランク",
                GeneratedType2JavaInfo.Assignment.AssignmentType.DIRECT_FIELD
            ));
            return;
        }

        // 处理布尔字段赋值
        Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(line);
        if (booleanMatcher.find()) {
            String fieldName = booleanMatcher.group(1);
            info.addAssignment(new GeneratedType2JavaInfo.Assignment(
                fieldName,  // 不需要添加 *
                "'1'",  // 使用 '1' 表示 true
                GeneratedType2JavaInfo.Assignment.AssignmentType.BOOLEAN_FIELD
            ));
        }
    }

    private String getEntityFieldReference(String entityId, String fieldComment) {
        ClassInfo entityInfo = getEntityInfo(entityId);
        if (entityInfo != null) {
            FieldInfo fieldInfo = entityInfo.findFieldByComment(fieldComment);
            if (fieldInfo != null) {
                String instanceName = getInstanceName(entityInfo.getClassName());
                return instanceName + "." + fieldInfo.getGetMethod() + "()";
            }
        }
        logger.warn("Could not find field for entity {} with comment {}", entityId, fieldComment);
        return null;
    }

    private String getInstanceName(String className) {
        // 将类名转换为实例名（首字母小写）
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private ClassInfo getEntityInfo(String entityId) {
        if (!entityInfoMap.containsKey(entityId)) {
            String entityFile = entityFiles.get(entityId);
            if (entityFile != null) {
                try {
                    entityInfoMap.put(entityId, JavaFileReader.readJavaFileToModel(entityFile));
                } catch (IOException e) {
                    logger.error("Failed to read entity file for {}", entityId, e);
                    return null;
                }
            }
        }
        return entityInfoMap.get(entityId);
    }

    private String extractValue(String text) {
        logger.info("Extracting value from: {}", text);
        text = text.trim();
        
        // 处理实体字段引用
        Matcher entityMatcher = ENTITY_PATTERN.matcher(text);
        if (entityMatcher.find()) {
            String entityId = entityMatcher.group(1);
            String fieldComment = entityMatcher.group(3);
            logger.info("Found entity reference - id: {}, comment: {}", entityId, fieldComment);
            
            String reference = getEntityFieldReference(entityId, fieldComment);
            if (reference != null) {
                logger.info("Converted to entity reference: {}", reference);
                return reference;
            }
            logger.warn("Could not find entity field reference for: {} {}", entityId, fieldComment);
            return null;
        }

        // 处理直接字段引用
        Matcher directMatcher = DIRECT_PATTERN.matcher(text);
        if (directMatcher.find()) {
            String fieldName = directMatcher.group(1).trim();
            return "this." + fieldName;  // 移除反斜杠
        }

        // 处理布尔字段引用
        Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(text);
        if (booleanMatcher.find()) {
            return "this." + booleanMatcher.group(1);
        }

        // 处理 ブランク
        if (text.equals("ブランク")) {
            return "\"\"";
        }

        logger.info("Extracted value: {}", text);
        return text;
    }

    private String normalizeSpaces(String line) {
        if (line == null) return null;
        
        // 记录原始行用于日志
        String originalLine = line;
        
        // 1. 将全角空格转换为半角空格
        line = line.replace('　', ' ');
        
        // 2. 去除括号内的空格
        // 处理「」内的空格
        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        Pattern pattern = Pattern.compile("「([^」]*)」");
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            result.append(line.substring(lastPos, matcher.start()));
            String content = matcher.group(1).replaceAll("\\s+", "");
            result.append("「").append(content).append("」");
            lastPos = matcher.end();
        }
        if (lastPos < line.length()) {
            result.append(line.substring(lastPos));
        }
        line = result.toString();
        
        // 处理 () 内的空格
        result = new StringBuilder();
        lastPos = 0;
        pattern = Pattern.compile("\\(([^)]*)\\)");
        matcher = pattern.matcher(line);
        while (matcher.find()) {
            result.append(line.substring(lastPos, matcher.start()));
            String content = matcher.group(1).replaceAll("\\s+", "");
            result.append("(").append(content).append(")");
            lastPos = matcher.end();
        }
        if (lastPos < line.length()) {
            result.append(line.substring(lastPos));
        }
        line = result.toString();
        
        // 3. 将多个连续空格转换为单个空格
        line = line.replaceAll("\\s+", " ");
        
        // 4. 去除前后空格
        line = line.trim();
        
        if (!line.equals(originalLine)) {
            logger.debug("Space normalization - Original: [{}], Normalized: [{}]", originalLine, line);
        }
        
        return line;
    }
} 