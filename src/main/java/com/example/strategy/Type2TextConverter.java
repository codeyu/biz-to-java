package com.example.strategy;

import com.example.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.*;
import java.io.IOException;

public class Type2TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type2TextConverter.class);
    
    private static final Pattern CONDITION_START = Pattern.compile("》【条件】(.+)のとき");
    private static final Pattern CONDITION_CONTINUE = Pattern.compile("^(または|かつ)、(.+)のとき");
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
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("》【条件】")) {
                if (currentInfo != null) {
                    results.add(currentInfo.generateCode());
                }
                currentInfo = new GeneratedType2JavaInfo();
                processConditionLine(line, currentInfo);
            } else if (line.startsWith("または、") || line.startsWith("かつ、")) {
                processContinuationLine(line, currentInfo);
            } else if (line.startsWith("項目「")) {
                processAssignmentLine(line, currentInfo);
            }
        }
        
        if (currentInfo != null) {
            results.add(currentInfo.generateCode());
        }
        
        return results;
    }

    private void processConditionLine(String line, GeneratedType2JavaInfo info) {
        Matcher matcher = CONDITION_START.matcher(line);
        if (matcher.find()) {
            String condition = matcher.group(1);
            processCondition(condition, info.getCondition(), null);
        }
    }

    private void processContinuationLine(String line, GeneratedType2JavaInfo info) {
        Matcher matcher = CONDITION_CONTINUE.matcher(line);
        if (matcher.find()) {
            String operator = matcher.group(1);
            String condition = matcher.group(2);
            processCondition(condition, info.getCondition(), operator);
        }
    }

    private void processCondition(String condition, GeneratedType2JavaInfo.Condition conditionInfo, String operator) {
        if (operator != null) {
            conditionInfo.setLogicalOperator(operator);
        }

        Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(condition);
        if (comparisonMatcher.find()) {
            String leftSide = comparisonMatcher.group(1).trim();
            String op = comparisonMatcher.group(2);
            String rightSide = comparisonMatcher.group(3).trim();
            
            conditionInfo.addPart(new GeneratedType2JavaInfo.ConditionPart(
                extractValue(leftSide),
                op,
                extractValue(rightSide)
            ));
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
            Matcher valueMatcher = ENTITY_PATTERN.matcher(line);
            // 查找第二个匹配（如果存在）
            if (valueMatcher.find() && valueMatcher.find()) {
                String valueEntityId = valueMatcher.group(1);
                String valueFieldComment = valueMatcher.group(3);
                targetValue = getEntityFieldReference(valueEntityId, valueFieldComment);
            } else if (line.contains("ブランク")) {
                targetValue = "\"\"";
            }

            if (targetValue != null) {
                String targetField = getEntityFieldReference(entityId, fieldComment);
                info.addAssignment(new GeneratedType2JavaInfo.Assignment(
                    targetField,
                    targetValue,
                    GeneratedType2JavaInfo.Assignment.AssignmentType.ENTITY_FIELD
                ));
            }
            return;
        }

        // 处理直接字段赋值
        Matcher directMatcher = DIRECT_PATTERN.matcher(line);
        if (directMatcher.find()) {
            String fieldName = directMatcher.group(1);
            info.addAssignment(new GeneratedType2JavaInfo.Assignment(
                "D\\" + fieldName,
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
                "*" + fieldName,
                "true",
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
        // 处理实体字段引用
        Matcher entityMatcher = ENTITY_PATTERN.matcher(text);
        if (entityMatcher.find()) {
            String entityId = entityMatcher.group(1);
            String fieldComment = entityMatcher.group(3);
            return getEntityFieldReference(entityId, fieldComment);
        }

        // 处理直接字段引用
        Matcher directMatcher = DIRECT_PATTERN.matcher(text);
        if (directMatcher.find()) {
            return "this." + directMatcher.group(1);
        }

        // 处理布尔字段引用
        Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(text);
        if (booleanMatcher.find()) {
            return "this." + booleanMatcher.group(1);
        }

        return text.trim();
    }
} 