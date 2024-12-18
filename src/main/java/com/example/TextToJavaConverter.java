package com.example;

import com.example.config.ConverterConfig;
import com.example.factory.ConverterFactory;
import com.example.strategy.TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TextToJavaConverter {
    private static final Logger logger = LoggerFactory.getLogger(TextToJavaConverter.class);
    
    public static void main(String[] args) {
        try {
            ConverterConfig config = new ConverterConfig();
            TextConverter converter = ConverterFactory.getConverter(config.getGenType());
            
            // 读取输入文本
            List<String> inputLines = readFile(config.getInputFile());
            // 读取实体类文件
            List<String> entityLines = readFile(config.getEntityFile());
            
            // 创建输出目录
            new File(config.getOutputFile()).getParentFile().mkdirs();
            
            // 处理并写入输出文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(config.getOutputFile()))) {
                for (String line : inputLines) {
                    String javaCode = converter.convertLine(line, entityLines);
                    if (javaCode != null) {
                        writer.println(javaCode);
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
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
} 
