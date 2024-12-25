package com.example.strategy;

import com.example.model.ClassInfo;
import com.example.model.FieldInfo;
import com.example.util.JavaFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.model.GeneratedType1JavaInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.io.IOException;

public class Type1TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type1TextConverter.class);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("「([^」]*)\\.(\\(([^)]+)\\))」");
    private static final Pattern VALUE_PATTERN = Pattern.compile("(ブランク|０|[0-9]+)(?=[^０-９]*[。．])");
    private static final Pattern ENTITY_PATTERN = 
        Pattern.compile("項目「[^(]*?\\(([^()]*(?:\\([^()]*\\)[^()]*)*?)\\)[^.]*\\.(\\(([^)]+)\\))」");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern DATE_PATTERN = Pattern.compile("システム日付|��の日付");
    private static final Pattern SPACE_PATTERN = Pattern.compile("スペース|空白");
    private static final Pattern BLANK_PATTERN = Pattern.compile(
        "項目「([^」]*)\\.(\\(([^)]+)\\))」に[　\\s]*(ブランク|ﾌﾞﾗﾝｸ)[　\\s]*を代入します[。]?");
    
    private Map<String, String> entityInstances;  // 添加实例名映射
    private Map<String, String> entityFiles;  // 添加实体文件映射

    public void setEntityInstances(Map<String, String> entityInstances) {
        logger.info("Setting entity instances: {}", entityInstances);
        this.entityInstances = entityInstances;
    }

    public void setEntityFiles(Map<String, String> entityFiles) {
        logger.info("Setting entity files: {}", entityFiles);
        this.entityFiles = entityFiles;
    }

    @Override
    public String convertLine(String line, List<String> entityLines) {
        GeneratedType1JavaInfo result = new GeneratedType1JavaInfo();
        
        // 先去除双引号和句号
        line = removeQuotes(line);
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        logger.info("Processing line: {}", line);
        
        try {
            // 尝试各种处理方式
            if (tryProcessBlankAssignment(line, result) ||
                tryProcessNumberAssignment(line, result) ||
                tryProcessBooleanAssignment(line, result) ||
                tryProcessStringAssignment(line, result)) {
                return result.getCode();
            }
            
            // 如果所有处理方式都失败了
            result.setFailure(line, "No matching pattern found");
            return result.getCode();
            
        } catch (Exception e) {
            logger.error("Error converting line: {}", line, e);
            result.setFailure(line, "Exception: " + e.getMessage());
            return result.getCode();
        }
    }

    private boolean tryProcessBlankAssignment(String line, GeneratedType1JavaInfo result) {
        // 检查全角和半角的空值表示
        if (!line.contains("ブランク") && !line.contains("ﾌﾞﾗﾝｸ")) {
            return false;
        }

        Matcher matcher = ENTITY_PATTERN.matcher(line);
        if (!matcher.find()) {
            logger.debug("Line '{}' doesn't match entity pattern", line);
            result.setFailure(line, "No entity pattern found");
            return true;
        }

        String entityId = matcher.group(1);  // 提取第一个小括号内的完整内容作为实体ID
        String comment = matcher.group(3).trim();
        
        logger.debug("Found blank assignment - entityId: [{}], comment: [{}]", entityId, comment);
        
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(\"\");", instanceName, setterMethod));
        return true;
    }

    private boolean tryProcessSpaceAssignment(String line, GeneratedType1JavaInfo result) {
        if (!SPACE_PATTERN.matcher(line).find()) {
            return false;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            result.setFailure(line, "No comment pattern found");
            return true;
        }

        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(StringUtil.spaces(%d));", 
            instanceName, setterMethod, getSpaceCount(line)));
        return true;
    }

    private boolean tryProcessSystemDateAssignment(String line, GeneratedType1JavaInfo result) {
        if (!line.contains("システム日付")) {
            return false;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            result.setFailure(line, "No comment pattern found");
            return true;
        }

        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(DateUtil.getSystemDate());", 
            instanceName, setterMethod));
        return true;
    }

    private int getSpaceCount(String line) {
        // 从文本中提取空格数量，如果没有指定则返回默认值1
        Matcher matcher = Pattern.compile("(\\d+)\\s*(?:個|つ)?(?:の)?(?:スペース|空白)").matcher(line);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    private boolean tryProcessNumberAssignment(String line, GeneratedType1JavaInfo result) {
        if (!line.contains("＝")) {
            return false;
        }

        Matcher matcher = ENTITY_PATTERN.matcher(line);
        if (!matcher.find()) {
            logger.debug("Line '{}' doesn't match entity pattern", line);
            result.setFailure(line, "No entity pattern found");
            return true;
        }

        String entityId = matcher.group(1);
        String comment = matcher.group(3).trim();
        
        // 修改值的提取正则表达式
        Matcher valueMatcher = Pattern.compile("＝[　\\s]*([０0-9]+)").matcher(line);
        if (!valueMatcher.find()) {
            result.setFailure(line, "No value pattern found");
            return true;
        }
        
        String value = valueMatcher.group(1);
        if (value.equals("０")) {
            value = "0";
        }

        logger.debug("Found number assignment - entityId: [{}], comment: [{}], value: [{}]", 
            entityId, comment, value);

        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        
        // 对日期类型字段进行特殊处理
        if (fieldInfo.isDateType() && value.equals("0")) {
            result.setSuccessCode(String.format("%s.%s(null);", instanceName, setterMethod));
        } else {
            result.setSuccessCode(String.format("%s.%s(%s);", instanceName, setterMethod, value));
        }
        
        return true;
    }

    private boolean tryProcessBooleanAssignment(String line, GeneratedType1JavaInfo result) {
        if (!line.contains("'1'") && !line.contains("'0'")) {
            return false;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            result.setFailure(line, "No comment pattern found");
            return true;
        }

        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        
        String value = line.contains("'1'") ? "true" : "false";
        
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(%s);", instanceName, setterMethod, value));
        return true;
    }

    private boolean tryProcessEntityToEntityAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理实体间赋值，例如：項目「手袋(Ｌ０１).(常務コード)」に 項目「z3333(Ｌ０３).(常務コード) 」を代入します
        if (!line.contains("」に") || !line.contains("」を") || !line.contains("代入します")) {
            return false;
        }

        // 提取目标实体和字段
        Matcher targetMatcher = COMMENT_PATTERN.matcher(line);
        if (!targetMatcher.find()) {
            result.setFailure(line, "No target entity pattern found");
            return true;
        }

        String targetEntityId = targetMatcher.group(1);
        String targetComment = targetMatcher.group(3).trim();

        // 提取源实体和字段
        String remaining = line.substring(targetMatcher.end());
        Matcher sourceMatcher = COMMENT_PATTERN.matcher(remaining);
        if (!sourceMatcher.find()) {
            result.setFailure(line, "No source entity pattern found");
            return true;
        }

        String sourceEntityId = sourceMatcher.group(1);
        String sourceComment = sourceMatcher.group(3).trim();

        // 获取实例名和实体信息
        String targetInstanceName = getInstanceName(targetEntityId);
        String sourceInstanceName = getInstanceName(sourceEntityId);
        ClassInfo targetEntityInfo = getEntityInfo(targetEntityId);
        ClassInfo sourceEntityInfo = getEntityInfo(sourceEntityId);

        if (targetEntityInfo == null || sourceEntityInfo == null) {
            result.setFailure(line, "Entity info not found");
            return true;
        }

        // 获取字段信息
        FieldInfo targetField = targetEntityInfo.findFieldByComment(targetComment);
        FieldInfo sourceField = sourceEntityInfo.findFieldByComment(sourceComment);

        if (targetField == null || sourceField == null) {
            result.setFailure(line, "Field info not found");
            return true;
        }

        // 生成代码
        String code = String.format("%s.%s(%s.%s());",
            targetInstanceName,
            targetField.getSetMethod(),
            sourceInstanceName,
            sourceField.getGetMethod());
        
        result.setSuccessCode(code);
        return true;
    }

    private boolean tryProcessRightAlignedAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理右对齐赋值，例如：項目「手袋(Ｌ０１).(常務コード)」に 項目「z3333(Ｌ０３).(常務コード) 」を右詰で代入します
        if (!line.contains("右詰で")) {
            return false;
        }

        // 提取目标实体和字段
        Matcher targetMatcher = COMMENT_PATTERN.matcher(line);
        if (!targetMatcher.find()) {
            result.setFailure(line, "No target entity pattern found");
            return true;
        }

        String targetEntityId = targetMatcher.group(1);
        String targetComment = targetMatcher.group(3).trim();

        // 提取源实体和字段
        String remaining = line.substring(targetMatcher.end());
        Matcher sourceMatcher = COMMENT_PATTERN.matcher(remaining);
        if (!sourceMatcher.find()) {
            result.setFailure(line, "No source entity pattern found");
            return true;
        }

        String sourceEntityId = sourceMatcher.group(1);
        String sourceComment = sourceMatcher.group(3).trim();

        // 获取实例名和实体信息
        String targetInstanceName = getInstanceName(targetEntityId);
        String sourceInstanceName = getInstanceName(sourceEntityId);
        ClassInfo targetEntityInfo = getEntityInfo(targetEntityId);
        ClassInfo sourceEntityInfo = getEntityInfo(sourceEntityId);

        if (targetEntityInfo == null || sourceEntityInfo == null) {
            result.setFailure(line, "Entity info not found");
            return true;
        }

        // 获取字段信息
        FieldInfo targetField = targetEntityInfo.findFieldByComment(targetComment);
        FieldInfo sourceField = sourceEntityInfo.findFieldByComment(sourceComment);

        if (targetField == null || sourceField == null) {
            result.setFailure(line, "Field info not found");
            return true;
        }

        // 生成代码，添加右对齐处理
        String code = String.format("%s.%s(StringUtil.rightAlign(%s.%s()));",
            targetInstanceName,
            targetField.getSetMethod(),
            sourceInstanceName,
            sourceField.getGetMethod());
        
        result.setSuccessCode(code);
        return true;
    }

    private boolean tryProcessLeftAlignedAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理左对齐赋值
        if (!line.contains("左詰で")) {
            return false;
        }

        // ... 类似于右对齐的处理逻辑 ...
        // 但使用 StringUtil.leftAlign 方法

        return true;
    }

    private boolean tryProcessDateAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理日期赋值
        if (!line.contains("日付")) {
            return false;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            result.setFailure(line, "No comment pattern found");
            return true;
        }

        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        // 生成日期处理代码
        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(DateUtil.getCurrentDate());", 
            instanceName, setterMethod));
        return true;
    }

    private boolean tryProcessStringAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理字符串赋值，例如 "ABC" 或其他非空白、非数字的值
        if (line.contains("ブランク") || line.contains("＝")) {
            return false;  // 这些情况由其他方法处理
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            result.setFailure(line, "No comment pattern found");
            return true;
        }

        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        
        // 提取字符串值
        Matcher valueMatcher = Pattern.compile("\"([^\"]*)\"").matcher(line);
        if (!valueMatcher.find()) {
            result.setFailure(line, "No string value found");
            return true;
        }
        
        String value = valueMatcher.group(1);
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(\"%s\");", instanceName, setterMethod, value));
        return true;
    }

    private boolean tryProcessPaddingAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理补位赋值，例如：項目「手袋(Ｌ０１).(常務コード)」に"123"を5桁で代入します
        if (!line.contains("桁で")) {
            return false;
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            result.setFailure(line, "No comment pattern found");
            return true;
        }

        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        
        // 提取值和位数
        Matcher valueMatcher = STRING_PATTERN.matcher(line);
        Matcher digitMatcher = Pattern.compile("(\\d+)桁で").matcher(line);
        
        if (!valueMatcher.find() || !digitMatcher.find()) {
            result.setFailure(line, "Value or digit count not found");
            return true;
        }

        String value = valueMatcher.group(1);
        int digits = Integer.parseInt(digitMatcher.group(1));
        
        String instanceName = getInstanceName(entityId);
        ClassInfo entityInfo = getEntityInfo(entityId);
        
        if (entityInfo == null) {
            result.setFailure(line, "Entity info not found for " + entityId);
            return true;
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            result.setFailure(line, "Field not found for comment: " + comment);
            return true;
        }

        String setterMethod = fieldInfo.getSetMethod();
        result.setSuccessCode(String.format("%s.%s(StringUtil.padLeft(\"%s\", %d));", 
            instanceName, setterMethod, value, digits));
        return true;
    }

    private boolean tryProcessSubstringAssignment(String line, GeneratedType1JavaInfo result) {
        // 处理截取赋值，例如：項目「手袋(Ｌ０１).(常務コード)」に 項目「z3333(Ｌ０３).(常務コード)」の1文字目から3文字を代入します
        if (!line.contains("文字目から") || !line.contains("文字を")) {
            return false;
        }

        Matcher targetMatcher = ENTITY_PATTERN.matcher(line);
        if (!targetMatcher.find()) {
            result.setFailure(line, "Target entity not found");
            return true;
        }

        String targetEntityId = targetMatcher.group(1);
        String targetComment = targetMatcher.group(3).trim();

        String remaining = line.substring(targetMatcher.end());
        Matcher sourceMatcher = ENTITY_PATTERN.matcher(remaining);
        if (!sourceMatcher.find()) {
            result.setFailure(line, "Source entity not found");
            return true;
        }

        String sourceEntityId = sourceMatcher.group(1);
        String sourceComment = sourceMatcher.group(3).trim();

        // 提取起始位置和长度
        Matcher positionMatcher = Pattern.compile("(\\d+)文字目から(\\d+)文字を").matcher(line);
        if (!positionMatcher.find()) {
            result.setFailure(line, "Position and length not found");
            return true;
        }

        int startPos = Integer.parseInt(positionMatcher.group(1)) - 1; // 转换为0基索引
        int length = Integer.parseInt(positionMatcher.group(2));

        String targetInstanceName = getInstanceName(targetEntityId);
        String sourceInstanceName = getInstanceName(sourceEntityId);
        ClassInfo targetEntityInfo = getEntityInfo(targetEntityId);
        ClassInfo sourceEntityInfo = getEntityInfo(sourceEntityId);

        if (targetEntityInfo == null || sourceEntityInfo == null) {
            result.setFailure(line, "Entity info not found");
            return true;
        }

        FieldInfo targetField = targetEntityInfo.findFieldByComment(targetComment);
        FieldInfo sourceField = sourceEntityInfo.findFieldByComment(sourceComment);

        if (targetField == null || sourceField == null) {
            result.setFailure(line, "Field info not found");
            return true;
        }

        String code = String.format("%s.%s(StringUtil.substring(%s.%s(), %d, %d));",
            targetInstanceName,
            targetField.getSetMethod(),
            sourceInstanceName,
            sourceField.getGetMethod(),
            startPos,
            length);
        
        result.setSuccessCode(code);
        return true;
    }

    /**
     * 去除行首和行尾的双引号，保留行中间的双引号
     */
    private String removeQuotes(String line) {
        if (line == null) {
            return null;
        }
        // 去除首尾的双引号和句号
        return line.replaceAll("^[\"]|[\"。]$", "").trim();
    }

    private String generateCode(String entityId, String comment, String value) {
        logger.info("Generating code for entityId: {}, comment: {}, value: {}", entityId, comment, value);
        
        // 生成TODO注释
        if (comment == null) {
            logger.warn("Comment is null");
            return String.format("//TODO: Comment not found");
        }

        // 获取实例名
        String instanceName = getInstanceName(entityId);
        
        // 从实体类中获取的字段信息
        ClassInfo entityInfo = getEntityInfo(entityId);
        if (entityInfo == null) {
            logger.warn("Entity info not found for {}", entityId);
            return String.format("//TODO: Entity not found for %s", entityId);
        }
        
        FieldInfo fieldInfo = entityInfo.findFieldByComment(comment);
        if (fieldInfo == null) {
            logger.warn("Field not found for comment: {}", comment);
            return String.format("//TODO: Field not found for comment %s", comment);
        }

        // 生成代码
        String setValue;
        if (value.equals("ブランク")) {
            setValue = "\"\"";
        } else if (value.equals("０") || value.matches("[0-9]+")) {
            setValue = value.equals("０") ? "0" : value;
        } else {
            logger.warn("Unsupported value: {}", value);
            return String.format("//TODO: [%s] - Unsupported value: %s", comment, value);
        }

        String setterMethod = fieldInfo.getSetMethod();
        String result = String.format("// %s\n%s.%s(%s);", comment, instanceName, setterMethod, setValue);
        logger.info("Generated code: {}", result);
        return result;
    }

    private String getInstanceName(String entityId) {
        logger.info("Getting instance name for entityId: {}", entityId);
        if (entityInstances != null && entityInstances.containsKey(entityId)) {
            String instanceName = entityInstances.get(entityId);
            logger.info("Found configured instance name: {} for entityId: {}", instanceName, entityId);
            return instanceName;
        }
        // 默认实例名
        String defaultName = "testTable1BaseEntity";
        logger.info("Using default instance name: {} for entityId: {}", defaultName, entityId);
        return defaultName;
    }

    private ClassInfo getEntityInfo(String entityId) {
        String entityFile = entityFiles.get(entityId);
        if (entityFile != null) {
            try {
                return JavaFileReader.readJavaFileToModel(entityFile);
            } catch (IOException e) {
                logger.error("Failed to read entity file for {}", entityId, e);
            }
        }
        return null;
    }
} 
