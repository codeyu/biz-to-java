package com.example.factory;

import com.example.strategy.TextConverter;
import com.example.strategy.Type1TextConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConverterFactory.class);

    public static TextConverter getConverter(int type) {
        logger.info("Creating converter for type: {}", type);
        switch (type) {
            case 1:
                return new Type1TextConverter();
            // 可以添加更多类型的转换器
            default:
                throw new IllegalArgumentException("Unsupported converter type: " + type);
        }
    }
} 
