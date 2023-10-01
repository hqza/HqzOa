package com.atguigu.auth.service.impl;

import com.atguigu.auth.mapper.SysMenuMapper;
import com.atguigu.auth.service.SysMenuService;
import com.atguigu.auth.service.SysRoleMenuService;
import com.atguigu.auth.utils.MeunHelper;
import com.atguigu.common.execption.GuiguException;
import com.atguigu.model.system.SysMenu;
import com.atguigu.model.system.SysRoleMenu;
import com.atguigu.vo.system.AssginMenuVo;
import com.atguigu.vo.system.MetaVo;
import com.atguigu.vo.system.RouterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.FormatFlagsConversionMismatchException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author plus
 * @since 2023-09-28
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;


    @Override
    public List<SysMenu> findNodes() {
        List<SysMenu> sysMenuList = baseMapper.selectList(null);
        List<SysMenu> resultList= MeunHelper.buildTree(sysMenuList);
        return resultList;
    }

    @Override
    public void removeMenuById(Long id) {
        LambdaQueryWrapper <SysMenu> wrapper =new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId,id);
        Integer count = baseMapper.selectCount(wrapper);
        if(count>0){
            throw new GuiguException(201,"菜单不能删除");
        }
        baseMapper.deleteById(id);
    }
    //查询所有菜单和角色分配的菜单
    @Override
    public List<SysMenu> findSysMenuByRoleId(Long roleId) {
        //添加条件，status=1
        LambdaQueryWrapper<SysMenu> wrapperSysMenu=new LambdaQueryWrapper<>();
        wrapperSysMenu.eq(SysMenu::getStatus,1);
        List<SysMenu> allSsMenuList = baseMapper.selectList(wrapperSysMenu);

        //根据角色id roleId 查询 角色菜单关系表里面，角色id对应所有的菜单id
        LambdaQueryWrapper<SysRoleMenu> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId,roleId);
        List<SysRoleMenu> sysRoleMenus = sysRoleMenuService.list(wrapper);

        //根据获取菜单id,获取对应菜单对象
        List<Long> menuIdList = sysRoleMenus.stream().map(c -> c.getMenuId()).collect(Collectors.toList());

        //拿着菜单id和菜单集合里面的id进行比较，相同就封装
        allSsMenuList.stream().forEach(item->{
            if(menuIdList.contains(item.getId())){
                item.setSelect(true);
            }else {
                item.setSelect(false);
            }
        });

        //返回规定格式菜单列表

        List<SysMenu> sysMenuList = MeunHelper.buildTree(allSsMenuList);
        return sysMenuList;
    }

    //根据角色id分配权限
    @Override
    public void doAssign(AssginMenuVo assignMenuVo) {
        //根据角色ID，删除菜单角色表，分配数据
        LambdaQueryWrapper<SysRoleMenu> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId,assignMenuVo.getRoleId());
        sysRoleMenuService.remove(wrapper);


        //从参数里面获取角色新分配菜单id列表
        List<Long> menuIdList=assignMenuVo.getMenuIdList();
        for (Long menuId :menuIdList){
            if(StringUtils.isEmpty(menuId)){
                continue;
            }else {
                SysRoleMenu sysRoleMenu=new SysRoleMenu();
                sysRoleMenu.setMenuId(menuId);
                sysRoleMenu.setRoleId(assignMenuVo.getRoleId());
                sysRoleMenuService.save(sysRoleMenu);
            }
        }



        //进行遍历，把每个id数据添加菜单角色表
    }

    //更具userid获得角色权限
    @Override
    public List<RouterVo> findUserMenuListByUserId(Long userId) {
        List<SysMenu> sysMenuList=null;
        //判断当前用户是否是管理员，userId=1是管理员
        //如果是管理员，查询所有菜单
        if(userId.longValue()==1){
            LambdaQueryWrapper<SysMenu> wrapper =new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus,1);
            wrapper.orderByAsc(SysMenu::getSortValue);
            sysMenuList = baseMapper.selectList(wrapper);
        }else {
            //如果不是管理员，根据userId查询可以操作菜单列表
            //多表关联查询：用户角色关系表，角色菜单关系表，菜单表
            sysMenuList=baseMapper.findMenuListByUserId(userId);
        }
        //把查询出来数据列表，构建成框架要求的路由结构。
        //使用菜单工具类构建树形结构
        List<SysMenu> sysMenuTreeList = MeunHelper.buildTree(sysMenuList);
        //构建成框架要求的路由结构
        List<RouterVo> routerVos= this.buidRouter(sysMenuTreeList);
        //多表关联查询：
        return routerVos;
    }


    //构建成框架要求的路由结构
    private List<RouterVo> buidRouter(List<SysMenu> menus) {
        //创建集合List存储最终数据
        List<RouterVo> routers=new ArrayList<>();
        //menus遍历
        for (SysMenu menu: menus){
            RouterVo router=new RouterVo();
            router.setHidden(false);
            router.setAlwaysShow(false);
            router.setPath(getRouterPath(menu));
            router.setComponent(menu.getComponent());
            router.setMeta(new MetaVo(menu.getName(), menu.getIcon()));
            //下一层数据
            List<SysMenu> children = menu.getChildren();
            //
            if(menu.getType().intValue()==1){
                //加载出来隐藏路由
                List<SysMenu> hiddenMenuList = children.stream()
                        .filter(item -> !StringUtils.isEmpty(item.getComponent()))
                        .collect(Collectors.toList());
                for (SysMenu hiddenMenu:hiddenMenuList){
                    RouterVo hiddenRouter = new RouterVo();
                    //true  隐藏路由
                    hiddenRouter.setHidden(true);
                    hiddenRouter.setAlwaysShow(false);
                    hiddenRouter.setPath(getRouterPath(hiddenMenu));
                    hiddenRouter.setComponent(hiddenMenu.getComponent());
                    hiddenRouter.setMeta(new MetaVo(hiddenMenu.getName(), hiddenMenu.getIcon()));
                    routers.add(hiddenRouter);
                }
            }else {
                if(!CollectionUtils.isEmpty(children)){
                    if(children.size()>0){
                        router.setAlwaysShow(true);
                    }
                    //递归
                    router.setChildren(buidRouter(children));
                }
            }
            routers.add(router);
        }
        return routers;
    }
    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = "/" + menu.getPath();
        if(menu.getParentId().intValue() != 0) {
            routerPath = menu.getPath();
        }
        return routerPath;
    }

    //根据userId获得按钮权限
    @Override
    public List<String> findUserPermsByUserId(Long userId) {
        //判断是否是管理员，如果是管理员，查询所有按钮列表
        List<SysMenu> sysMenuList=null;
        if(userId.longValue()==1){
            LambdaQueryWrapper<SysMenu> wrapper =new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getStatus,1);
            sysMenuList = baseMapper.selectList(wrapper);
        }else {
            //如果不是管理员，根据userId查询可以操作按钮列表
            //多表关联查询：用户角色关系表，角色菜单关系表，菜单表
            sysMenuList=baseMapper.findMenuListByUserId(userId);
        }
        //从查询出来的数据里面，获取可以操作按钮查询值的List集合，返回
        List<String> permsList = sysMenuList.stream()
                .filter(item -> item.getType() == 2)
                .map(item -> item.getPerms())
                .collect(Collectors.toList());
        return permsList;
    }
}
