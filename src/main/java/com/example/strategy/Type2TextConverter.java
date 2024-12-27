package com.example.strategy;

import com.example.model.*;
import com.example.util.JavaFileReader;
import com.example.util.LogicOperatorPostProcessor;
import com.example.util.VariableDefinitionReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.*;
import java.io.IOException;

/**
 * Type2 文本转换器
 * 使用 UTF-8 编码
 */
public class Type2TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type2TextConverter.class);
    
    private static final Pattern CONDITION_START = Pattern.compile("》【条件】(.+)のとき");
    private static final Pattern CONDITION_CONTINUE = Pattern.compile("^\\s*(または|かつ)、(.+)のとき");
    private static final Pattern ENTITY_PATTERN = 
        Pattern.compile("項目「[^(]*?\\(((?:[^()]+|\\([^()]*\\))+)\\)[^.]*\\.(\\(([^)]+)\\))」");
    private static final Pattern DIRECT_PATTERN = Pattern.compile("項目「\\s*([A-Z])\\\\([^」]+)\\s*」");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("項目「\\*([^」]+)」");
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(.+)(＝|≠)(.+)");
    private static final Pattern SIMPLE_ASSIGNMENT_PATTERN = 
        Pattern.compile("^項目「\\s*([^」]+?)\\s*」に\\s*(ブランク|'[01]')\\s*を代入します");
    private static final Pattern SIMPLE_EQUALS_PATTERN =
        Pattern.compile("^項目「[^(]*?\\(((?:[^()]+|\\([^()]*\\))+)\\)[^.]*\\.(\\(([^)]+)\\))」＝\\s*([０0-9]+)");

    private Map<String, ClassInfo> entityInfoMap;
    private Map<String, String> entityFiles;
    private Map<String, String> entityInstances;
    private boolean enableLogicConversion = false;
    private LogicOperatorPostProcessor logicProcessor;

    public void setEntityFiles(Map<String, String> entityFiles) {
        this.entityFiles = entityFiles;
        this.entityInfoMap = new HashMap<>();
    }

    public void setEntityInstances(Map<String, String> entityInstances) {
        this.entityInstances = entityInstances;
    }

    public void setEnableLogicConversion(boolean enable) {
        this.enableLogicConversion = enable;
        if (enable && logicProcessor == null) {
            logicProcessor = new LogicOperatorPostProcessor();
        }
    }

    public void setDefineFile(String defineFile) {
        try {
            Map<String, VariableDefinition> definitions = 
                VariableDefinitionReader.readDefinitions(defineFile);
            if (logicProcessor != null) {
                logicProcessor.setVariableDefinitions(definitions);
            }
        } catch (IOException e) {
            logger.error("Failed to read define file: {}", defineFile, e);
        }
    }

    @Override
    public String convertLine(String line, List<String> entityLines) {
        // 实现单行转换（如果需要）
        return null;
    }

    // 添加一个内部类来保存带序号的文本行
    private static class TextLine {
        final int lineNumber;
        final String content;
        boolean processed;  // 标记是否已处理

        TextLine(int lineNumber, String content) {
            this.lineNumber = lineNumber;
            this.content = content;
            this.processed = false;
        }
    }

    public List<String> convertFile(List<String> lines) {
        // 1. 初始化带序号的行列表
        List<TextLine> textLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String cleanedLine = removeQuotes(lines.get(i));
            textLines.add(new TextLine(i, cleanedLine));
        }

        // 2. 存储生成的代码
        List<String> generatedCode = new ArrayList<>();
        
        // 3. 处理条件块
        GeneratedType2JavaInfo currentInfo = null;

        for (int i = 0; i < textLines.size(); i++) {
            TextLine line = textLines.get(i);
            String normalizedLine = normalizeSpaces(line.content);
            String trimmed = normalizedLine.trim();

            // 新的条件块开始
            if (trimmed.startsWith("》【条件】")) {
                // 处理前一个条件块
                if (currentInfo != null) {
                    String code = currentInfo.generateCode();
                    if (code != null && !code.trim().isEmpty()) {
                        generatedCode.add(code);
                    }
                }
                // 创建新的条件块
                currentInfo = new GeneratedType2JavaInfo();
                processConditionLine(trimmed, currentInfo);
            }
            // 条件块的继续条件（または、或かつ、）
            else if (trimmed.startsWith("または、") || trimmed.startsWith("かつ、")) {
                if (currentInfo != null) {
                    processContinuationLine(trimmed, currentInfo);
                }
            }
            // 条件块内的语句
            else if (!trimmed.isEmpty()) {
                if (currentInfo != null) {
                    processAssignmentLine(trimmed, currentInfo);
                } else {
                    // 如果不在条件块内，创建一个新的条件块
                    currentInfo = new GeneratedType2JavaInfo();
                    processAssignmentLine(trimmed, currentInfo);
                }
            }
        }

        // 处理最后一个条件块
        if (currentInfo != null) {
            String code = currentInfo.generateCode();
            if (code != null && !code.trim().isEmpty()) {
                generatedCode.add(code);
            }
        }

        // 4. 如果启用了逻辑转换，进行后处理
        if (enableLogicConversion && logicProcessor != null) {
            return processLogicConversion(generatedCode);
        }
        
        return generatedCode;
    }

    private String processStandaloneAssignment(String line) {
        // 处理简单赋值（带"に...代入します"）
        Matcher simpleAssignMatcher = SIMPLE_ASSIGNMENT_PATTERN.matcher(line);
        if (simpleAssignMatcher.find()) {
            String fieldName = simpleAssignMatcher.group(1).trim();
            // 处理 D\AABB 格式的字段名
            Matcher directMatcher = DIRECT_PATTERN.matcher(fieldName);
            if (directMatcher.find()) {
                String prefix = directMatcher.group(1);  // 获取前缀（如 "D"）
                String name = directMatcher.group(2);    // 获取名称（如 "AABB"）
                fieldName = prefix + name;              // 组合（如 "DAABB"）
                logger.info("Processed field name: prefix=[{}], name=[{}], result=[{}]", 
                    prefix, name, fieldName);
            } else {
                fieldName = fieldName.replace("\\", "");
            }
            return String.format("this.%s = \"\";", fieldName);
        }
        
        // 处理简单赋值（带"＝"）
        Matcher equalsAssignMatcher = SIMPLE_EQUALS_PATTERN.matcher(line);
        if (equalsAssignMatcher.find()) {
            String entityId = equalsAssignMatcher.group(1);
            String value = equalsAssignMatcher.group(4);
            String instanceName = getInstanceName(entityId);
            return String.format("%s.setTestField1(%s);", instanceName, value);
        }
        
        return null;
    }

    private void processConditionLine(String line, GeneratedType2JavaInfo info) {
        logger.info("Processing condition line: {}", line);
        try {
            Matcher matcher = CONDITION_START.matcher(line);
            if (matcher.find()) {
                String condition = matcher.group(1);
                logger.info("Extracted condition: {}", condition);
                processCondition(condition, info.getCondition(), null);
            } else {
                info.handleFailure(line, "Failed to match condition pattern");
            }
        } catch (Exception e) {
            logger.error("Error processing condition line: {}", line, e);
            info.handleFailure(line, "Exception: " + e.getMessage());
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

    private boolean processCondition(String condition, GeneratedType2JavaInfo.Condition conditionInfo, String operator) {
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
            
            logger.info("Extracted comparison - left: {}, operator: {}, right: {}", 
                leftSide, op, rightSide);
            
            String leftValue = extractValue(leftSide);
            String rightValue = extractValue(rightSide);
            
            logger.info("Converted values - left: {}, right: {}", leftValue, rightValue);
            
            if (leftValue != null && rightValue != null) {
                conditionInfo.addPart(new GeneratedType2JavaInfo.ConditionPart(
                    leftValue,
                    op,
                    rightValue,
                    false  // 正常的条件
                ));
                return true;
            } else {
                logger.error("Failed to extract values - left: [{}], right: [{}]", 
                    leftValue, rightValue);
                // 添加错误的条件部分
                conditionInfo.addPart(new GeneratedType2JavaInfo.ConditionPart(
                    leftSide,  // 原始的左值
                    op,        // 原始的操作符
                    rightSide, // 原始的右值
                    true      // 标记为错误
                ));
                return false;
            }
        } else {
            logger.error("Failed to match comparison pattern in condition: [{}]", condition);
            return false;
        }
    }

    private String getEntityFieldReference(String entityId, String fieldComment) {
        // 记录日志，帮助调试
        logger.info("Getting entity field reference - entityId: [{}], fieldComment: [{}]", 
            entityId, fieldComment);

        ClassInfo entityInfo = getEntityInfo(entityId);
        if (entityInfo != null) {
            FieldInfo fieldInfo = entityInfo.findFieldByComment(fieldComment);
            if (fieldInfo != null) {
                String instanceName = getInstanceName(entityId);
                String reference = instanceName + "." + fieldInfo.getGetMethod() + "()";
                logger.info("Generated reference: [{}]", reference);
                return reference;
            } else {
                logger.error("Field not found - entityId: [{}], fieldComment: [{}]", 
                    entityId, fieldComment);
            }
        } else {
            logger.error("Entity info not found for ID: [{}]", entityId);
        }
        return null;
    }

    private String getInstanceName(String entityId) {
        // 从配置中获取实例名
        if (entityInstances != null && entityInstances.containsKey(entityId)) {
            String instanceName = entityInstances.get(entityId);
            logger.info("Found configured instance name '{}' for entity ID '{}'", instanceName, entityId);
            return instanceName;
        }

        // 如果没有找到配置的实例名，记录错误并返回一个默认值
        logger.error("No instance name configured for entity ID: {}", entityId);
        return "defaultInstance"; // 或者抛出异常，取决于你的需求
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
        
        // 处理体字段引用
        Matcher entityMatcher = ENTITY_PATTERN.matcher(text);
        if (entityMatcher.find()) {
            String entityId = entityMatcher.group(1);  // 获取第一个号内的实体ID
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
            String prefix = directMatcher.group(1);  // 获取前缀（如 "D"）
            String name = directMatcher.group(2);    // 获取名称（如 "AABB"）
            String fieldName = prefix + name;        // 组合（如 "DAABB"）
            logger.info("Found direct field reference: prefix=[{}], name=[{}], result=[{}]", 
                prefix, name, fieldName);
            return "this." + fieldName;
        }

        // 处理布尔字段引用
        Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(text);
        if (booleanMatcher.find()) {
            return "this." + booleanMatcher.group(1);
        }

        // 处理 ブランク
        if (text.equals("ブランク") || text.equals("ﾌﾞﾗﾝｸ")) {
            return "\"\"";
        }

        logger.info("Extracted value: {}", text);
        return text;
    }

    private String normalizeSpaces(String line) {
        if (line == null) return null;
        
        // 记录原始行用于日志
        String originalLine = line;
        
        // 1. 将全角格转换为半角空格
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
        
        // 3. 将多连续空格转换为单个空格
        line = line.replaceAll("\\s+", " ");
        
        // 4. 去除前后空格
        line = line.trim();
        
        if (!line.equals(originalLine)) {
            logger.debug("Space normalization - Original: [{}], Normalized: [{}]", originalLine, line);
        }
        
        return line;
    }

    private void processAssignmentLine(String line, GeneratedType2JavaInfo currentInfo) {
        logger.info("Processing assignment line: {}", line);
        
        try {
            // 尝试各种处理方式
            if (tryProcessEntityAssignment(line, currentInfo) ||
                tryProcessDirectAssignment(line, currentInfo) ||
                tryProcessBooleanAssignment(line, currentInfo) ||
                tryProcessNumberAssignment(line, currentInfo)) {
                return;
            }
            
            // 如果所有处理方式都失败了
            currentInfo.handleFailure(line, "No matching pattern found");
            
        } catch (Exception e) {
            logger.error("Error processing line: {}", line, e);
            currentInfo.handleFailure(line, "Exception: " + e.getMessage());
        }
    }

    private boolean tryProcessEntityAssignment(String line, GeneratedType2JavaInfo currentInfo) {
        if (line.contains("」に") && line.contains("」を") && line.contains("代入します")) {
            try {
                return processEntityToEntityAssignment(line, currentInfo);
            } catch (Exception e) {
                logger.error("Failed to process entity assignment", e);
                currentInfo.handleFailure(line, "Exception: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean tryProcessDirectAssignment(String line, GeneratedType2JavaInfo currentInfo) {
        if (line.contains("ブランク") || line.contains("ﾌﾞﾗﾝｸ")) {
            try {
                Matcher directMatcher = DIRECT_PATTERN.matcher(line);
                if (directMatcher.find()) {
                    String prefix = directMatcher.group(1);
                    String name = directMatcher.group(2);
                    String fieldName = prefix + name.replace("\\", "").trim();
                    currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                        fieldName,
                        "\"\"",
                        GeneratedType2JavaInfo.Assignment.AssignmentType.DIRECT_FIELD
                    ));
                    return true;
                }
                currentInfo.handleFailure(line, "Failed to match direct field pattern");
            } catch (Exception e) {
                logger.error("Failed to process direct assignment", e);
                currentInfo.handleFailure(line, "Exception: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean tryProcessBooleanAssignment(String line, GeneratedType2JavaInfo currentInfo) {
        if (line.contains("*") && line.contains("'1'")) {
            try {
                Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(line);
                if (booleanMatcher.find()) {
                    String fieldName = booleanMatcher.group(1).trim();
                    currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                        fieldName,
                        "'1'",
                        GeneratedType2JavaInfo.Assignment.AssignmentType.BOOLEAN_FIELD
                    ));
                    return true;
                }
                currentInfo.handleFailure(line, "Failed to match boolean field pattern");
            } catch (Exception e) {
                logger.error("Failed to process boolean assignment", e);
                currentInfo.handleFailure(line, "Exception: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean tryProcessNumberAssignment(String line, GeneratedType2JavaInfo currentInfo) {
        if (line.contains("＝")) {
            try {
                Matcher equalsAssignMatcher = SIMPLE_EQUALS_PATTERN.matcher(line);
                if (equalsAssignMatcher.find()) {
                    String entityId = equalsAssignMatcher.group(1);
                    String fieldComment = equalsAssignMatcher.group(3);
                    String value = equalsAssignMatcher.group(4);
                    
                    String instanceName = getInstanceName(entityId);
                    ClassInfo entityInfo = getEntityInfo(entityId);
                    
                    if (entityInfo != null) {
                        FieldInfo field = entityInfo.findFieldByComment(fieldComment);
                        if (field != null) {
                            String setterMethod = field.getSetMethod();
                            currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                                instanceName + "." + setterMethod,
                                value,
                                GeneratedType2JavaInfo.Assignment.AssignmentType.ENTITY_FIELD
                            ));
                            return true;
                        }
                        currentInfo.handleFailure(line, "Field not found: " + fieldComment);
                        return false;
                    }
                    currentInfo.handleFailure(line, "Entity not found: " + entityId);
                    return false;
                }
                currentInfo.handleFailure(line, "Failed to match number assignment pattern");
            } catch (Exception e) {
                logger.error("Failed to process number assignment", e);
                currentInfo.handleFailure(line, "Exception: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean processEntityToEntityAssignment(String line, GeneratedType2JavaInfo currentInfo) {
        // 提取源实体和目���实体
        Matcher sourceMatcher = ENTITY_PATTERN.matcher(line);
        if (!sourceMatcher.find()) {
            currentInfo.handleFailure(line, "Failed to match source entity pattern");
            return false;
        }

        String targetEntityId = sourceMatcher.group(1);
        String targetFieldComment = sourceMatcher.group(3);
        
        // 查找第二个实体引用
        String remaining = line.substring(sourceMatcher.end());
        Matcher targetMatcher = ENTITY_PATTERN.matcher(remaining);
        if (!targetMatcher.find()) {
            currentInfo.handleFailure(line, "Failed to match target entity pattern");
            return false;
        }

        String sourceEntityId = targetMatcher.group(1);
        String sourceFieldComment = targetMatcher.group(3);
        
        try {
            String targetInstanceName = getInstanceName(targetEntityId);
            String sourceInstanceName = getInstanceName(sourceEntityId);
            
            ClassInfo targetEntityInfo = getEntityInfo(targetEntityId);
            ClassInfo sourceEntityInfo = getEntityInfo(sourceEntityId);
            
            if (targetEntityInfo == null || sourceEntityInfo == null) {
                currentInfo.handleFailure(line, "Entity info not found");
                return false;
            }

            FieldInfo targetField = targetEntityInfo.findFieldByComment(targetFieldComment);
            FieldInfo sourceField = sourceEntityInfo.findFieldByComment(sourceFieldComment);
            
            if (targetField == null || sourceField == null) {
                currentInfo.handleFailure(line, "Field info not found");
                return false;
            }

            String sourceValue = sourceInstanceName + "." + sourceField.getGetMethod() + "()";
            String setterMethod = targetField.getSetMethod();
            
            currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                targetInstanceName + "." + setterMethod,
                sourceValue,
                GeneratedType2JavaInfo.Assignment.AssignmentType.ENTITY_FIELD
            ));
            
            return true;
        } catch (Exception e) {
            logger.error("Error processing entity assignment", e);
            currentInfo.handleFailure(line, "Exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * 去除行首和行尾的双引号，保留行中间的双引号
     */
    private String removeQuotes(String line) {
        if (line == null) return null;
        
        // 记录原始行用于日志
        String originalLine = line;
        
        // 去除前后空格
        line = line.trim();
        
        // 处理开头的双引号
        if (line.startsWith("\"")) {
            line = line.substring(1);
        }
        
        // 处理结尾的双引号
        if (line.endsWith("\"")) {
            line = line.substring(0, line.length() - 1);
        }
        
        if (!line.equals(originalLine)) {
            logger.debug("Quote removal - Original: [{}], Cleaned: [{}]", originalLine, line);
        }
        
        return line;
    }

    private List<String> processLogicConversion(List<String> generatedCodeList) {
        logger.info("Logic conversion is enabled, processing {} lines", generatedCodeList.size());
        
        // 确保实体信息已加载
        if (entityInfoMap.isEmpty() && entityFiles != null) {
            try {
                for (Map.Entry<String, String> entry : entityFiles.entrySet()) {
                    String entityId = entry.getKey();
                    String filePath = entry.getValue();
                    entityInfoMap.put(entityId, JavaFileReader.readJavaFileToModel(filePath));
                    logger.debug("Loaded entity info for {}", entityId);
                }
            } catch (IOException e) {
                logger.error("Failed to load entity files", e);
            }
        }

        // 创建处理器并设置必要的信息
        LogicOperatorPostProcessor logicProcessor = new LogicOperatorPostProcessor();
        logicProcessor.setEntityInfos(entityInfoMap);
        
        List<String> processedCode = new ArrayList<>();
        for (String code : generatedCodeList) {
            try {
                String processed = logicProcessor.process(code);
                logger.info("Original code: [{}]", code);
                logger.info("Processed code: [{}]", processed);
                processedCode.add(processed);
            } catch (Exception e) {
                logger.error("Error processing code: {}", code, e);
                processedCode.add(code);  // 出错时保留原代码
            }
        }
        return processedCode;
    }

    private void processLogicConversion(String code, Map<String, String> logicOperatorMapping) {
        if (!enableLogicConversion) {
            return;
        }

        LogicOperatorPostProcessor processor = new LogicOperatorPostProcessor();
        processor.setVariableDefinitions(variableDefinitions);
        
        // 添加这行，设置 entityInfos
        processor.setEntityInfos(entityInfoMap);
        
        String processedCode = processor.process(code);
        if (!code.equals(processedCode)) {
            logger.info("Logic conversion applied:\nBefore: {}\nAfter: {}", code, processedCode);
            // 处理逻辑运算符映射
            for (Map.Entry<String, String> entry : logicOperatorMapping.entrySet()) {
                processedCode = processedCode.replace(entry.getKey(), entry.getValue());
            }
            convertedCode.append(processedCode).append("\n");
        } else {
            convertedCode.append(code).append("\n");
        }
    }
} 