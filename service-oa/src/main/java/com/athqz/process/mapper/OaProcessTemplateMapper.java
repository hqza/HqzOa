package com.athqz.process.mapper;
import com.athqz.model.process.ProcessTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 审批模板 Mapper 接口
 * </p>
 *
 * @author plus
 * @since 2023-10-05
 */

@Mapper
public interface OaProcessTemplateMapper extends BaseMapper<ProcessTemplate> {
    IPage<ProcessTemplate> selectPage(Page<ProcessTemplate> pageParam);
}
