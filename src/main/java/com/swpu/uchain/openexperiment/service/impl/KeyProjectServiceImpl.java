package com.swpu.uchain.openexperiment.service.impl;

import com.swpu.uchain.openexperiment.DTO.KeyProjectDTO;
import com.swpu.uchain.openexperiment.DTO.OperationRecord;
import com.swpu.uchain.openexperiment.DTO.ProjectHistoryInfo;
import com.swpu.uchain.openexperiment.VO.limit.AmountAndTypeVO;
import com.swpu.uchain.openexperiment.VO.limit.AmountLimitVO;
import com.swpu.uchain.openexperiment.domain.AmountLimit;
import com.swpu.uchain.openexperiment.domain.UserProjectGroup;
import com.swpu.uchain.openexperiment.form.amount.AmountAndType;
import com.swpu.uchain.openexperiment.mapper.*;
import com.swpu.uchain.openexperiment.domain.ProjectGroup;
import com.swpu.uchain.openexperiment.domain.User;
import com.swpu.uchain.openexperiment.enums.*;
import com.swpu.uchain.openexperiment.exception.GlobalException;
import com.swpu.uchain.openexperiment.form.query.HistoryQueryKeyProjectInfo;
import com.swpu.uchain.openexperiment.form.check.KeyProjectCheck;
import com.swpu.uchain.openexperiment.form.project.KeyProjectApplyForm;
import com.swpu.uchain.openexperiment.form.query.QueryConditionForm;
import com.swpu.uchain.openexperiment.form.user.StuMember;
import com.swpu.uchain.openexperiment.result.Result;
import com.swpu.uchain.openexperiment.service.GetUserService;
import com.swpu.uchain.openexperiment.service.KeyProjectService;
import com.swpu.uchain.openexperiment.service.TimeLimitService;
import com.swpu.uchain.openexperiment.util.SerialNumberUtil;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author dengg
 */
@Service
public class KeyProjectServiceImpl implements KeyProjectService {


    private ProjectGroupMapper projectGroupMapper;
    private UserProjectGroupMapper userProjectGroupMapper;
    private KeyProjectStatusMapper keyProjectStatusMapper;
    private GetUserService getUserService;
    private OperationRecordMapper operationRecordMapper;
    private TimeLimitService timeLimitService;
    private AmountLimitMapper amountLimitMapper;

    @Autowired
    public KeyProjectServiceImpl(ProjectGroupMapper projectGroupMapper, UserProjectGroupMapper userProjectGroupMapper,
                                 KeyProjectStatusMapper keyProjectStatusMapper,GetUserService getUserService,
                                 OperationRecordMapper operationRecordMapper,TimeLimitService timeLimitService,
                                 AmountLimitMapper amountLimitMapper) {
        this.projectGroupMapper = projectGroupMapper;
        this.userProjectGroupMapper = userProjectGroupMapper;
        this.keyProjectStatusMapper = keyProjectStatusMapper;
        this.getUserService = getUserService;
        this.operationRecordMapper = operationRecordMapper;
        this.timeLimitService = timeLimitService;
        this.amountLimitMapper = amountLimitMapper;
    }

    @Transactional(rollbackFor = GlobalException.class)
    @Override
    public Result createKeyApply(KeyProjectApplyForm form) {
        //验证项目是否存在
        ProjectGroup projectGroup = projectGroupMapper.selectByPrimaryKey(form.getProjectId());
        if (projectGroup == null){
            throw new GlobalException(CodeMsg.PROJECT_GROUP_NOT_EXIST);
        }
        User user = getUserService.getCurrentUser();

        //验证用户是否有权限操作该项目组
        UserProjectGroup userProjectGroup = userProjectGroupMapper.selectByProjectGroupIdAndUserId(form.getProjectId(), Long.valueOf(user.getCode()));
        if (userProjectGroup == null || !userProjectGroup.getMemberRole().equals(MemberRole.PROJECT_GROUP_LEADER.getValue())){
            throw new GlobalException(CodeMsg.PERMISSION_DENNY);
        }

        //验证时间的合法性
        timeLimitService.validTime(TimeLimitType.KEY_DECLARE_LIMIT);

        //验证项目状态合法性
        validProjectStatus(ProjectStatus.LAB_ALLOWED.getValue(),projectGroup.getStatus());

        Long projectId = projectGroup.getId();
        if (projectGroup.getStatus() < ProjectStatus.LAB_ALLOWED.getValue()){
            throw new GlobalException(CodeMsg.PROJECT_IS_NOT_LAB_ALLOWED);
        }
        //创建重点项目状态
        if (keyProjectStatusMapper.getStatusByProjectId(projectGroup.getId())!=null) {
            throw new GlobalException(CodeMsg.PROJECT_CURRENT_STATUS_ERROR);
        }
        //将项目组表中的项目状态变为重点项目申请  （之后的状态查询都在重点项目状态表中查询）
        projectGroupMapper.updateProjectStatus(form.getProjectId(),ProjectStatus.KEY_PROJECT_APPLY.getValue());
        keyProjectStatusMapper.insert(projectId, KeyProjectStatus.TO_DE_CONFIRMED.getValue(),
                projectGroup.getSubordinateCollege(), Long.valueOf(user.getCode()));


        //将重点项目上报的操作存入历史
        OperationRecord operationRecord = new OperationRecord();
        operationRecord.setOperationUnit(RoleType.PROJECT_LEADER.getValue());
        operationRecord.setOperationType(OperationType.REPORT.getValue());
        operationRecord.setOperationExecutorId(Long.valueOf(user.getCode()));
        operationRecord.setRelatedId(projectGroup.getId());

        List<StuMember> stuMemberList = form.getMembers();

        if (stuMemberList!=null){
            for (StuMember stuMember:stuMemberList
            ) {
                userProjectGroupMapper.updateUserInfo(stuMember, new Date(), projectId);
            }
        }

        return Result.success();
    }

    @Override
    public Result getKeyProjectApplyingListByGuideTeacher() {
        User user = getUserService.getCurrentUser();
        if (user == null){
            throw new GlobalException(CodeMsg.AUTHENTICATION_ERROR);
        }
        Long userId = Long.valueOf(user.getCode());
        List<KeyProjectDTO> list = keyProjectStatusMapper.getKeyProjectListByUserId(userId,KeyProjectStatus.TO_DE_CONFIRMED.getValue());
        for (KeyProjectDTO keyProjectDTO :list
        ) {
            keyProjectDTO.setNumberOfTheSelected(userProjectGroupMapper.getMemberAmountOfProject(keyProjectDTO.getId(),null));
            keyProjectDTO.setGuidanceTeachers(userProjectGroupMapper.selectUserMemberVOListByMemberRoleAndProjectId(MemberRole.GUIDANCE_TEACHER.getValue(),keyProjectDTO.getId()));
        }
        return Result.success(list);
    }

    @Override
    public Result getKeyProjectApplyingListByLabAdmin() {
         return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.GUIDE_TEACHER_ALLOWED,null);
    }

    @Override
    public Result getKeyProjectApplyingListBySecondaryUnit() {
        return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.LAB_ALLOWED_AND_REPORTED,null);
    }

    @Override
    public Result getKeyProjectApplyingListByFunctionalDepartment() {
        return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.SECONDARY_UNIT_ALLOWED_AND_REPORTED,null);
    }

    @Override
    public Result getIntermediateInspectionKeyProject(Integer college) {
        return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.ESTABLISH,college);
    }

    @Override
    public Result getToBeConcludingKeyProject(Integer college) {
        return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.MID_TERM_INSPECTION,college);
    }

    private Result getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus status, Integer college){
        List<KeyProjectDTO> list = keyProjectStatusMapper.getKeyProjectDTOListByStatusAndCollege(status.getValue(),college);
        for (KeyProjectDTO keyProjectDTO :list
             ) {
            keyProjectDTO.setNumberOfTheSelected(userProjectGroupMapper.getMemberAmountOfProject(keyProjectDTO.getId(),null));
            keyProjectDTO.setGuidanceTeachers(userProjectGroupMapper.selectUserMemberVOListByMemberRoleAndProjectId(MemberRole.GUIDANCE_TEACHER.getValue(),keyProjectDTO.getId()));
        }
        return Result.success(list);
    }

    private KeyProjectStatus getNextStatusByRoleAndOperation(RoleType roleType, OperationType operationType){
        KeyProjectStatus keyProjectStatus;
        if (operationType == OperationType.REJECT) {
            keyProjectStatus = KeyProjectStatus.REJECT_MODIFY;
            return keyProjectStatus;
        }
        switch (roleType.getValue()){
            //如果是指导老师
            case 3:
                keyProjectStatus = KeyProjectStatus.GUIDE_TEACHER_ALLOWED;
                    break;
            //如果是实验室
            case 4:
                if (operationType == OperationType.AGREE){
                    keyProjectStatus = KeyProjectStatus.LAB_ALLOWED;
                }else {
                    keyProjectStatus = KeyProjectStatus.LAB_ALLOWED_AND_REPORTED;
                }
                break;
                //如果是二级单位
            case 5:
                if (operationType == OperationType.AGREE){
                    keyProjectStatus = KeyProjectStatus.SECONDARY_UNIT_ALLOWED;
                }else {
                    keyProjectStatus = KeyProjectStatus.SECONDARY_UNIT_ALLOWED_AND_REPORTED;
                }
                break;
                //如果是职能部门
            case 6:
                keyProjectStatus = KeyProjectStatus.ESTABLISH;
                    break;
            default:
                throw new GlobalException(CodeMsg.UNKNOWN_ROLE_TYPE_AND_OPERATION_TYPE);
        }
        return keyProjectStatus;
    }

    private Result operateKeyProjectOfSpecifiedRoleAndOperation(RoleType roleType, OperationType operationType,
                                                                List<KeyProjectCheck> list){
        User user = getUserService.getCurrentUser();
        if (user == null){
            throw new GlobalException(CodeMsg.AUTHENTICATION_ERROR);
        }
        //需要生成编号，通过操作人来判断学院信息
        Integer college = getUserService.getCurrentUser().getInstitute();
        if (college == null){
            throw new GlobalException(CodeMsg.COLLEGE_TYPE_NULL_ERROR);
        }

        List<OperationRecord> operationRecordList = new LinkedList<>();
        List<Long> idList = new LinkedList<>();
        for (KeyProjectCheck check:list
        ) {
            OperationRecord operationRecord = new OperationRecord();
            operationRecord.setOperationUnit(roleType.getValue());
            operationRecord.setOperationType(operationType.getValue());
            operationRecord.setOperationReason(check.getReason());
            operationRecord.setOperationExecutorId(Long.valueOf(user.getCode()));
            operationRecord.setRelatedId(check.getProjectId());
            operationRecordList.add(operationRecord);

            //如果是二级单位同意立项
            if (roleType == RoleType.SECONDARY_UNIT && operationType == OperationType.AGREE){
                String serialNumber = projectGroupMapper.selectByPrimaryKey(check.getProjectId()).getSerialNumber();
                //计算编号并在数据库中插入编号
                projectGroupMapper.updateProjectSerialNumber(check.getProjectId(),SerialNumberUtil.getSerialNumberOfProject(college,ProjectType.KEY.getValue(),serialNumber));
            }

            idList.add(check.getProjectId());
        }
        keyProjectStatusMapper.updateList(idList,getNextStatusByRoleAndOperation(roleType, operationType).getValue());
        operationRecordMapper.multiInsert(operationRecordList);
        return Result.success();
    }

    @Override
    public Result agreeKeyProjectByGuideTeacher(List<KeyProjectCheck> list) {
        //验证时间的合法性
        timeLimitService.validTime(TimeLimitType.TEACHER_KEY_CHECK_LIMIT);

        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.MENTOR, OperationType.AGREE,list);
    }

    @Override
    public Result agreeKeyProjectByLabAdministrator(List<KeyProjectCheck> list) {

        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.LAB_ADMINISTRATOR, OperationType.AGREE,list);
    }

    @Override
    public Result agreeKeyProjectBySecondaryUnit(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.SECONDARY_UNIT, OperationType.AGREE,list);
    }

    @Override
    public Result agreeKeyProjectByFunctionalDepartment(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.FUNCTIONAL_DEPARTMENT, OperationType.AGREE,list);
    }

    @Override
    public Result agreeIntermediateInspectionKeyProject(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.FUNCTIONAL_DEPARTMENT, OperationType.OFFLINE_CHECK,list);
    }

    @Override
    public Result agreeToBeConcludingKeyProject(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.FUNCTIONAL_DEPARTMENT, OperationType.CONCLUSION,list);
    }

    @Override
    public Result reportKeyProjectByLabAdministrator(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.LAB_ADMINISTRATOR, OperationType.REPORT,list);
    }

    @Override
    public Result rejectIntermediateInspectionKeyProject(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.FUNCTIONAL_DEPARTMENT, OperationType.OFFLINE_CHECK_REJECT,list);
    }

    @Override
    public Result rejectToBeConcludingKeyProject(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.FUNCTIONAL_DEPARTMENT, OperationType.CONCLUSION_REJECT,list);
    }

    @Override
    public Result reportKeyProjectBySecondaryUnit(List<KeyProjectCheck> list) {
        //验证数量
        User user = getUserService.getCurrentUser();
        Integer college = user.getInstitute();
        if (college == null){
            throw new GlobalException(CodeMsg.COLLEGE_TYPE_NULL_ERROR);
        }

        AmountAndTypeVO amountAndTypeVO = amountLimitMapper.getAmountAndTypeVOByCollegeAndProjectType(college,ProjectType.KEY.getValue());
        Integer currentAmount = keyProjectStatusMapper.getCountOfSpecifiedStatusAndProjectProject(ProjectStatus.SECONDARY_UNIT_ALLOWED_AND_REPORTED.getValue(),college);
        if (currentAmount + list.size() < amountAndTypeVO.getMaxAmount()) {
            throw new GlobalException(CodeMsg.KEY_PROJECT_AMOUNT_LIMIT);
        }

        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.SECONDARY_UNIT, OperationType.REPORT,list);
    }


    @Override
    public Result rejectKeyProjectByGuideTeacher(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.MENTOR, OperationType.REJECT,list);
    }

    @Override
    public Result getHistoricalKeyProjectInfo(HistoryQueryKeyProjectInfo info) {

        // TODO 权限验证

        List<ProjectGroup> list = projectGroupMapper.selectKeyHistoricalInfoByUnitAndOperation(info.getOperationUnit(),info.getOperationType());
        for (ProjectGroup projectGroup:list
        ) {
            projectGroup.setNumberOfTheSelected(userProjectGroupMapper.getMemberAmountOfProject(projectGroup.getId(),null));
            projectGroup.setGuidanceTeachers(userProjectGroupMapper.selectUserMemberVOListByMemberRoleAndProjectId(MemberRole.GUIDANCE_TEACHER.getValue(),projectGroup.getId()));
        }
        return Result.success(list);
    }

    @Override
    public Result getToBeReportedProjectByLabAdmin() {
        return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.LAB_ALLOWED,null);
    }

    @Override
    public Result getToBeReportedProjectBySecondaryUnit() {
        return getKeyProjectDTOListByStatusAndCollege(KeyProjectStatus.SECONDARY_UNIT_ALLOWED,null);
    }


    @Override
    public Result rejectKeyProjectByLabAdministrator(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.LAB_ADMINISTRATOR, OperationType.REJECT,list);
    }

    @Override
    public Result rejectKeyProjectBySecondaryUnit(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.SECONDARY_UNIT, OperationType.REJECT,list);
    }

    @Override
    public Result rejectKeyProjectByFunctionalDepartment(List<KeyProjectCheck> list) {
        return operateKeyProjectOfSpecifiedRoleAndOperation(RoleType.FUNCTIONAL_DEPARTMENT, OperationType.REJECT,list);
    }

    @Override
    public Result getKeyProjectDetailById(Long projectId) {
        List<ProjectHistoryInfo> list = operationRecordMapper.selectAllOfKeyProjectByProjectId(projectId);
        return Result.success(list);
    }

    @Override
    public Result conditionallyQueryOfKeyProject(QueryConditionForm form){
        return conditionallyQueryOfCheckedProject(form);
    }

    private Result conditionallyQueryOfCheckedProject(QueryConditionForm form) {
        List<Long>  projectIdList = projectGroupMapper.conditionQueryOfKeyProject(form);
        if (projectIdList.isEmpty()){
            return Result.success(null);
        }
        List<ProjectGroup> list = projectGroupMapper.selectAllByList(projectIdList);
        for (ProjectGroup projectGroup:list
        ) {
            Long id = projectGroup.getId();
            projectGroup.setNumberOfTheSelected(userProjectGroupMapper.getMemberAmountOfProject(id,null));
            projectGroup.setGuidanceTeachers(userProjectGroupMapper.selectUserMemberVOListByMemberRoleAndProjectId(null,id));
        }
        return Result.success(list);
    }

    private void validProjectStatus(Integer currentStatus,Integer rightStatus){
        if (!rightStatus.equals(currentStatus)){
            throw new GlobalException(CodeMsg.CURRENT_PROJECT_STATUS_ERROR);
        }
    }
}
