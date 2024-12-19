package com.example.strategy;

import com.example.model.ClassInfo;
import com.example.model.GeneratedType1JavaInfo;
import com.example.util.JavaFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Type1TextConverter implements TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type1TextConverter.class);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("「[^」]*\\.(\\(([^)]+)\\))」");
    private static final Pattern VALUE_PATTERN = Pattern.compile("(ブランク|０|[0-9]+)(?=[^０-９]*[。．])");

    private ClassInfo entityInfo;

    @Override
    public String convertLine(String line, List<String> entityLines) {
        // 延迟加载实体类信息
        if (entityInfo == null) {
            try {
                entityInfo = JavaFileReader.readJavaFileToModel("input/TestTable1BaseEntity.java");
            } catch (IOException e) {
                logger.error("Failed to read entity file", e);
                return null;
            }
        }

        // 解析输入行
        GeneratedType1JavaInfo info = parseLine(line);
        if (info == null) {
            return null;
        }

        // 查找匹配的字段
        info.setMatchedField(entityInfo.findFieldByComment(info.getMatchedComment()));

        // 生成代码
        return info.generateCode();
    }

    private GeneratedType1JavaInfo parseLine(String line) {
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

        return new GeneratedType1JavaInfo(comment, value);
    }

    public List<GeneratedType1JavaInfo> convertFile(List<String> inputLines) {
        List<GeneratedType1JavaInfo> results = new ArrayList<>();
        
        for (String line : inputLines) {
            GeneratedType1JavaInfo info = parseLine(line);
            if (info != null) {
                if (entityInfo == null) {
                    try {
                        entityInfo = JavaFileReader.readJavaFileToModel("input/TestTable1BaseEntity.java");
                    } catch (IOException e) {
                        logger.error("Failed to read entity file", e);
                        continue;
                    }
                }
                info.setMatchedField(entityInfo.findFieldByComment(info.getMatchedComment()));
                results.add(info);
            }
        }
        
        return results;
    }
} 
