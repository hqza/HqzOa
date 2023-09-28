package com.atguigu.auth.service;
import com.atguigu.model.system.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author plus
 * @since 2023-09-04
 */
public interface SysUserService extends IService<SysUser> {
    void updateStatus(Long id, Integer status);
}
