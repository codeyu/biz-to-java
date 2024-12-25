package com.example.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratedType1JavaInfo {
    private static final Logger logger = LoggerFactory.getLogger(GeneratedType1JavaInfo.class);
    private String code;
    private boolean isFailure;
    private String originalText;

    public GeneratedType1JavaInfo() {
        this.isFailure = false;
    }

    public void setSuccessCode(String code) {
        this.code = code;
        this.isFailure = false;
    }

    public void setFailure(String originalText, String reason) {
        logger.warn("Failed to process text: {} (Reason: {})", originalText, reason);
        this.code = String.format("//TODO: %s", originalText);
        this.originalText = originalText;
        this.isFailure = true;
    }

    public String getCode() {
        return code;
    }

    public boolean isFailure() {
        return isFailure;
    }
} 