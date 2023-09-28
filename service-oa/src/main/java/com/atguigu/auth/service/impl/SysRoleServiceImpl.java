package com.atguigu.auth.service.impl;

import com.atguigu.auth.mapper.SysRoleMapper;
import com.atguigu.auth.mapper.SysUserRoleMapper;
import com.atguigu.auth.service.SysRoleService;
import com.atguigu.auth.service.SysUserRoleService;
import com.atguigu.model.system.SysRole;
import com.atguigu.model.system.SysUserRole;
import com.atguigu.vo.system.AssginRoleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.velocity.runtime.directive.contrib.For;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    @Autowired
    private SysUserRoleService SysRoleService;

    @Override
    public Map<String, Object> findRoleByUserId(Long userId) {
        //查询所有的角色
        List<SysRole> allRolesList = this.list();
/*

        //拥有的角色id
        List<SysUserRole> existUserRoleList = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId).select(SysUserRole::getRoleId));
        List<Long> existRoleIdList = existUserRoleList.stream().map(c->c.getRoleId()).collect(Collectors.toList());

        //对角色进行分类
        List<SysRole> assginRoleList = new ArrayList<>();
        for (SysRole role : allRolesList) {
            //已分配
            if(existRoleIdList.contains(role.getId())) {
                assginRoleList.add(role);
            }
        }

        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("assginRoleList", assginRoleList);
        roleMap.put("allRolesList", allRolesList);

*/


        //根据UserId火车对应所有角色
        LambdaQueryWrapper<SysUserRole> wraper=new LambdaQueryWrapper<>();
        wraper.eq(SysUserRole::getUserId,userId);
        List<SysUserRole> existUserRoleLists= SysRoleService.list(wraper);
        //从查询出来的用户id对应角色list集合，获得所有角色id
        List<Long> existRoleIdList = existUserRoleLists.stream().map(c->c.getRoleId()).collect(Collectors.toList());
        List<SysRole> assignRoleList=new ArrayList<>();
        for (SysRole sysRole:allRolesList){
            if(existRoleIdList.contains(sysRole.getId()))
                assignRoleList.add(sysRole);
        }
        Map<String, Object> roleMap = new HashMap<>();
        roleMap.put("assginRoleList", assignRoleList);
        roleMap.put("allRolesList", allRolesList);
        return roleMap;
    }

    //为用户分配角色
    @Transactional
    @Override
    public void doAssign(AssginRoleVo assginRoleVo) {
        //把用户之前分配校色数据删除，用户校色关系表里面，更具userid删除
        LambdaQueryWrapper<SysUserRole> wrapper=new LambdaQueryWrapper<SysUserRole>();
        wrapper.eq(SysUserRole::getUserId,assginRoleVo.getUserId());
        SysRoleService.remove(wrapper);
        List<Long> roleIdList=assginRoleVo.getRoleIdList();
        for (Long roleId:roleIdList){
            if(StringUtils.isEmpty(roleId)){
                continue;
            }
            SysUserRole sysUserRole=new SysUserRole();
            sysUserRole.setUserId(assginRoleVo.getUserId());
            sysUserRole.setRoleId(roleId);
            SysRoleService.save(sysUserRole);
        }
    }
}
