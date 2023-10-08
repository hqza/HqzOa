package com.athqz.wechat.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.athqz.model.wechat.Menu;
import com.athqz.vo.wechat.MenuVo;
import com.athqz.wechat.mapper.MenuMapper;
import com.athqz.wechat.service.MenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单 服务实现类
 * </p>
 *
 * @author plus
 * @since 2023-10-08
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {


    @Value("${wechaturlhttp}")
    String wechaturlhttp;
    @Override
    public List<MenuVo> findMenuInfo() {
        //查询所有菜单list集合
        List<MenuVo> list = new ArrayList<>();
        List<Menu> menuList = baseMapper.selectList(null);
        //查询所有一级菜单parent_id=0,返回一级菜单
        List<Menu> oneMenuList = menuList.stream().filter(menu -> menu.getParentId().longValue() == 0).collect(Collectors.toList());
        //遍历一级菜单
        for (Menu oneMenu : oneMenuList) {
            MenuVo oneMenuVo = new MenuVo();
            BeanUtils.copyProperties(oneMenu, oneMenuVo);
            //通过一级菜单获得二级菜单，一级id和和其他paren_id对比
            List<Menu> twoMenuList = menuList.stream()
                    .filter(menu -> menu.getParentId().longValue() == oneMenu.getId())
                    .sorted(Comparator.comparing(Menu::getSort))
                    .collect(Collectors.toList());

            //把一级菜单里面所有二级菜单获取到，封装一级菜单children集合里面
            List<MenuVo> children = new ArrayList<>();
            for (Menu twoMenu : twoMenuList) {
                MenuVo twoMenuVo = new MenuVo();
                BeanUtils.copyProperties(twoMenu, twoMenuVo);
                children.add(twoMenuVo);
            }
            oneMenuVo.setChildren(children);
            //放到集合
            list.add(oneMenuVo);
        }
        return list;
    }

    @Autowired
    private WxMpService wxMpService;

    @Override
    public void syncMenu() {
        //菜单数据查询出来，然后进行封装
        List<MenuVo> menuVoList = this.findMenuInfo();
        //菜单
        JSONArray buttonList = new JSONArray();
        for(MenuVo oneMenuVo : menuVoList) {
            JSONObject one = new JSONObject();
            one.put("name", oneMenuVo.getName());
            if(CollectionUtils.isEmpty(oneMenuVo.getChildren())) {
                one.put("type", oneMenuVo.getType());
                one.put("url", wechaturlhttp+"/#"+oneMenuVo.getUrl());
            } else {
                JSONArray subButton = new JSONArray();
                for(MenuVo twoMenuVo : oneMenuVo.getChildren()) {
                    JSONObject view = new JSONObject();
                    view.put("type", twoMenuVo.getType());
                    if(twoMenuVo.getType().equals("view")) {
                        view.put("name", twoMenuVo.getName());
                        //H5页面地址
                        view.put("url", wechaturlhttp+"#"+twoMenuVo.getUrl());
                    } else {
                        view.put("name", twoMenuVo.getName());
                        view.put("key", twoMenuVo.getMeunKey());
                    }
                    subButton.add(view);
                }
                one.put("sub_button", subButton);
            }
            buttonList.add(one);
        }
        //菜单
        JSONObject button = new JSONObject();
        button.put("button", buttonList);
        try {
            wxMpService.getMenuService().menuCreate(button.toJSONString());
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    @Override
    public void removeMenu() {
        wxMpService.getMenuService().menuDelete();
    }
}
