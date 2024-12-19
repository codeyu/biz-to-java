package com.example.factory;

import com.example.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConverterFactory.class);

    public static TextConverter getConverter(String type) {
        logger.info("Creating converter for type: {}", type);
        switch (type) {
            case "type1":
                return new Type1TextConverter();
            case "type2":
                return new Type2TextConverter();
            default:
                throw new IllegalArgumentException("Unsupported converter type: " + type);
        }
    }
} 
