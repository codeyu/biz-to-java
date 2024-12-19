package com.example.util;

import java.text.Normalizer;

public class StringCompareUtil {
    
    public static boolean compareJapaneseString(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return false;
        }
        
        // 1. 去除所有空格和特殊字符
        str1 = str1.replaceAll("[\\s*\"./\\[\\]]", "");
        str2 = str2.replaceAll("[\\s*\"./\\[\\]]", "");
        
        // 2. 转换全角数字为半角数字
        str1 = convertFullWidthNumberToHalf(str1);
        str2 = convertFullWidthNumberToHalf(str2);
        
        // 3. 标准化日文字符
        str1 = normalizeJapanese(str1);
        str2 = normalizeJapanese(str2);
        
        return str1.equals(str2);
    }
    
    private static String convertFullWidthNumberToHalf(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c >= '０' && c <= '９') {
                sb.append((char) (c - '０' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    private static String normalizeJapanese(String str) {
        // 使用NFKC标准化，这会将半角片假名转换为全角片假名
        str = Normalizer.normalize(str, Normalizer.Form.NFKC);
        
        // 移除所有空格和标点符号
        str = str.replaceAll("[\\p{Punct}\\s]", "");
        
        return str;
    }
} 
