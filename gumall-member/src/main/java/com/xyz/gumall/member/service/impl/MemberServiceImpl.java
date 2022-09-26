package com.xyz.gumall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xyz.common.utils.HttpUtils;
import com.xyz.gumall.member.dao.MemberLevelDao;
import com.xyz.gumall.member.entity.MemberLevelEntity;
import com.xyz.gumall.member.exception.PhoneExistException;
import com.xyz.gumall.member.exception.UsernameExistException;
import com.xyz.gumall.member.vo.MemberLoginVo;
import com.xyz.gumall.member.vo.MemberRegisterVo;
import com.xyz.gumall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xyz.common.utils.PageUtils;
import com.xyz.common.utils.Query;

import com.xyz.gumall.member.dao.MemberDao;
import com.xyz.gumall.member.entity.MemberEntity;
import com.xyz.gumall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegisterVo vo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        // 检查手机号和用户名是否唯一
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        entity.setNickname(vo.getUserName());

        //密码要加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        memberDao.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String username) {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        // 1、去数据库查询 select * from  ums_member where username=? or mobile =?
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>()
                .eq("username", loginacct).or()
                .eq("mobile", loginacct));
        if (entity == null) {
            // 登录失败
            return null;
        } else {
            // 获取数据库的密码
            String passwordDb = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            // 和用户密码进行校验
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if(matches) {
                // 密码验证成功 返回对象
                return entity;
            } else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "token " + socialUser.getAccess_token());
        String id = null;
        try {
            HttpResponse response = HttpUtils.doGet("https://api.github.com", "/user", "get", headers, null);
            if (response.getStatusLine().getStatusCode() == 200) {
                String json = EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSON.parseObject(json);
                id = jsonObject.getString("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        socialUser.setId(id);
//        String id = socialUser.getId();
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", socialUser.getId()));

        if (memberEntity != null) {
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());

            memberDao.updateById(update);
            return memberEntity;
        }else {
            MemberEntity regist = null;
            String login = null;
            try {
                regist = new MemberEntity();
//                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "token " + socialUser.getAccess_token());
                HttpResponse response = HttpUtils.doGet("https://api.github.com", "/user", "get", headers, null);
                if (response.getStatusLine().getStatusCode() == 200) {
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    String name = jsonObject.getString("name");
                    login = jsonObject.getString("login");

//            regist.setSocialUid(socialUser.getId());
                    regist.setSocialUid(id);
                    regist.setAccessToken(socialUser.getAccess_token());
                    regist.setNickname(name);
                    memberDao.insert(regist);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return regist;
        }
    }

}
