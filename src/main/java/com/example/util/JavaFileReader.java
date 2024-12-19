package com.example.util;

import com.example.model.ClassInfo;
import com.example.model.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaFileReader {
    private static final Logger logger = LoggerFactory.getLogger(JavaFileReader.class);
    
    private static final Pattern CLASS_PATTERN = Pattern.compile("public class (\\w+)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\s*([^\\n]+)");
    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("private\\s+(\\w+)\\s+\\w+;");

    public static ClassInfo readJavaFileToModel(String filename) throws IOException {
        ClassInfo classInfo = new ClassInfo();
        StringBuilder commentBuffer = new StringBuilder();
        boolean inComment = false;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 处理类名
                if (line.contains("public class")) {
                    Matcher matcher = CLASS_PATTERN.matcher(line);
                    if (matcher.find()) {
                        classInfo.setClassName(matcher.group(1));
                        logger.debug("Found class name: {}", matcher.group(1));
                    }
                }
                
                // 处理注释开始
                if (line.startsWith("/**")) {
                    inComment = true;
                    commentBuffer.setLength(0);
                    continue;
                }
                
                // 处理注释结束
                if (line.startsWith("*/")) {
                    inComment = false;
                    processComment(commentBuffer.toString(), classInfo);
                    continue;
                }
                
                // 收集注释内容
                if (inComment && line.startsWith("*")) {
                    commentBuffer.append(line.substring(1).trim()).append("\n");
                }
                
                // 处理字段类型
                if (line.startsWith("private")) {
                    Matcher matcher = FIELD_TYPE_PATTERN.matcher(line);
                    if (matcher.find() && !classInfo.getFields().isEmpty()) {
                        classInfo.getFields().get(classInfo.getFields().size() - 1)
                            .setFieldType(matcher.group(1));
                        logger.debug("Found field type: {}", matcher.group(1));
                    }
                }
            }
        }
        
        return classInfo;
    }

    private static void processComment(String comment, ClassInfo classInfo) {
        Matcher matcher = FIELD_PATTERN.matcher(comment);
        if (matcher.find()) {
            String tableFieldName = matcher.group(1);
            String fieldComment = matcher.group(2).trim();
            classInfo.addField(new FieldInfo(tableFieldName, fieldComment));
            logger.debug("Added field - table name: {}, comment: {}", 
                tableFieldName, fieldComment);
        }
    }
} 