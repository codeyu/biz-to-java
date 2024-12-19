package com.example.config;

import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConverterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ConverterConfig.class);
    private static final String CONFIG_FILE = "src/main/resources/application.yml";
    
    private String converterType;      // 转换器类型 "converterType1" 或 "converterType2"
    private String inputFile;          // 输入文件路径
    private Map<String, String> entityFiles;  // 实体文件映射
    private String outputFile;         // 输出文件路径
    private final String baseDir;

    public ConverterConfig(String converterType) {
        this.converterType = converterType;
        this.baseDir = System.getProperty("user.dir");
        this.entityFiles = new HashMap<>();
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            
            Map<String, Object> converterConfig = (Map<String, Object>) config.get(converterType);
            if (converterConfig == null) {
                throw new RuntimeException("Configuration not found for converter type: " + converterType);
            }
            
            this.inputFile = resolvePath((String) converterConfig.get("inputFile"));
            this.outputFile = resolvePath((String) converterConfig.get("outputFile"));
            
            // 处理实体文件映射
            List<String> entityFilesList = (List<String>) converterConfig.get("entityFile");
            for (String mapping : entityFilesList) {
                String[] parts = mapping.split("=");
                if (parts.length == 2) {
                    entityFiles.put(parts[0].trim(), resolvePath(parts[1].trim()));
                }
            }
            
            logger.info("Configuration loaded successfully for {}", converterType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file", e);
        }
    }

    private String resolvePath(String path) {
        if (path == null) return null;
        return Paths.get(path).isAbsolute() ? path : 
            Paths.get(baseDir, path).normalize().toString();
    }

    public String getInputFile() { return inputFile; }
    public Map<String, String> getEntityFiles() { return entityFiles; }
    public String getOutputFile() { return outputFile; }
    public String getConverterType() { return converterType; }
} 
