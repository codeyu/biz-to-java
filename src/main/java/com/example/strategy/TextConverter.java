package com.example.strategy;

import java.util.List;

public interface TextConverter {
    /**
     * 将输入行转换为Java代码
     * @param line 输入行
     * @param entityLines 实体类文件内容
     * @return 生成的Java代码，如果无法转换则返回null
     */
    String convertLine(String line, List<String> entityLines);
} 
