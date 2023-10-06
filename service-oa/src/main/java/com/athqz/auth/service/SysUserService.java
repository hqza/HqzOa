package com.athqz.auth.service;
import com.athqz.model.system.SysUser;
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

    SysUser getByUsername(String username);
}
