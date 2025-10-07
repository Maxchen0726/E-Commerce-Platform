package com.rabbiter.em.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.common.Result;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.constants.RedisConstants;
import com.rabbiter.em.entity.LoginForm;
import com.rabbiter.em.entity.User;
import com.rabbiter.em.entity.dto.UserDTO;
import com.rabbiter.em.exception.ServiceException;
import com.rabbiter.em.mapper.UserMapper;
import com.rabbiter.em.utils.TokenUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private org.springframework.data.redis.core.RedisTemplate<String, User> redisTemplate;

    /* ========================  登录  ======================== */
    public UserDTO login(LoginForm loginForm) {
        // 1) 仅按用户名查
        User user = getOne(new QueryWrapper<User>().eq("username", loginForm.getUsername()));
        if (user == null) throw new ServiceException(Constants.CODE_403, "用户名或密码错误");

        // 2) 校验并在需要时完成迁移
        if (!verifyAndMigrate(user, loginForm.getPassword())) {
            throw new ServiceException(Constants.CODE_403, "用户名或密码错误");
        }

        // 3) 签发 token
        String token = TokenUtils.genToken(String.valueOf(user.getId()), user.getUsername());

        // 4) 缓存（不带口令字段）
        User cacheUser = new User();
        BeanUtils.copyProperties(user, cacheUser);
        cacheUser.setPasswordHash(null);
        cacheUser.setLegacyPlainPassword(null);

        redisTemplate.opsForValue().set(RedisConstants.USER_TOKEN_KEY + token, cacheUser);
        redisTemplate.expire(RedisConstants.USER_TOKEN_KEY + token, RedisConstants.USER_TOKEN_TTL, TimeUnit.MINUTES);

        // 5) 返回 DTO
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        userDTO.setToken(token);
        return userDTO;
    }

    /* ========================  注册  ======================== */
    public User register(LoginForm loginForm) {
        // 唯一性
        User exists = getOne(new QueryWrapper<User>().eq("username", loginForm.getUsername()));
        if (exists != null) throw new ServiceException(Constants.CODE_403, "用户名已被使用");

        User user = new User();
        BeanUtils.copyProperties(loginForm, user);
        user.setNickname("新用户");
        user.setRole("user");

        // 强哈希写入
        user.setPasswordHash(passwordEncoder.encode(loginForm.getPassword()));
        user.setLegacyPlainPassword(null);

        save(user);
        return user;
    }

    /* ========================  便捷查询  ======================== */
    public User getOne(String username){
        return getOne(new QueryWrapper<User>().eq("username", username));
    }

    /* ========================  新增 / 修改资料  ======================== */
    public Result saveUpdate(User user) {
        if (user.getId() != null) {
            // 修改资料：不改口令
            User old = this.baseMapper.selectById(user.getId());
            if (old == null) return Result.error("404", "用户不存在");

            old.setNickname(ObjectUtils.isEmpty(user.getNickname()) ? old.getNickname() : user.getNickname());
            old.setAvatarUrl(ObjectUtils.isEmpty(user.getAvatarUrl()) ? old.getAvatarUrl() : user.getAvatarUrl());
            old.setRole(ObjectUtils.isEmpty(user.getRole()) ? old.getRole() : user.getRole());
            old.setPhone(ObjectUtils.isEmpty(user.getPhone()) ? old.getPhone() : user.getPhone());
            old.setEmail(ObjectUtils.isEmpty(user.getEmail()) ? old.getEmail() : user.getEmail());
            old.setAddress(ObjectUtils.isEmpty(user.getAddress()) ? old.getAddress() : user.getAddress());
            super.updateById(old);
            return Result.success("修改成功");
        } else {
            // 新增用户：必须提供 newPassword
            if (!ObjectUtils.isEmpty(this.getOne(user.getUsername()))) {
                return Result.error("400", "用户名已存在");
            }
            if (ObjectUtils.isEmpty(user.getNewPassword())) {
                return Result.error("400", "新增用户必须提供初始密码");
            }
            user.setPasswordHash(passwordEncoder.encode(user.getNewPassword()));
            user.setLegacyPlainPassword(null);
            super.save(user);
            return Result.success("新增成功");
        }
    }

    @Override
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    /* ========================  重置密码  ======================== */
    public void resetPassword(String id, String newPassword) {
        User user = this.getById(id);
        if (user == null) return;
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLegacyPlainPassword(null);
        this.updateById(user);
    }

    /* ========================  辅助：校验 & 迁移  ======================== */
    /**
     * 优先使用 password_hash 校验；否则使用旧列 password（MD5）校验，
     * 通过后立刻迁移到强哈希并清空旧列。
     */
    private boolean verifyAndMigrate(User user, String rawPassword) {
        // 1) 新哈希存在：直接 matches
        if (!ObjectUtils.isEmpty(user.getPasswordHash())) {
            boolean ok = passwordEncoder.matches(rawPassword, user.getPasswordHash());
            if (!ok) return false;
            // 可选：参数升级（更强配置时自动重哈希）
            if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                this.updateById(user);
            }
            return true;
        }

        // 2) 旧列（你的库里是 MD5 值，如 e10adc...）
        if (!ObjectUtils.isEmpty(user.getLegacyPlainPassword())) {
            String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
            if (!md5.equalsIgnoreCase(user.getLegacyPlainPassword())) return false;

            // 登录即迁移
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setLegacyPlainPassword(null);
            this.updateById(user);
            return true;
        }

        // 既没有新哈希也没有旧口令，视为不通过
        return false;
    }
}