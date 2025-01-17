package com.example.strategy;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class Type4TextConverter {
    private static final Logger logger = LoggerFactory.getLogger(Type4TextConverter.class);
    private static final int VAR_NAME_COL = 0;      // 变量名列
    private static final int COMMENT_COL = 1;       // 注释列
    private static final int TYPE_COL = 2;          // 数据类型列

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

            if (varName == null || varName.trim().isEmpty()) {
                return null;
            }

            // 处理变量名
            varName = processVariableName(varName.trim());

            StringBuilder code = new StringBuilder();
            
            // 添加注释
            if (comment != null && !comment.trim().isEmpty()) {
                code.append("    // ").append(comment).append("\n");
            }

            // 生成变量声明
            String javaType = convertType(type);
            code.append("    private ").append(javaType).append(" ")
                .append(varName).append(";");

            return code.toString();
            
        } catch (Exception e) {
            logger.error("Error processing row: {}", row.getRowNum(), e);
            return null;
        }
    }

    private String processVariableName(String name) {
        // 移除特殊字符
        String processed = name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        
        // 检查是否是纯数字
        if (processed.matches("\\d+")) {
            processed = "m" + processed;
        }
        
        return processed;
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
        if (type == null) return "String";
        
        switch (type.toUpperCase()) {
            case "P":
                return "boolean";
            case "A":
                return "String";
            case "S":
                return "int";
            default:
                return "String";
        }
    }
} 