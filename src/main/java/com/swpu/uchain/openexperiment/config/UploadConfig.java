package com.swpu.uchain.openexperiment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: clf
 * @Date: 19-1-29
 * @Description:
 */
@Component
@Data
@ConfigurationProperties("upload")
public class UploadConfig {
    /**
     * 上传重点项目立项申请书文件夹路径
     */
    private String applyDir;

    /**
     *  上传重点项目立项申请书文件夹路径--头部
     */
    private String applyDir2;

    /**
     * 中间PDF存放位置
     */
    private String pdfTempDir;

    /**
     * 上传立项申请正文文件名
     */
    private String applyFileName;
    /**
     * 上传结题报告文件文件名
     */
    private String concludingFileName;

    /**
     * 证明材料上传位置
     */
    private String conclusionDir;
}

