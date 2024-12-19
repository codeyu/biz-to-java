package com.example.strategy;

import com.example.model.ClassInfo;
import com.example.model.FieldInfo;
import com.example.util.JavaFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // 先去除双引号
        line = removeQuotes(line);
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        logger.info("Processing line: {}", line);
        
        // 提取注释和实体ID
        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            logger.info("No comment found in line");
            return null;
        }
        String entityId = commentMatcher.group(1);
        String comment = commentMatcher.group(3).trim();
        logger.info("Extracted entityId: {}, comment: {}", entityId, comment);

        // 提取值
        Matcher valueMatcher = VALUE_PATTERN.matcher(line);
        if (!valueMatcher.find()) {
            logger.info("No value found in line");
            return null;
        }
        String value = valueMatcher.group(1);
        logger.info("Extracted value: {}", value);

        // 生成代码
        return generateCode(entityId, comment, value);
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

    private String generateCode(String entityId, String comment, String value) {
        logger.info("Generating code for entityId: {}, comment: {}, value: {}", entityId, comment, value);
        
        // 生成TODO注释
        if (comment == null) {
            logger.warn("Comment is null");
            return String.format("//TODO: Comment not found");
        }

        // 获取实例名
        String instanceName = getInstanceName(entityId);
        
        // 从实体类中获取正确的字段信息
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
