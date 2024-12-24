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
import java.io.File;
import java.io.FileNotFoundException;

public class ConverterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ConverterConfig.class);
    private static final String CONFIG_FILE = "application.yml";
    
    private String converterType;      // 转换器类型 "converterType1" 或 "converterType2"
    private String inputFile;          // 输入文件路径
    private Map<String, String> entityFiles;  // 实体文件映射
    private Map<String, String> entityInstances;  // 实体实例名映射
    private String outputFile;         // 输出文件路径
    private final String baseDir;
    private boolean enableLogicConversion;  // 添加这行
    private String defineFile;  // 添加字段

    public ConverterConfig(String converterType) {
        this.converterType = converterType;
        this.baseDir = System.getProperty("user.dir");
        this.entityFiles = new HashMap<>();
        this.entityInstances = new HashMap<>();
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        // 首先尝试从当前目录加载
        File localConfig = new File(CONFIG_FILE);
        InputStream input = null;
        
        try {
            if (localConfig.exists()) {
                logger.info("Loading config from local file: {}", localConfig.getAbsolutePath());
                input = new FileInputStream(localConfig);
            } else {
                // 如果本地文件不存在，从classpath加载
                logger.info("Loading config from classpath: {}", CONFIG_FILE);
                input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
                if (input == null) {
                    throw new FileNotFoundException("Config file not found in classpath: " + CONFIG_FILE);
                }
            }
            
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
            
            // 处理实体实例名映射
            List<String> entityInstancesList = (List<String>) converterConfig.get("entityInstance");
            for (String mapping : entityInstancesList) {
                String[] parts = mapping.split("=");
                if (parts.length == 2) {
                    entityInstances.put(parts[0].trim(), parts[1].trim());
                }
            }
            
            // 加载逻辑转换开关
            Object enableLogic = converterConfig.get("enableLogicConversion");
            this.enableLogicConversion = enableLogic != null && (Boolean) enableLogic;
            logger.info("Logic conversion enabled: {}", this.enableLogicConversion);
            
            // 加载 defineFile 配置
            this.defineFile = resolvePath((String) converterConfig.get("defineFile"));
            logger.info("Loaded define file path: {}", this.defineFile);
            
            logger.info("Configuration loaded successfully for {}", converterType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error closing config file", e);
                }
            }
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
    public Map<String, String> getEntityInstances() { return entityInstances; }
    public boolean isEnableLogicConversion() {
        return enableLogicConversion;
    }
    public String getDefineFile() {
        return defineFile;
    }
} 
