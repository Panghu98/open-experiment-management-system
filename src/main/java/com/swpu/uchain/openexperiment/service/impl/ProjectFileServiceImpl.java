package com.swpu.uchain.openexperiment.service.impl;

import com.swpu.uchain.openexperiment.DTO.AttachmentFileDTO;
import com.swpu.uchain.openexperiment.VO.file.AttachmentFileVO;
import com.swpu.uchain.openexperiment.config.UploadConfig;
import com.swpu.uchain.openexperiment.mapper.ProjectFileMapper;
import com.swpu.uchain.openexperiment.mapper.ProjectGroupMapper;
import com.swpu.uchain.openexperiment.domain.ProjectFile;
import com.swpu.uchain.openexperiment.domain.ProjectGroup;
import com.swpu.uchain.openexperiment.domain.User;
import com.swpu.uchain.openexperiment.enums.CodeMsg;
import com.swpu.uchain.openexperiment.enums.FileType;
import com.swpu.uchain.openexperiment.enums.MaterialType;
import com.swpu.uchain.openexperiment.enums.ProjectStatus;
import com.swpu.uchain.openexperiment.exception.GlobalException;
import com.swpu.uchain.openexperiment.redis.RedisService;
import com.swpu.uchain.openexperiment.redis.key.FileKey;
import com.swpu.uchain.openexperiment.redis.key.ProjectGroupKey;
import com.swpu.uchain.openexperiment.result.Result;
import com.swpu.uchain.openexperiment.service.GetUserService;
import com.swpu.uchain.openexperiment.service.ProjectFileService;
import com.swpu.uchain.openexperiment.service.xdoc.XDocService;
import com.swpu.uchain.openexperiment.util.ConvertUtil;
import com.swpu.uchain.openexperiment.util.FileUtil;
import com.swpu.uchain.openexperiment.util.PDFConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.swpu.uchain.openexperiment.util.FileUtil.getFileSuffix;

/**
 * @Description
 * @Author cby
 * @Date 19-1-22
 **/
@Service
@Slf4j
@Component
public class ProjectFileServiceImpl implements ProjectFileService {
    @Autowired
    private UploadConfig uploadConfig;
    @Autowired
    private ProjectFileMapper projectFileMapper;
    @Autowired
    private GetUserService getUserService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private XDocService xDocService;
    @Autowired
    private ConvertUtil convertUtil;
    @Autowired
    private ProjectGroupMapper projectGroupMapper;


    public boolean insert(ProjectFile projectFile) {
        ProjectFile projectFile1 = projectFileMapper.selectByProjectGroupIdAndMaterialType(projectFile.getProjectGroupId(),projectFile.getMaterialType());
        if (projectFile1!=null){
            projectFile.setId(projectFile1.getId());
            return update(projectFile);
        }
        return projectFileMapper.insert(projectFile) == 1;
    }

    public boolean update(ProjectFile projectFile) {
        return projectFileMapper.updateByPrimaryKey(projectFile) == 1;
    }

    @Override
    public void delete(Long id) {
        ProjectFile projectFile = selectById(id);
        if (projectFile == null){
            throw new GlobalException(CodeMsg.FILE_NOT_EXIST);
        }
        redisService.delete(FileKey.getById, id + "");
        if (FileUtil.deleteFile(FileUtil.getFileRealPath(
                projectFile.getId(),
                uploadConfig.getApplyDir(),
                projectFile.getFileName()))) {
            projectFileMapper.deleteByPrimaryKey(id);
        }
        throw new GlobalException(CodeMsg.DELETE_FILE_ERROR);
    }

    @Override
    public ProjectFile selectById(Long id) {
        return projectFileMapper.selectByPrimaryKey(id);
    }

    @Override
    public ProjectFile getAimNameProjectFile(Long projectGroupId, String aimFileName){
        return projectFileMapper.selectByGroupIdFileName(projectGroupId, aimFileName);
    }

    @Override
    public Result uploadApplyDoc(MultipartFile file, Long projectGroupId) {
        //先检查文件是否为空
        if (file.isEmpty()) {
            return Result.error(CodeMsg.UPLOAD_CANT_BE_EMPTY);
        }
        String docName = FileUtil.getFileRealPath(projectGroupId,
                uploadConfig.getApplyDir(),
                uploadConfig.getApplyFileName()+getFileSuffix(file.getOriginalFilename()));
        String pdfName = FileUtil.getFileRealPath(projectGroupId,
                uploadConfig.getApplyDir(),
                uploadConfig.getApplyFileName()+".pdf");
        //如果存在则覆盖
        File dest = new File(docName);

        dest.delete();
        if (!checkFileFormat(file, FileType.WORD.getValue())) {
            return Result.error(CodeMsg.FORMAT_UNSUPPORTED);
        }
        User user = getUserService.getCurrentUser();

        //TODO,校验当前用户是否有权进行上传
        ProjectFile projectFile = new ProjectFile();
        projectFile.setUploadUserId(Long.valueOf(user.getCode()));
        projectFile.setFileType(FileType.WORD.getValue());
        projectFile.setFileName(projectGroupId+"_"+uploadConfig.getApplyFileName()+".pdf");
        projectFile.setSize(FileUtil.FormatFileSize(file.getSize()));
        projectFile.setUploadTime(new Date());
        projectFile.setDownloadTimes(0);
        projectFile.setMaterialType(MaterialType.APPLY_MATERIAL.getValue());
        projectFile.setProjectGroupId(projectGroupId);
        if (!insert(projectFile)) {
            return Result.error(CodeMsg.ADD_ERROR);
        }
        if (FileUtil.uploadFile(
                file,
                FileUtil.getFileRealPath(
                    projectGroupId,
                    uploadConfig.getApplyDir(),
                    uploadConfig.getApplyFileName() + FileUtil.getMultipartFileSuffix(file)))) {
            //转换为PDF
            convertDocToPDF(docName,pdfName);
            return Result.success();
        }
        return Result.error(CodeMsg.UPLOAD_ERROR);
    }


    @Override
    public void downloadApplyFile(Long fileId, HttpServletResponse response) {
        ProjectFile projectFile = projectFileMapper.selectByPrimaryKey(fileId);
        if (projectFile == null){
            throw new GlobalException(CodeMsg.FILE_NOT_EXIST);
        }
        String realPath = FileUtil.getFileRealPath(fileId, uploadConfig.getApplyDir(), projectFile.getFileName());
        if (FileUtil.downloadFile(response, realPath)){
            throw new GlobalException(CodeMsg.DOWNLOAD_ERROR);
        }
        projectFile.setDownloadTimes(projectFile.getDownloadTimes() + 1);
        if (!update(projectFile)) {
            throw new GlobalException(CodeMsg.UPDATE_ERROR);
        }
    }

    @Override
    public void getConclusionDoc(Long fileId, HttpServletResponse response) {
        ProjectFile projectFile = projectFileMapper.selectByPrimaryKey(fileId);
        if (projectFile == null){
            throw new GlobalException(CodeMsg.FILE_NOT_EXIST);
        }
        String realPath = FileUtil.getFileRealPath(fileId, uploadConfig.getConclusionDir(), projectFile.getFileName());
        if (FileUtil.downloadFile(response, realPath)){
            throw new GlobalException(CodeMsg.DOWNLOAD_ERROR);
        }
        projectFile.setDownloadTimes(projectFile.getDownloadTimes() + 1);
        if (!update(projectFile)) {
            throw new GlobalException(CodeMsg.UPDATE_ERROR);
        }
    }

    @Override
    public void downloadApplyPdf(Long fileId, HttpServletResponse response) {
        ProjectFile projectFile = projectFileMapper.selectByPrimaryKey(fileId);
        if (projectFile == null){
            throw new GlobalException(CodeMsg.FILE_NOT_EXIST);
        }
        String realPath = FileUtil.getFileRealPath(
                projectFile.getId(),
                uploadConfig.getApplyDir(),
                FileUtil.getFileNameWithoutSuffix(projectFile.getFileName()) + ".pdf");
        File file = new File(realPath);
        if (file.exists()){
            if (FileUtil.downloadFile(response, realPath)) {
                throw new GlobalException(CodeMsg.DOWNLOAD_ERROR);
            }
        }
        try {
            xDocService.run(
                    FileUtil.getFileRealPath(projectFile.getId(), uploadConfig.getApplyDir(), projectFile.getFileName())
                    ,new File(realPath));
        } catch (IOException e) {
            e.printStackTrace();
            log.error("======================XDoc文件类型转换异常=========================");
        }
        if (FileUtil.downloadFile(response, realPath)) {
            throw new GlobalException(CodeMsg.DOWNLOAD_ERROR);
        }
    }

    @Override
    public List<ProjectFile> getProjectAllFiles(Long projectGroupId) {
//        return projectFileMapper.selectByProjectGroupIdAndMaterialType(projectGroupId,null);
        return null;
    }

    @Override
    public Result uploadAttachmentFile(MultipartFile multipartFile,Integer attachmentType) {
        if (multipartFile == null || multipartFile.isEmpty()){
            return Result.error(CodeMsg.UPLOAD_CANT_BE_EMPTY);
        }
        User currentUser = getUserService.getCurrentUser();
        ProjectFile projectFile = new ProjectFile();
        projectFile.setFileName(multipartFile.getOriginalFilename());
        projectFile.setDownloadTimes(0);
        projectFile.setFileType(FileUtil.getType(FileUtil.getMultipartFileSuffix(multipartFile)));
        projectFile.setSize(FileUtil.FormatFileSize(multipartFile.getSize()));
        projectFile.setUploadTime(new Date());
        projectFile.setMaterialType(MaterialType.APPLY_MATERIAL.getValue());
        projectFile.setUploadUserId(Long.valueOf(currentUser.getCode()));
        if (!insert(projectFile)) {
            return Result.error(CodeMsg.ADD_ERROR);
        }
        if (!FileUtil.uploadFile(
                multipartFile,
                FileUtil.getFileRealPath(
                        projectFile.getId(),
                        uploadConfig.getApplyDir(),
                        projectFile.getFileName()))) {
            return Result.error(CodeMsg.UPLOAD_ERROR);
        }
        return Result.success();
    }

    @Override
    public void downloadAttachmentFile(long fileId, HttpServletResponse response) {
        ProjectFile projectFile = selectById(fileId);
        if (projectFile == null){
            throw new GlobalException(CodeMsg.FILE_NOT_EXIST);
        }
        if (FileUtil.downloadFile(response, FileUtil.getFileRealPath(fileId, uploadConfig.getApplyDir(), projectFile.getFileName()))) {
            throw new GlobalException(CodeMsg.DOWNLOAD_ERROR);
        }
    }

    @Override
    public Result listAttachmentFiles() {
        List<AttachmentFileDTO> attachmentFileDTOS = projectFileMapper.selectAttachmentFiles();
        List<AttachmentFileVO> attachmentFileVOS = convertUtil.getAttachmentFileVOS(attachmentFileDTOS);
        return Result.success(attachmentFileVOS);
    }

    @Override
    public Result uploadConcludingReport(Long projectId,MultipartFile file) {
        ProjectGroup projectGroup = redisService.get(ProjectGroupKey.getByProjectGroupId, projectId + "", ProjectGroup.class);
        if (projectGroup == null) {
            projectGroup = projectGroupMapper.selectByPrimaryKey(projectId);
            if (projectGroup != null) {
                redisService.set(ProjectGroupKey.getByProjectGroupId, projectId + "", projectGroup);
            }
        }
        if (file == null) {
            throw new GlobalException(CodeMsg.FILE_EMPTY_ERROR);
        }
        if (projectGroup == null){
            return Result.error(CodeMsg.PROJECT_GROUP_NOT_EXIST);
        }
        if (!projectGroup.getStatus().equals(ProjectStatus.CONCLUDED.getValue())){
            throw new GlobalException(CodeMsg.PROJECT_STATUS_IS_NOT_CONCLUDED);
        }

        //判断是否存在该文件,若存在则进行覆盖
        ProjectFile projectFile = getAimNameProjectFile(projectGroup.getId(), uploadConfig.getConcludingFileName());
        if (projectFile != null){
            FileUtil.uploadFile(
                    file,
                    FileUtil.getFileRealPath(
                            projectFile.getId(),
                            uploadConfig.getConclusionDir(),
                            uploadConfig.getConcludingFileName()));
        }
        User currentUser = getUserService.getCurrentUser();

        //TODO,校验当前用户是否有权进行上传


        projectFile = new ProjectFile();
        projectFile.setUploadUserId(Long.valueOf(currentUser.getCode()));
        //数据库存储为pdf名称
        projectFile.setFileName(uploadConfig.getConcludingFileName()+".pdf");
        projectFile.setUploadTime(new Date());
        projectFile.setMaterialType(MaterialType.CONCLUSION_MATERIAL.getValue());
        projectFile.setSize(FileUtil.FormatFileSize(file.getSize()));
        projectFile.setFileType(FileUtil.getType(FileUtil.getMultipartFileSuffix(file)));
        projectFile.setDownloadTimes(0);
        if (!insert(projectFile)) {
            return Result.error(CodeMsg.ADD_ERROR);
        }
        if (!FileUtil.uploadFile(
                file,
                FileUtil.getFileRealPath(
                        projectFile.getId(),
                        uploadConfig.getConclusionDir(),
                        uploadConfig.getConcludingFileName()))) {
            return Result.error(CodeMsg.UPLOAD_ERROR);
        }
        return Result.success();
    }

    public boolean checkFileFormat(MultipartFile multipartFile, Integer aimType){
        String suffix = FileUtil.getMultipartFileSuffix(multipartFile);
        int type = FileUtil.getType(suffix);
        if (type != aimType) {
            return false;
        }
        return true;
    }

    @Async
    public void convertDocToPDF(String fileNameOfDoc,String fileNameOfPDF){
        PDFConvertUtil.convert(fileNameOfDoc,fileNameOfPDF);
    }

    /**
     * 解决IE edge 文件上传 文件名却出现了全路径+文件名的形式
     * @param fileName 文件名
     * @return
     */
    private static String getFileSuffix(String fileName){
        if (fileName == null){
            throw new GlobalException(CodeMsg.FILE_NAME_EMPTY_ERROR);
        }
        //最后一位  注意是"\\",主要针对于微软的浏览器
        int lastIndexOfSlash = fileName.lastIndexOf(".");
        return fileName.substring(lastIndexOfSlash);
    }

}
