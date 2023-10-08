package com.athqz.wechat.service;

import com.athqz.model.wechat.Menu;
import com.athqz.vo.wechat.MenuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 菜单 服务类
 * </p>
 *
 * @author plus
 * @since 2023-10-08
 */

public interface MenuService extends IService<Menu> {

    List<MenuVo> findMenuInfo();

    void syncMenu();

    void removeMenu();
}
