package com.example.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

public class Type1TextConverterTest {
    private static final Logger logger = LoggerFactory.getLogger(Type1TextConverterTest.class);
    
    private Type1TextConverter converter;
    private List<String> mockEntityLines;

    @Before
    public void setUp() {
        converter = new Type1TextConverter();
        // 模拟实体类文件内容
        mockEntityLines = Arrays.asList(
            "/**",
            " * [test_field_1] 生活／仕事　会社コード",
            " */",
            "@Column(columnName = \"test_field_1\")",
            "private String testField1;",
            "/**",
            " * [test_field_2] 請求 コード",
            " */",
            "@Column(columnName = \"test_field_2\")",
            "private Integer testField2;"
        );
        logger.info("Test setup complete with mock data");
    }

    @Test
    public void testExtractComment() {
        String input = "項目「手袋(Ｌ０１).(生活／仕事　会社コード)」にブランクを代入します。";
        logger.info("Testing input: {}", input);
        
        // 验证输入数据
        assertNotNull("Input should not be null", input);
        assertTrue("Input should contain expected comment", 
            input.contains("生活／仕事　会社コード"));
        
        String result = converter.convertLine(input, mockEntityLines);
        logger.info("Test result: {}", result);
        
        // 验证结果
        assertNotNull("Should extract and convert the line", result);
        assertTrue("Result should contain setter method", 
            result.contains("setTestField1"));
        assertTrue("Result should contain empty string for ブランク", 
            result.contains("\"\""));
        assertEquals("testTable1BaseEntity.setTestField1(\"\");", result);
    }

    @Test
    public void testHalfWidthKatakana() {
        String input = "項目「手袋(Ｌ０１).(請求 コード)」＝　０。";
        logger.info("Testing input: {}", input);
        
        // 验证输入数据
        assertNotNull("Input should not be null", input);
        assertTrue("Input should contain expected katakana", 
            input.contains("コード"));
        
        String result = converter.convertLine(input, mockEntityLines);
        logger.info("Test result: {}", result);
        
        // 验证结果
        assertNotNull("Should handle half-width katakana", result);
        assertTrue("Result should contain setter method", 
            result.contains("setTestField2"));
        assertTrue("Result should contain zero", 
            result.contains("0"));
        assertEquals("testTable1BaseEntity.setTestField2(0);", result);
    }

    @Test
    public void testInvalidInput() {
        // 测试无效输入
        String result = converter.convertLine("invalid input", mockEntityLines);
        assertNull("Should return null for invalid input", result);
    }

    @Test
    public void testNoMatchingField() {
        // 测试找不到匹配字段的情况
        String input = "項目「手袋(Ｌ０１).(不存在的字段)」にブランクを代入します。";
        String result = converter.convertLine(input, mockEntityLines);
        assertNull("Should return null when no matching field found", result);
    }
} 
