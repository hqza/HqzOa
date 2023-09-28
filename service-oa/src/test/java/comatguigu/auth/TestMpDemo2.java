package comatguigu.auth;

import com.atguigu.auth.ServiceAuthApplication;
import com.atguigu.auth.mapper.SysRoleMapper;
import com.atguigu.auth.service.SysRoleService;
import com.atguigu.model.system.SysRole;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = ServiceAuthApplication.class)
public class TestMpDemo2 {
    public static void main(String[] args) {

    }
    @Autowired
    private SysRoleMapper mapper;

    @Autowired
    private SysRoleService sysRoleService;


    @Test
    public void list(){
        List<SysRole> list = sysRoleService.list();
        System.out.println("666666666666666+list = " + list);
    }

    @Test
    public void getAll(){
        List<SysRole> sysRoles = mapper.selectList(null);
        System.out.println("sysRoles = " + sysRoles);
    }
    @Test
    public void testInsert(){
        SysRole sysRole = new SysRole();
        sysRole.setRoleName("角色管理员");
        sysRole.setRoleCode("role");
        sysRole.setDescription("角色管理员");

        int result = mapper.insert(sysRole);
        System.out.println(result); //影响的行数
        System.out.println(sysRole); //id自动回填
    }
    @Test
    public void testUpdateById(){
        SysRole sysRole = new SysRole();
        sysRole.setId(10L);
        sysRole.setRoleName("角色管理员1");
        int result = mapper.updateById(sysRole);
        System.out.println(result);

    }
    @Test
    public void testDeleteBatchIds() {
        int result = mapper.deleteBatchIds(Arrays.asList(1, 1));
        System.out.println(result);
    }
}
