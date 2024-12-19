package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class GeneratedType2JavaInfo {
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

        public ConditionPart(String leftSide, String operator, String rightSide) {
            this.leftSide = leftSide;
            this.operator = operator;
            this.rightSide = rightSide;
        }

        public String generateCode() {
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
            } else if (value.equals("ブランク")) {
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
            BOOLEAN_FIELD
        }

        public String generateCode() {
            switch (type) {
                case ENTITY_FIELD:
                    return String.format("%s(%s);", target, value);
                case DIRECT_FIELD:
                    return String.format("this.%s = %s;", target, convertValue(value));
                case BOOLEAN_FIELD:
                    return String.format("this.%s = true;", target);
                default:
                    return null;
            }
        }

        private String convertValue(String value) {
            if (value.equals("ブランク")) {
                return "\"\"";
            } else if (value.equals("'1'")) {
                return "true";
            } else if (value.equals("'0'")) {
                return "false";
            }
            return value;
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