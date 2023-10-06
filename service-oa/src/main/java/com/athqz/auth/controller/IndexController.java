package com.athqz.auth.controller;

import com.athqz.auth.service.SysMenuService;
import com.athqz.auth.service.SysUserService;
import com.athqz.common.execption.GuiguException;
import com.athqz.common.jwt.JwtHelper;
import com.athqz.common.result.Result;
import com.athqz.common.utils.MD5;
import com.athqz.model.system.SysUser;
import com.athqz.vo.system.LoginVo;
import com.athqz.vo.system.RouterVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "后台登录管理")
@RestController
@RequestMapping("/admin/system/index")
public class IndexController {


    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuService sysMenuService;

    @PostMapping("login")
    public Result login(@RequestBody LoginVo loginVo){
        //{
        //  "code": 20000,
        //  "data": {
        //    "token": "admin-token"
        //  }
        //}
/*        Map<String, String> map=new HashMap<>();
        map.put("token","admin-token");
        return Result.ok(map);*/
        //1,获取输入用户名和密码

        //2,根据用户名查询数据库
        String username=loginVo.getUsername();
        LambdaQueryWrapper<SysUser> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername,username);
        SysUser sysUser = sysUserService.getOne(wrapper);

        //3，用户信息是否存在
        if(sysUser==null){
            throw new GuiguException(201,"用户不存在");
        }
        //4，判断密码
        //数据库存密码（MD5）
        String password_db = sysUser.getPassword();
        String password = loginVo.getPassword();
        String password_input = MD5.encrypt(password);
        if(!password_db.equals(password_input)){
            throw new GuiguException(201,"密码错误");
        }
        //5，判断用户是否被禁用
        if(sysUser.getStatus().intValue()==0){
            throw new GuiguException(201,"用户已经被禁用");
        }
        //6，使用JWT根据用户ID和用户名称生成toKen字符串
        String token= JwtHelper.createToken(sysUser.getId(),sysUser.getUsername());
        Map<String,Object> map=new HashMap<>();
        map.put("token",token);
        return Result.ok(map);
    }
    /**
     * 获取用户信息
     * @return
     */
    @GetMapping("info")
    public Result info(HttpServletRequest request) {
        //1，从请求头获取用户信息（获取请求头token字符串）
        String header = request.getHeader("token");
        //2，从token字符串获取用户id或者用户名称
        Long userId =JwtHelper.getUserId(header);
        //3，根据用户id查询数据库，把用户信息获取出来
        SysUser sysUser = sysUserService.getById(userId);
        //4，根据用户id获取用户可以操作菜单列表
        //查询数据库，动态构建出数据结构，进行显示

        List<RouterVo> routerList= sysMenuService.findUserMenuListByUserId(userId);

        //5，根据用户id获取用户可以操作操作列表
        List<String> permsList =sysMenuService.findUserPermsByUserId(userId);
        //6，返回响应的数据
        Map<String, Object> map = new HashMap<>();
        map.put("roles","[admin]");
        map.put("name","admin");
        map.put("avatar","https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
        //TODO 返回用户可以操作菜单
        map.put("routers",routerList);
        //TODO 返回用户可以操作按
        map.put("buttons",permsList);

        return Result.ok(map);
    }
    /**
     * 退出
     * @return
     */
    @PostMapping("logout")
    public Result logout(){
        return Result.ok();
    }
}
