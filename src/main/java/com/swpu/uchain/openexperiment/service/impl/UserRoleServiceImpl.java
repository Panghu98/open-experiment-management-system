package com.swpu.uchain.openexperiment.service.impl;

import com.swpu.uchain.openexperiment.domain.User;
import com.swpu.uchain.openexperiment.enums.RoleType;
import com.swpu.uchain.openexperiment.exception.GlobalException;
import com.swpu.uchain.openexperiment.mapper.UserMapper;
import com.swpu.uchain.openexperiment.mapper.UserRoleMapper;
import com.swpu.uchain.openexperiment.domain.UserRole;
import com.swpu.uchain.openexperiment.enums.CodeMsg;
import com.swpu.uchain.openexperiment.form.permission.UserRoleForm;
import com.swpu.uchain.openexperiment.result.Result;
import com.swpu.uchain.openexperiment.service.GetUserService;
import com.swpu.uchain.openexperiment.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: clf
 * @Date: 19-1-22
 * @Description:
 * 实现用户角色管理模块
 */
@Service
public class UserRoleServiceImpl implements UserRoleService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private GetUserService getCurrentUser;
    @Override
    public boolean insert(UserRole userRole) {
        return userRoleMapper.insert(userRole) == 1;
    }

    @Override
    public Result deleteByUserIdRoleId(Long userId, Integer roleId) {
        if (userRoleMapper.selectByUserId(userId) == null){
            throw new GlobalException(CodeMsg.USER_NO_EXIST);
        }
        int result = userRoleMapper.updateUserRoleByUserIdAndRole(userId, RoleType.MENTOR.getValue());
        if (result != 1){
            throw new GlobalException(CodeMsg.USER_INFORMATION_MATCH_ERROR);
        }
        return Result.success();
    }

    /**
     * 多角色身份验证
     * @param roleType 需要的角色
     * @return
     */
    @Override
    public boolean validContainsUserRole(RoleType roleType) {
        User user  = getCurrentUser.getCurrentUser();
        //用户角色组
        List<UserRole> list = userRoleMapper.selectByUserId(Long.valueOf(user.getCode()));
        if (list == null || list.size() == 0) {
            throw new GlobalException(CodeMsg.PERMISSION_DENNY);
        }

        for (UserRole userRole:list
        ) {
            if (roleType.getValue().equals(userRole.getRoleId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Result addUserRole(UserRoleForm userRoleForm) {
        if (userMapper.selectByUserCode(userRoleForm.getUserId().toString()) == null){
            throw new GlobalException(CodeMsg.USER_NO_EXIST);
        }
        UserRole userRole = userRoleMapper.selectByUserIdAndRoleId(userRoleForm.getUserId(), userRoleForm.getRoleId());

        if (userRole != null){
            return Result.error(CodeMsg.USER_ROLE_HAD_EXIST);
        }

        int result ;

        List<UserRole> list = userRoleMapper.selectByUserId(userRoleForm.getUserId());

        //不存在则增加
        if (list == null) {
            UserRole userRoleDomain = new UserRole();
            userRoleDomain.setRoleId(userRoleForm.getRoleId());
            userRoleDomain.setUserId(userRoleForm.getUserId());
            result = userRoleMapper.insert(userRoleDomain);
        //存在则更新
        }else {
            if (validContainsUserRole(RoleType.NORMAL_STU) ||
                    //暂时不存在这种情况
                validContainsUserRole(RoleType.PROJECT_LEADER)) {
                throw new GlobalException(CodeMsg.STUDENT_CANT_GAIN_THIS_PERMISSION);
            }
            result = userRoleMapper.updateUserRoleByUserIdAndRole(userRoleForm.getUserId(),userRoleForm.getRoleId());
        }
        if (result != 1) {
            return Result.error(CodeMsg.ADD_ERROR);
        }
        return Result.success();
    }

    @Override
    public List<UserRole> selectUsersByRoleId(Long roleId) {
        return userRoleMapper.selectByRoleId(roleId);
    }

    @Override
    public Result getUserInfoByRole() {
        return Result.success(userRoleMapper.getUserInfoByRole(RoleType.MENTOR.getValue()));
    }
}
