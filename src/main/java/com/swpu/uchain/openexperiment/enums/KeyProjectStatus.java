package com.swpu.uchain.openexperiment.enums;

import lombok.Getter;

/**
 * 重点项目状态
 * @author dengg
 */

@Getter
public enum KeyProjectStatus {

    /**
     *
     */
    ESTABLISH_FAILED(-3,"立项失败"),

    REJECT_MODIFY(-2,"驳回修改"),

    TO_DE_CONFIRMED(-1,"待确认--项目组长编写后"),

    LAB_ALLOWED(1,"实验室审核通过,待上报"),

    LAB_ALLOWED_AND_REPORTED(2,"实验室审核通过并已上报"),

    SECONDARY_UNIT_ALLOWED(3,"二级单位审核通过,待上报"),

    SECONDARY_UNIT_ALLOWED_AND_REPORTED(4,"二级单位审核通过并已上报"),

    ESTABLISH(5,"职能部门审核通过,立项"),

    MID_TERM_INSPECTION(6,"中期检查"),

    CONCLUDED(7,"结题项目")
    ;

    private Integer value;

    private String tips;

    KeyProjectStatus(Integer value, String tips) {
        this.value = value;
        this.tips = tips;
    }

}