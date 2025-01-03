package com.example.model;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratedType2JavaInfo {
    private static final Logger logger = LoggerFactory.getLogger(GeneratedType2JavaInfo.class);
    private Condition condition;
    private List<Assignment> assignments;

    public GeneratedType2JavaInfo() {
        this.assignments = new ArrayList<>();
        this.condition = new Condition();
    }

    public Condition getCondition() {
        return condition;
    }

    public void addAssignment(Assignment assignment) {
        this.assignments.add(assignment);
    }

    private boolean hasTodoForText(String text) {
        return assignments.stream()
            .anyMatch(assignment -> 
                assignment.getType() == Assignment.AssignmentType.TODO && 
                text.equals(assignment.getTarget()));
    }

    public void handleFailure(String originalText, String reason) {
        if (hasTodoForText(originalText)) {
            logger.debug("TODO already exists for text: {}", originalText);
            return;
        }
        
        logger.warn("Failed to process text: {} (Reason: {})", originalText, reason);
        assignments.add(new Assignment(
            originalText,
            null,
            Assignment.AssignmentType.TODO
        ));
    }

    public static class Condition {
        private List<ConditionPart> parts = new ArrayList<>();
        private String logicalOperator; // "または" 或 "かつ"

        public void addPart(ConditionPart part) {
            parts.add(part);
        }

        public void setLogicalOperator(String operator) {
            this.logicalOperator = operator;
        }

        public String generateCode() {
            StringBuilder code = new StringBuilder("if(");
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    code.append(logicalOperator.equals("または") ? " || " : " && ");
                }
                code.append(parts.get(i).generateCode());
            }
            code.append(")");
            return code.toString();
        }
    }

    public static class ConditionPart {
        private String leftSide;
        private String operator;
        private String rightSide;
        private boolean isError;

        public ConditionPart(String leftSide, String operator, String rightSide, boolean isError) {
            this.leftSide = leftSide;
            this.operator = operator;
            this.rightSide = rightSide;
            this.isError = isError;
        }

        public String generateCode() {
            if (isError) {
                return String.format("//IFのERROR: [%s %s %s]", 
                    leftSide, operator, rightSide);
            }
            return String.format("%s %s %s", 
                convertValue(leftSide),
                convertOperator(operator),
                convertValue(rightSide));
        }

        private String convertOperator(String op) {
            if ("≠".equals(op)) return "!=";
            if ("＝".equals(op)) return "==";
            return op;
        }

        private String convertValue(String value) {
            if (value.startsWith("D\\")) {
                return "this." + value.substring(2);
            } else if (value.startsWith("*IN")) {
                return "this." + value.substring(1);
            } else if (value.equals("ブランク") || value.equals("ﾌﾞﾗﾝｸ")) {
                return "\"\"";
            } else {
                return value;
            }
        }
    }

    public static class Assignment {
        private String target;
        private String value;
        private AssignmentType type;

        public Assignment(String target, String value, AssignmentType type) {
            this.target = target;
            this.value = value;
            this.type = type;
        }

        public enum AssignmentType {
            ENTITY_FIELD,
            DIRECT_FIELD,
            BOOLEAN_FIELD,
            TODO
        }

        public String generateCode() {
            switch (type) {
                case ENTITY_FIELD:
                    return String.format("%s(%s);", target, value);
                case DIRECT_FIELD:
                    return String.format("this.%s = %s;", target, convertValue(value));
                case BOOLEAN_FIELD:
                    return String.format("this.%s = true;", target);
                case TODO:
                    return String.format("//TODO: %s", target);
                default:
                    return null;
            }
        }

        private String convertValue(String value) {
            if (value.equals("ブランク") || value.equals("ﾌﾞﾗﾝｸ")) {
                return "\"\"";
            } else if (value.equals("'1'")) {
                return "true";
            } else if (value.equals("'0'")) {
                return "false";
            }
            return value;
        }

        public AssignmentType getType() {
            return type;
        }

        public String getTarget() {
            return target;
        }
    }

    public String generateCode() {
        StringBuilder code = new StringBuilder();
        code.append(condition.generateCode()).append(" {\n");
        for (Assignment assignment : assignments) {
            code.append("    ").append(assignment.generateCode()).append("\n");
        }
        code.append("}\n");
        return code.toString();
    }
} 