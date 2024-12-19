package com.example.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "test_table3")
public class TestTable3BaseEntity {

    /**
     * [test_field_1] 生活／仕事　会社コード
     */
    @Column(columnName = "test_field_1", columnOrder = 1, primary = false, dbType = "varchar", javaType = "String", comment = "字段1")
    private String testField1;

    /**
     * [test_field_2] 生活／仕事　取引先コード
     */
    @Column(columnName = "test_field_2", columnOrder = 2, primary = false, dbType = "varchar", javaType = "String", comment = "字段2")
    private String testField2;

    /**
     * [test_field_3] 生活／仕事　常務コード
     */
    @Column(columnName = "test_field_3", columnOrder = 3, primary = false, dbType = "varchar", javaType = "String", comment = "字段3")
    private String testField3;

    /**
     * [test_field_4] 請求　事実コード
     */
    @Column(columnName = "test_field_4", columnOrder = 4, primary = false, dbType = "varchar", javaType = "String", comment = "字段4")
    private String testField4;

    /**
     * [test_field_5] 請求情報
     */
    @Column(columnName = "test_field_5", columnOrder = 5, primary = false, dbType = "integer", javaType = "Integer", comment = "字段5")
    private Integer testField5;

    /**
     * [test_field_6] 請求時間
     */
    @Column(columnName = "test_field_6", columnOrder = 6, primary = false, dbType = "integer", javaType = "Integer", comment = "字段6")
    private Integer testField6;

    /**
     * [test_field_7] 発行日123
     */
    @Column(columnName = "test_field_7", columnOrder = 7, primary = false, dbType = "date", javaType = "Date", comment = "字段7")
    private Date testField7;

    /**
     * [test_field_8] 性能調査ー区別
     */
    @Column(columnName = "test_field_8", columnOrder = 8, primary = false, dbType = "integer", javaType = "Integer", comment = "字段8")
    private Integer testField8;

    /**
     * [test_field_9] 常務コード
     */
    @Column(columnName = "test_field_9", columnOrder = 9, primary = false, dbType = "varchar", javaType = "String", comment = "字段9")
    private String testField9;

    // Getters and Setters
    public String getTestField1() {
        return testField1;
    }

    public void setTestField1(String testField1) {
        this.testField1 = testField1;
    }

    public String getTestField2() {
        return testField2;
    }

    public void setTestField2(String testField2) {
        this.testField2 = testField2;
    }

    public String getTestField3() {
        return testField3;
    }

    public void setTestField3(String testField3) {
        this.testField3 = testField3;
    }

    public String getTestField4() {
        return testField4;
    }

    public void setTestField4(String testField4) {
        this.testField4 = testField4;
    }

    public Integer getTestField5() {
        return testField5;
    }

    public void setTestField5(Integer testField5) {
        this.testField5 = testField5;
    }

    public Integer getTestField6() {
        return testField6;
    }

    public void setTestField6(Integer testField6) {
        this.testField6 = testField6;
    }

    public Date getTestField7() {
        return testField7;
    }

    public void setTestField7(Date testField7) {
        this.testField7 = testField7;
    }

    public Integer getTestField8() {
        return testField8;
    }

    public void setTestField8(Integer testField8) {
        this.testField8 = testField8;
    }

    public String getTestField9() {
        return testField9;
    }

    public void setTestField9(String testField9) {
        this.testField9 = testField9;
    }
} 
