package com.example.config;

import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ConverterConfig {
    private static final Logger logger = LoggerFactory.getLogger(ConverterConfig.class);
    private static final String CONFIG_FILE = "src/main/resources/application.yml";
    private String inputFile;
    private String entityFile;
    private String outputFile;
    private int genType;
    private final String baseDir;

    public ConverterConfig() {
        // 获取项目根目录
        this.baseDir = System.getProperty("user.dir");
        loadConfig();
    }

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            Map<String, Object> converter = (Map<String, Object>) config.get("converter");
            
            this.inputFile = resolvePath((String) converter.get("inputFile"));
            this.entityFile = resolvePath((String) converter.get("entityFile"));
            this.outputFile = resolvePath((String) converter.get("outputFile"));
            this.genType = (Integer) converter.get("genType");
            
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file", e);
        }
    }

    private String resolvePath(String path) {
        if (path == null) {
            return null;
        }
        
        // 如果是绝对路径，直接返回
        if (Paths.get(path).isAbsolute()) {
            return path;
        }
        
        // 如果是相对路径，相对于项目根目录解析
        return Paths.get(baseDir, path).normalize().toString();
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getEntityFile() {
        return entityFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public int getGenType() {
        return genType;
    }
} 
