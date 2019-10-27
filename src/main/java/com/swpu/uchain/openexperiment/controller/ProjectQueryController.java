package com.swpu.uchain.openexperiment.controller;

import com.swpu.uchain.openexperiment.enums.CodeMsg;
import com.swpu.uchain.openexperiment.form.project.HistoryQueryProjectInfo;
import com.swpu.uchain.openexperiment.form.query.QueryConditionForm;
import com.swpu.uchain.openexperiment.result.Result;
import com.swpu.uchain.openexperiment.service.ProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 项目模块查询接口
 * @author panghu
 */
@CrossOrigin
@RestController
@Api(tags = "项目(普通)模块查询接口")
@RequestMapping("/project")
public class ProjectQueryController {

    private ProjectService projectService;


    @Autowired
    public ProjectQueryController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @ApiOperation("学生获取可参与的开放性选题")
    @GetMapping("/getAllOpenTopicByStudent")
    public Result getAllOpenTopic(){
        return projectService.getAllOpenTopic();
    }

    @ApiOperation("获取项目的立项信息--可使用")
    @GetMapping(value = "/getApplyInfo", name = "获取项目的立项信息")
    public Result getApplyInfo(Long projectGroupId){
        return projectService.getApplyForm(projectGroupId);
    }

    @GetMapping(value = "/getApplyingJoinInfo", name = "获取当前用户（限老师身份）指导项目的申请参加列表")
    @ApiOperation("获取当前用户（限老师身份）指导项目的申请参加列表--可使用")
    public Result getApplyingJoinInfo(){
        return Result.success(projectService.getJoinInfo());
    }


    @ApiOperation("获取当前用户参与的某状态的项目信息, 项目状态: 不传(所有), 0(申报), 1(立项), 2(驳回修改),3(已上报学院领导), 4(中期检查), 5(结项)")
    @GetMapping(value = "/getOwnProjects", name = "获取自己相关的项目信息")
    public Result getOwnProjects(@RequestParam(required = false) Integer projectStatus){
        return projectService.getCurrentUserProjects(projectStatus);
    }

    @ApiOperation("通过项目ID查看项目详情--项目进度信息")
    @GetMapping("/getProjectDetailById")
    public Result getProjectDetailById(Long projectId){
        return projectService.getProjectDetailById(projectId);
    }

    @ApiOperation("实验室获取待立项审核的项目")
    @GetMapping(value = "getPendingApprovalProjectByLabAdministrator")
    public Result getPendingApprovalProjectByLabAdministrator (){
        return projectService.getPendingApprovalProjectByLabAdministrator();
    }

    @ApiOperation("二级单位获取待立项审核的项目")
    @GetMapping(value = "getPendingApprovalProjectBySecondaryUnit")
    public Result getPendingApprovalProjectBySecondaryUnit (){
        return projectService.getPendingApprovalProjectBySecondaryUnit();
    }

    @ApiOperation("职能部门获取待立项审核的项目")
    @GetMapping(value = "getPendingApprovalProjectByFunctionalDepartment")
    public Result getPendingApprovalProjectByFunctionalDepartment (){

        return projectService.getPendingApprovalProjectByFunctionalDepartment();
    }

    @ApiOperation("根据项目名模糊查询项目--可使用")
    @GetMapping(value = "/selectProject", name = "根据项目名模糊查询项目--可使用")
    public Result selectProject(String name){
        if (StringUtils.isEmpty(name)){
            return Result.error(CodeMsg.PARAM_CANT_BE_NULL);
        }
        return Result.success(projectService.selectByProjectName(name));
    }

    @ApiOperation("通过项目ID查看项目详情--成员和项目信息")
    @GetMapping("/getProjectGroupDetailVOByProjectId")
    public Result getProjectGroupDetailByProjectId(Long projectId){
        return projectService.getProjectGroupDetailVOByProjectId(projectId);
    }


    @ApiOperation("二级单位查看待上报的项目")
    @GetMapping("/getToBeReportedProjectBySecondaryUnit")
    public Result getToBeReportedProjectBySecondaryUnit(){
        return projectService.getToBeReportedProjectBySecondaryUnit();
    }

    @ApiOperation("实验室主任查看待上报项目")
    @GetMapping("/getToBeReportedProjectByLabLeader")
    public Result getToBeReportedProjectByLabLeader(){
        return projectService.getToBeReportedProjectByLabLeader();
    }

    @ApiOperation("职能部门根据条件查询项目信息")
    @PostMapping("/conditionallyQueryOfCheckedProjectByFunctionalDepartment")
    public Result conditionallyQueryOfCheckedProjectByFunctionalDepartment(@RequestBody QueryConditionForm form){
        return projectService.conditionallyQueryOfCheckedProjectByFunctionalDepartment(form);
    }

    @ApiOperation("所有的单位查看历史的操作--以上报，已驳回")
    @PostMapping("/getHistoricalProjectInfoByUnitAndOperation")
    public Result getHistoricalProjectInfoByUnitAndOperation(@Valid @RequestBody HistoryQueryProjectInfo info ){
        return projectService.getHistoricalProjectInfoByUnitAndOperation(info);
    }
}
