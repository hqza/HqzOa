package com.athqz.process.service.impl;

import com.athqz.auth.service.SysUserService;
import com.athqz.model.process.ProcessRecord;
import com.athqz.model.system.SysUser;
import com.athqz.process.mapper.OaProcessRecordMapper;
import com.athqz.process.service.OaProcessRecordService;
import com.athqz.security.custom.LoginUserInfoHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 审批记录 服务实现类
 * </p>
 *
 * @author plus
 * @since 2023-10-07
 */
@Service
public class OaProcessRecordServiceImpl extends ServiceImpl<OaProcessRecordMapper, ProcessRecord> implements OaProcessRecordService {


    @Autowired
    private OaProcessRecordMapper processRecordMapper;

    @Autowired
    private SysUserService sysUserService;

    @Override
    public void record(Long processId, Integer status, String description) {
        SysUser sysUser = sysUserService.getById(LoginUserInfoHelper.getUserId());
        ProcessRecord processRecord = new ProcessRecord();
        processRecord.setProcessId(processId);
        processRecord.setStatus(status);
        processRecord.setDescription(description);
        processRecord.setOperateUserId(sysUser.getId());
        processRecord.setOperateUser(sysUser.getName());
        processRecordMapper.insert(processRecord);
    }
}
