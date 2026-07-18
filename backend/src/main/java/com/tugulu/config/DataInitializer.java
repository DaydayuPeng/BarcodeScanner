package com.tugulu.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tugulu.entity.CmsContent;
import com.tugulu.entity.SysUser;
import com.tugulu.mapper.CmsContentMapper;
import com.tugulu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final CmsContentMapper cmsContentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureUser("admin", "Admin", "admin");
        ensureUser("employee", "Employee", "employee");
        ensureCms("company", "About TuGuLu",
                "<p>We are a professional warehouse and logistics service provider.</p>",
                "<p>我们是专业的仓储物流服务商。</p>");
        ensureCms("service", "Our Services",
                "<p>Inbound scanning, packing, sealing and shipping services.</p>",
                "<p>提供入库扫描、打包、封箱与发货服务。</p>");
        ensureCms("product", "Our Products",
                "<p>Warehouse management and tracking solutions.</p>",
                "<p>仓储管理与轨迹查询解决方案。</p>");
    }

    private void ensureUser(String username, String realName, String role) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        String encoded = passwordEncoder.encode("admin123");
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(encoded);
            user.setRealName(realName);
            user.setRole(role);
            user.setStatus(1);
            sysUserMapper.insert(user);
        } else {
            user.setPassword(encoded);
            user.setRealName(realName);
            user.setRole(role);
            user.setStatus(1);
            sysUserMapper.updateById(user);
        }
    }

    private void ensureCms(String type, String titleEn, String contentEn, String contentZh) {
        Long count = cmsContentMapper.selectCount(new LambdaQueryWrapper<CmsContent>().eq(CmsContent::getType, type));
        if (count == 0) {
            CmsContent content = new CmsContent();
            content.setType(type);
            content.setTitleEn(titleEn);
            content.setContentEn(contentEn);
            content.setContentZh(contentZh);
            content.setImages("[]");
            content.setSortOrder(100);
            cmsContentMapper.insert(content);
        }
    }
}
