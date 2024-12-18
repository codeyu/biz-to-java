package com.example.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.util.StringCompareUtil;

public class Type1TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type1TextConverter.class);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("「[^」]*\\.(\\(([^)]+)\\))」");
    private static final Pattern VALUE_PATTERN = Pattern.compile("(ブランク|０|[0-9]+)");

    @Override
    public String convertLine(String line, List<String> entityLines) {
        logger.info("Processing line: {}", line);
        
        // 提取注释
        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (!commentMatcher.find()) {
            logger.info("No comment found in line");
            return null;
        }
        String comment = commentMatcher.group(2).trim();
        logger.info("Extracted comment: {}", comment);

        // 提取值
        Matcher valueMatcher = VALUE_PATTERN.matcher(line);
        if (!valueMatcher.find()) {
            logger.info("No value found in line");
            return null;
        }
        String value = valueMatcher.group(1);
        logger.info("Extracted value: {}", value);

        // 在实体类中查找对应字段
        String fieldInfo = findFieldByComment(comment, entityLines);
        if (fieldInfo == null) {
            logger.info("No matching field found");
            return null;
        }
        logger.info("Found field: {}", fieldInfo);

        // 生成Java代码
        String result = generateJavaCode(fieldInfo, value);
        logger.info("Generated code: {}", result);
        return result;
    }

    private String findFieldByComment(String comment, List<String> entityLines) {
        for (int i = 0; i < entityLines.size(); i++) {
            String line = entityLines.get(i);
            if (line.contains("*") && StringCompareUtil.compareJapaneseString(line, comment)) {
                // 查找下一个private字段声明
                for (int j = i; j < Math.min(i + 5, entityLines.size()); j++) {
                    if (entityLines.get(j).contains("private")) {
                        return entityLines.get(j);
                    }
                }
            }
        }
        return null;
    }

    private String generateJavaCode(String fieldInfo, String value) {
        Pattern fieldPattern = Pattern.compile("private\\s+\\w+\\s+(\\w+);");
        Matcher fieldMatcher = fieldPattern.matcher(fieldInfo);
        if (!fieldMatcher.find()) {
            return null;
        }
        String fieldName = fieldMatcher.group(1);

        String setValue = value.equals("ブランク") ? "\"\"" : value.equals("０") ? "0" : value;
        return String.format("testTable1BaseEntity.set%s(%s);", 
            capitalize(fieldName), setValue);
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
} 
