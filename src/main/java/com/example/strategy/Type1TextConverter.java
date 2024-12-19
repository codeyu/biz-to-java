package com.example.strategy;

import com.example.model.ClassInfo;
import com.example.model.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

public class Type1TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type1TextConverter.class);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("「([^」]*)\\.(\\(([^)]+)\\))」");
    private static final Pattern VALUE_PATTERN = Pattern.compile("(ブランク|０|[0-9]+)(?=[^０-９]*[。．])");
    
    private Map<String, String> entityInstances;  // 添加实例名映射

    public void setEntityInstances(Map<String, String> entityInstances) {
        logger.info("Setting entity instances: {}", entityInstances);
        this.entityInstances = entityInstances;
    }

    @Override
    public String convertLine(String line, List<String> entityLines) {
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

    private String generateCode(String entityId, String comment, String value) {
        logger.info("Generating code for entityId: {}, comment: {}, value: {}", entityId, comment, value);
        
        // 生成TODO注释
        if (comment == null) {
            logger.warn("Comment is null");
            return String.format("//TODO: Comment not found");
        }

        // 获取实例名
        String instanceName = getInstanceName(entityId);
        logger.info("Using instance name: {} for entityId: {}", instanceName, entityId);

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

        String result = String.format("// %s\n%s.setTestField1(%s);", comment, instanceName, setValue);
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
} 
