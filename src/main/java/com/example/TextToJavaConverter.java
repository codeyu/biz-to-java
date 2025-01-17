package com.example;

import com.example.config.ConverterConfig;
import com.example.factory.ConverterFactory;
import com.example.strategy.TextConverter;
import com.example.strategy.Type1TextConverter;
import com.example.strategy.Type2TextConverter;
import com.example.strategy.Type3TextConverter;
import com.example.strategy.Type4TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            
            // Type3 和 Type4 使用独立的处理流程
            if ("type3".equals(converterType.toLowerCase())) {
                processType3(config);
                return;
            } else if ("type4".equals(converterType.toLowerCase())) {
                processType4(config);
                return;
            }
            
            // 其他类型的处理保持不变
            TextConverter converter = ConverterFactory.getConverter(config.getConverterType());
            if (converter instanceof Type2TextConverter) {
                Type2TextConverter type2Converter = (Type2TextConverter) converter;
                type2Converter.setEntityFiles(config.getEntityFiles());
                type2Converter.setEntityInstances(config.getEntityInstances());
                type2Converter.setEnableLogicConversion(config.isEnableLogicConversion());
                type2Converter.setDefineFile(config.getDefineFile());
            } else if (converter instanceof Type1TextConverter) {
                Type1TextConverter type1Converter = (Type1TextConverter) converter;
                type1Converter.setEntityFiles(config.getEntityFiles());
                type1Converter.setEntityInstances(config.getEntityInstances());
            }
            
            // 处理 Type1 和 Type2
            processType1And2(converter, config);
            
        } catch (IOException e) {
            logger.error("Error during conversion", e);
        }
    }

    private static void processType3(ConverterConfig config) {
        logger.info("Processing Type3 conversion");
        try {
            Type3TextConverter converter = new Type3TextConverter();
            List<String> generatedCode = converter.convertExcelFile(config.getType3().getInputFile());
            
            // 创建输出目录
            File outputFile = new File("output/GeneratedCode3.java");
            outputFile.getParentFile().mkdirs();
            
            // 写入生成的代码
            try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(outputFile), "UTF-8"))) {
                
                // 写入文件头注释
                writer.println("/**");
                writer.println(" * Generated code from " + config.getType3().getInputFile());
                writer.println(" */");
                writer.println();
                
                for (String code : generatedCode) {
                    writer.println(code);
                }
            }
            
            logger.info("Type3 code generation completed. Output file: {}", outputFile.getPath());
        } catch (IOException e) {
            logger.error("Error in Type3 processing", e);
        }
    }

    private static void processType4(ConverterConfig config) {
        logger.info("Processing Type4 conversion");
        try {
            Type4TextConverter converter = new Type4TextConverter();
            List<String> generatedCode = converter.convertExcelFile(config.getType4().getInputFile());
            
            // 创建输出目录
            File outputFile = new File("output/GeneratedCode4.java");
            outputFile.getParentFile().mkdirs();
            
            // 写入生成的代码
            try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(outputFile), "UTF-8"))) {
                
                // 写入文件头注释
                writer.println("/**");
                writer.println(" * Generated code from " + config.getType4().getInputFile());
                writer.println(" */");
                writer.println();
                
                for (String code : generatedCode) {
                    writer.println(code);
                }
            }
            
            logger.info("Type4 code generation completed. Output file: {}", outputFile.getPath());
        } catch (IOException e) {
            logger.error("Error in Type4 processing", e);
        }
    }

    private static void processType1And2(TextConverter converter, ConverterConfig config) throws IOException {
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
