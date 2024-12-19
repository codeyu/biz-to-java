package com.example;

import com.example.config.ConverterConfig;
import com.example.factory.ConverterFactory;
import com.example.strategy.TextConverter;
import com.example.strategy.Type1TextConverter;
import com.example.strategy.Type2TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TextToJavaConverter {
    private static final Logger logger = LoggerFactory.getLogger(TextToJavaConverter.class);
    
    public static void main(String[] args) {
        try {
            // 解析参数，忽略以 -D 开头的系统属性
            String converterType = "type1"; // 默认值
            for (String arg : args) {
                if (!arg.startsWith("-D")) {
                    converterType = arg;
                    break;
                }
            }
            
            logger.info("Using converter type: {}", converterType);
            ConverterConfig config = new ConverterConfig(converterType);
            TextConverter converter = ConverterFactory.getConverter(config.getConverterType());
            
            // 如果是 Type2 转换器，设置实体文件映射
            if (converter instanceof Type2TextConverter) {
                Type2TextConverter type2Converter = (Type2TextConverter) converter;
                type2Converter.setEntityFiles(config.getEntityFiles());
                type2Converter.setEntityInstances(config.getEntityInstances());
            } else if (converter instanceof Type1TextConverter) {
                Type1TextConverter type1Converter = (Type1TextConverter) converter;
                type1Converter.setEntityFiles(config.getEntityFiles());
                type1Converter.setEntityInstances(config.getEntityInstances());
                logger.info("Set entity instances for Type1 converter: {}", config.getEntityInstances());
            }
            
            // 读取输入文本
            List<String> inputLines = readFile(config.getInputFile());
            
            // 创建输出目录
            new File(config.getOutputFile()).getParentFile().mkdirs();
            
            // 写入生成的代码
            try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(config.getOutputFile()), "UTF-8"))) {
                
                // 写入文件头注释
                writer.println("/**");
                writer.println(" * Generated code from " + config.getInputFile());
                writer.println(" */");
                writer.println();

                if (converter instanceof Type2TextConverter) {
                    List<String> results = ((Type2TextConverter) converter).convertFile(inputLines);
                    for (String code : results) {
                        writer.println(code);
                    }
                } else {
                    // Type1 转换器处理
                    for (String line : inputLines) {
                        String code = converter.convertLine(line, null);
                        if (code != null) {
                            writer.println(code);
                            if (!code.startsWith("//TODO:")) {
                                writer.println();
                            }
                        }
                    }
                }
            }
            
            logger.info("Code generation completed. Output file: {}", config.getOutputFile());
        } catch (IOException e) {
            logger.error("Error during conversion", e);
        }
    }

    private static List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(filename), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
} 
