package com.example.util;

import org.junit.Test;



import static org.junit.Assert.*;

public class StringCompareUtilTest {

    @Test
    public void testCompareJapaneseString() {
        // 测试空格处理
        assertTrue(StringCompareUtil.compareJapaneseString(
            "生活／仕事　会社コード",
            "生活／仕事会社コード"
        ));

        // 测试全角数字和半角数字
        assertTrue(StringCompareUtil.compareJapaneseString(
            "コード１２３",
            "コード123"
        ));

        // 测试半角片假名和全角片假名
        assertTrue(StringCompareUtil.compareJapaneseString(
            "請求 ｺｰﾄﾞ",
            "請求 コード"
        ));

        // 测试特殊字符
        assertTrue(StringCompareUtil.compareJapaneseString(
            "test.コード",
            "testコード"
        ));

        // 测试不相等的情况
        assertFalse(StringCompareUtil.compareJapaneseString(
            "コード123",
            "コード456"
        ));

        // 测试空值处理
        assertFalse(StringCompareUtil.compareJapaneseString(null, "test"));
        assertFalse(StringCompareUtil.compareJapaneseString("test", null));
        assertFalse(StringCompareUtil.compareJapaneseString(null, null));
    }

    @Test
    public void testSpecialCharacters() {
        // 测试方括号处理
        assertTrue(StringCompareUtil.compareJapaneseString(
            "[test_field_1] コード",
            "testfield1コード"
        ));

        // 测试斜杠处理
        assertTrue(StringCompareUtil.compareJapaneseString(
            "生活／仕事",
            "生活仕事"
        ));

        // 测试连字符处理
        assertTrue(StringCompareUtil.compareJapaneseString(
            "性能調査ー区別",
            "性能調査区別"
        ));
    }
} 
