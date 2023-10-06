package com.athqz.process.service.impl;

import com.athqz.model.process.ProcessType;
import com.athqz.process.mapper.OaProcessTypeMapper;
import com.athqz.process.service.OaProcessTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 审批类型 服务实现类
 * </p>
 *
 * @author plus
 * @since 2023-10-05
 */
@Service
public class OaProcessTypeServiceImpl extends ServiceImpl<OaProcessTypeMapper, ProcessType> implements OaProcessTypeService {

}
