package com.example;

import com.example.config.ConverterConfig;
import com.example.factory.ConverterFactory;
import com.example.model.GeneratedType1JavaInfo;
import com.example.strategy.Type1TextConverter;
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
            Type1TextConverter converter = (Type1TextConverter) ConverterFactory.getConverter(config.getGenType());
            
            // 读取输入文本
            List<String> inputLines = readFile(config.getInputFile());
            
            // 转换所有行并收集结果
            List<GeneratedType1JavaInfo> results = converter.convertFile(inputLines);
            
            // 创建输出目录
            new File(config.getOutputFile()).getParentFile().mkdirs();
            
            // 写入生成的代码
            try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(config.getOutputFile()), "UTF-8"))) {
                for (GeneratedType1JavaInfo info : results) {
                    String code = info.generateCode();
                    if (code != null) {
                        writer.println(code);
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
