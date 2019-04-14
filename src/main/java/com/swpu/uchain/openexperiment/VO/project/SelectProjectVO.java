package com.swpu.uchain.openexperiment.VO.project;

import lombok.Data;

/**
 * @Author: clf
 * @Date: 19-4-13
 * @Description:
 * 搜索项目信息展示VO
 */
@Data
public class SelectProjectVO {
    /**
     * 项目组id
     */
    private Long projectGroupId;
    /**
     * 项目名
     */
    private String projectName;
    /**
     * 创建者姓名
     */
    private String creatorName;

}
