package com.example.util;

import com.example.model.VariableDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class VariableDefinitionReader {
    private static final Logger logger = LoggerFactory.getLogger(VariableDefinitionReader.class);
    private static final Pattern VARIABLE_PATTERN = 
        Pattern.compile("private\\s+(\\w+)\\s+(\\w+)(?:\\s*=\\s*([^;]+))?;");

    public static Map<String, VariableDefinition> readDefinitions(String filename) throws IOException {
        Map<String, VariableDefinition> definitions = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                Matcher matcher = VARIABLE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String type = matcher.group(1);
                    String name = matcher.group(2);
                    String defaultValue = matcher.group(3);
                    
                    logger.debug("Found variable definition - type: {}, name: {}, default: {}", 
                        type, name, defaultValue);
                    
                    definitions.put(name, new VariableDefinition(name, type, defaultValue));
                }
            }
        }
        
        return definitions;
    }
} 