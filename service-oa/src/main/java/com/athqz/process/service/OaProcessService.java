package com.athqz.process.service;

import com.athqz.model.process.Process;
import com.athqz.vo.process.ProcessQueryVo;
import com.athqz.vo.process.ProcessVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
/**
 * <p>
 * 审批类型 服务类
 * </p>
 *
 * @author plus
 * @since 2023-10-06
 */
public interface OaProcessService extends IService<Process> {
    IPage<ProcessVo> selectPage(Page<ProcessVo> page, @Param("vo") ProcessQueryVo processQueryVo);
    void deployByZip(String deployPath);
}
