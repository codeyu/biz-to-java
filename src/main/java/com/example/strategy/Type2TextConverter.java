package com.example.strategy;

import com.example.model.*;
import com.example.util.JavaFileReader;
import com.example.util.LogicOperatorPostProcessor;

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
    private static final Pattern SIMPLE_ASSIGNMENT_PATTERN = 
        Pattern.compile("^項目「\\s*([^」]+?)\\s*」に\\s*(ブランク|'[01]')\\s*を代入します");
    private static final Pattern SIMPLE_EQUALS_PATTERN =
        Pattern.compile("^項目「([^」]*)\\.(\\(([^)]+)\\))」＝\\s*([０0-9]+)");

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
        // 1. 初始化带序号的行列表，同时处理双引号
        List<TextLine> textLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String cleanedLine = removeQuotes(lines.get(i));
            textLines.add(new TextLine(i, cleanedLine));
        }

        // 2. 存储生成的代码，使用TreeMap保证顺序
        TreeMap<Integer, String> generatedCode = new TreeMap<>();
        
        // 3. 首先处理独立的赋值语句（以"項目"开头的行）
        for (TextLine line : textLines) {
            String trimmed = line.content.trim();
            if (trimmed.startsWith("項目「") && !line.content.startsWith(" ") && !line.content.startsWith("　")) {
                String code = processStandaloneAssignment(normalizeSpaces(trimmed));
                if (code != null) {
                    generatedCode.put(line.lineNumber, code);
                    line.processed = true;
                }
            }
        }

        // 4. 处理条件块
        GeneratedType2JavaInfo currentInfo = null;
        int conditionStartLine = -1;

        for (TextLine line : textLines) {
            if (line.processed) continue;  // 跳过已处理的行
            
            String normalizedLine = normalizeSpaces(line.content);
            String trimmed = normalizedLine.trim();

            if (trimmed.startsWith("》【条件】")) {
                if (currentInfo != null) {
                    generatedCode.put(conditionStartLine, currentInfo.generateCode());
                }
                currentInfo = new GeneratedType2JavaInfo();
                conditionStartLine = line.lineNumber;
                processConditionLine(trimmed, currentInfo);
                line.processed = true;
            }
            else if (line.content.startsWith(" ") || line.content.startsWith("　")) {
                if (!line.processed && currentInfo != null) {
                    if (trimmed.startsWith("または、") || trimmed.startsWith("かつ、")) {
                        processContinuationLine(trimmed, currentInfo);
                    } else if (trimmed.startsWith("項目「")) {
                        processAssignmentLine(trimmed, currentInfo);
                    }
                    line.processed = true;
                }
            }
        }

        // 处理最后一个条件块
        if (currentInfo != null) {
            generatedCode.put(conditionStartLine, currentInfo.generateCode());
        }

        // 5. 按顺序返回生成的代码
        List<String> generatedCodeList = new ArrayList<>(generatedCode.values());
        
        // 如果启用了逻辑转换，进行后处理
        if (enableLogicConversion && logicProcessor != null) {
            List<String> processedCode = new ArrayList<>();
            for (String code : generatedCodeList) {
                try {
                    String processed = logicProcessor.process(code);
                    processedCode.add(processed);
                } catch (Exception e) {
                    logger.error("Error processing code: {}", code, e);
                    processedCode.add(code);  // 出错时保留原代码
                }
            }
            return processedCode;
        }
        
        return generatedCodeList;
    }

    private String processStandaloneAssignment(String line) {
        // 处理简单赋值（带"に...代入します"）
        Matcher simpleAssignMatcher = SIMPLE_ASSIGNMENT_PATTERN.matcher(line);
        if (simpleAssignMatcher.find()) {
            String fieldName = simpleAssignMatcher.group(1).trim().replace("\\", "");
            logger.info("Found field name with spaces: [{}], after trim: [{}]", 
                simpleAssignMatcher.group(1), fieldName);
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

    private String getEntityFieldReference(String entityId, String fieldComment) {
        ClassInfo entityInfo = getEntityInfo(entityId);
        if (entityInfo != null) {
            FieldInfo fieldInfo = entityInfo.findFieldByComment(fieldComment);
            if (fieldInfo != null) {
                String instanceName = getInstanceName(entityId);
                logger.info("Using instance name '{}' for entity ID '{}'", instanceName, entityId);
                return instanceName + "." + fieldInfo.getGetMethod() + "()";
            }
        }
        logger.warn("Could not find field for entity {} with comment {}", entityId, fieldComment);
        return null;
    }

    private String getInstanceName(String entityId) {
        // 从配置中获取实例名，如果没有配置则使用默认的命名规则
        if (entityInstances != null && entityInstances.containsKey(entityId)) {
            String instanceName = entityInstances.get(entityId);
            logger.info("Found configured instance name '{}' for entity ID '{}'", instanceName, entityId);
            return instanceName;
        }
        // 默认命名规则（首字母小写）
        String className = entityInfoMap.get(entityId).getClassName();
        String defaultName = Character.toLowerCase(className.charAt(0)) + className.substring(1);
        logger.info("Using default instance name '{}' for entity ID '{}'", defaultName, entityId);
        return defaultName;
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

    private void processAssignmentLine(String line, GeneratedType2JavaInfo currentInfo) {
        logger.info("Processing assignment line: {}", line);
        
        // 处理实体字段赋值
        Matcher entityMatcher = ENTITY_PATTERN.matcher(line);
        if (entityMatcher.find()) {
            String entityId = entityMatcher.group(1);
            String fieldComment = entityMatcher.group(3);
            logger.info("Found entity assignment - id: {}, comment: {}", entityId, fieldComment);
            
            // 查找目标值
            String targetValue = null;
            String targetField = getEntityFieldReference(entityId, fieldComment);
            
            // 查找第二个实体引用（如果存在）
            Matcher valueMatcher = ENTITY_PATTERN.matcher(line.substring(entityMatcher.end()));
            if (valueMatcher.find()) {
                String valueEntityId = valueMatcher.group(1);
                String valueFieldComment = valueMatcher.group(3);
                targetValue = getEntityFieldReference(valueEntityId, valueFieldComment);
                logger.info("Found entity value reference - id: {}, comment: {}", valueEntityId, valueFieldComment);
            } else if (line.contains("ブランク")) {
                targetValue = "\"\"";
                logger.info("Found blank value assignment");
            }

            if (targetValue != null && targetField != null) {
                // 对于实体字段，使用setter方法
                String setterField = targetField.replace("get", "set").replace("()", "");
                currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                    setterField,
                    targetValue,
                    GeneratedType2JavaInfo.Assignment.AssignmentType.ENTITY_FIELD
                ));
                logger.info("Added entity field assignment: {} = {}", setterField, targetValue);
            }
            return;
        }

        // 处理直接字段赋值
        Matcher directMatcher = DIRECT_PATTERN.matcher(line);
        if (directMatcher.find()) {
            String fieldName = directMatcher.group(1).trim();
            logger.info("Found direct field assignment: {}", fieldName);
            currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                fieldName,  // 不再添加 "D" 前缀，因为字段名已经包含了
                "ブランク",
                GeneratedType2JavaInfo.Assignment.AssignmentType.DIRECT_FIELD
            ));
            return;
        }

        // 处理布尔字段赋值
        Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(line);
        if (booleanMatcher.find()) {
            String fieldName = booleanMatcher.group(1);
            logger.info("Found boolean field assignment: {}", fieldName);
            currentInfo.addAssignment(new GeneratedType2JavaInfo.Assignment(
                fieldName,  // 不需要添加 *
                "'1'",  // 使用 '1' 表示 true
                GeneratedType2JavaInfo.Assignment.AssignmentType.BOOLEAN_FIELD
            ));
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
} 