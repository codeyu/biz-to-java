package com.example.strategy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Type3TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type3TextConverter.class);
    private static final int VAR_NAME_COL = 0;      // 变量名列
    private static final int COMMENT_COL = 1;       // 注释列
    private static final int TYPE_COL = 2;          // 数据类型列
    private static final int ARRAY_LENGTH_COL = 5;  // 数组长度列
    private static final int DEFAULT_VALUE_COL = 7; // 默认值列

    // 添加成员变量保存当前处理的变量信息
    private String currentVarName;
    private String currentJavaType;
    private List<String> currentDefaultValues = new ArrayList<>();

    public List<String> convertExcelFile(String filePath) {
        List<String> generatedCode = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            // 从第二行开始遍历（跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = processRow(row);
                if (code != null) {
                    generatedCode.add(code);
                }
            }
            
            // 处理最后一个变量
            if (currentVarName != null) {
                String finalCode = generateArrayCode();
                if (finalCode != null) {
                    generatedCode.add(finalCode);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing Excel file: {}", filePath, e);
        }
        
        return generatedCode;
    }

    private String processRow(Row row) {
        try {
            String varName = getCellStringValue(row.getCell(VAR_NAME_COL));
            String defaultValues = getCellStringValue(row.getCell(DEFAULT_VALUE_COL));

            // 如果只有默认值列有值，添加到当前变量的默认值列表中
            if (isEmptyExceptDefaultValue(row) && defaultValues != null && !defaultValues.trim().isEmpty()) {
                if (currentVarName != null) {
                    // 添加新的默认值
                    String[] values = defaultValues.trim().split("\\s+");
                    currentDefaultValues.addAll(Arrays.asList(values));
                    return null;  // 不生成代码，继续收集默认值
                }
                return null;
            }

            // 如果是新变量，先处理之前的变量
            String previousCode = null;
            if (currentVarName != null && varName != null && !varName.trim().isEmpty()) {
                previousCode = generateArrayCode();
                // 重置当前变量信息
                currentVarName = null;
                currentJavaType = null;
                currentDefaultValues.clear();
            }

            // 处理新变量
            if (varName != null && !varName.trim().isEmpty()) {
                currentVarName = varName.toLowerCase();
                currentJavaType = convertType(getCellStringValue(row.getCell(TYPE_COL)));
                if (defaultValues != null && !defaultValues.trim().isEmpty()) {
                    currentDefaultValues.addAll(Arrays.asList(defaultValues.trim().split("\\s+")));
                }
            }

            return previousCode;
            
        } catch (Exception e) {
            logger.error("Error processing row: {}", row.getRowNum(), e);
            return null;
        }
    }

    private boolean isEmptyExceptDefaultValue(Row row) {
        if (row == null) return false;
        
        // 检查除了默认值列以外的其他列是否都为空
        return getCellStringValue(row.getCell(VAR_NAME_COL)) == null &&
               getCellStringValue(row.getCell(COMMENT_COL)) == null &&
               getCellStringValue(row.getCell(TYPE_COL)) == null &&
               getCellStringValue(row.getCell(ARRAY_LENGTH_COL)) == null;
    }

    private String generateArrayCode() {
        if (currentVarName == null) return null;

        StringBuilder code = new StringBuilder();
        
        // 生成数组声明
        code.append("    private ").append(currentJavaType).append("[] ")
            .append(currentVarName).append(" = ");

        // 处理默认值
        if (!currentDefaultValues.isEmpty()) {
            code.append("new ").append(currentJavaType).append("[] {");
            for (int i = 0; i < currentDefaultValues.size(); i++) {
                if (i > 0) code.append(", ");
                code.append(formatValue(currentJavaType, currentDefaultValues.get(i)));
            }
            code.append("};");
        } else {
            code.append("new ").append(currentJavaType).append("[0];");
        }

        return code.toString();
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((int)cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private String convertType(String type) {
        if ("A".equals(type)) {
            return "String";
        }
        // 可以添加其他类型的转换
        return "String"; // 默认使用String
    }

    private String formatValue(String javaType, String value) {
        if ("String".equals(javaType)) {
            return "\"" + value + "\"";
        }
        // 可以添加其他类型的格式化
        return value;
    }
} 