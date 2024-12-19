package com.example.strategy;

import com.example.model.ClassInfo;
import com.example.model.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Type1TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type1TextConverter.class);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("「[^」]*\\.(\\(([^)]+)\\))」");
    private static final Pattern VALUE_PATTERN = Pattern.compile("(ブランク|０|[0-9]+)(?=[^０-９]*[。．])");

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

        // 生成代码
        return generateCode(comment, value);
    }

    private String generateCode(String comment, String value) {
        // 生成TODO注释
        if (comment == null) {
            return String.format("//TODO: Comment not found");
        }

        // 生成代码
        String setValue;
        if (value.equals("ブランク")) {
            setValue = "\"\"";
        } else if (value.equals("０") || value.matches("[0-9]+")) {
            setValue = value.equals("０") ? "0" : value;
        } else {
            return String.format("//TODO: [%s] - Unsupported value: %s", comment, value);
        }

        // 添加注释和代码
        return String.format("// %s\ntestTable1BaseEntity.setTestField1(%s);", 
            comment, setValue);
    }
} 
