package com.example.strategy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class Type3TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type3TextConverter.class);
    private static final int VAR_NAME_COL = 0;      // 变量名列
    private static final int COMMENT_COL = 1;       // 注释列
    private static final int TYPE_COL = 2;          // 数据类型列
    private static final int ARRAY_LENGTH_COL = 5;  // 数组长度列
    private static final int DEFAULT_VALUE_COL = 7; // 默认值列

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
            
        } catch (Exception e) {
            logger.error("Error processing Excel file: {}", filePath, e);
        }
        
        return generatedCode;
    }

    private String processRow(Row row) {
        try {
            String varName = getCellStringValue(row.getCell(VAR_NAME_COL));
            String comment = getCellStringValue(row.getCell(COMMENT_COL));
            String type = getCellStringValue(row.getCell(TYPE_COL));
            String arrayLength = getCellStringValue(row.getCell(ARRAY_LENGTH_COL));
            String defaultValues = getCellStringValue(row.getCell(DEFAULT_VALUE_COL));

            if (varName == null || varName.trim().isEmpty()) {
                return null;
            }

            // 将变量名转换为小写
            varName = varName.toLowerCase();

            StringBuilder code = new StringBuilder();
            
            // 添加注释
            if (comment != null && !comment.trim().isEmpty()) {
                code.append("    // ").append(comment).append("\n");
            }

            // 生成数组声明
            String javaType = convertType(type);
            code.append("    private ").append(javaType).append("[] ")
                .append(varName).append(" = ");

            // 处理默认值
            if (defaultValues != null && !defaultValues.trim().isEmpty()) {
                // 有默认值时使用默认值初始化
                String[] values = defaultValues.trim().split("\\s+");
                code.append("new ").append(javaType).append("[] {");
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) code.append(", ");
                    code.append(formatValue(javaType, values[i]));
                }
                code.append("};");
            } else if (arrayLength != null && !arrayLength.trim().isEmpty()) {
                // 没有默认值但有长度时，使用长度初始化
                code.append("new ").append(javaType)
                    .append("[").append(arrayLength.trim()).append("];");
            } else {
                // 都没有时，初始化为空数组
                code.append("new ").append(javaType).append("[0];");
            }

            return code.toString();
            
        } catch (Exception e) {
            logger.error("Error processing row: {}", row.getRowNum(), e);
            return null;
        }
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