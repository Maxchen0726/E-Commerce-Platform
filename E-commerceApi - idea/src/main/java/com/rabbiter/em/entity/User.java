package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;

@TableName("sys_user")
public class User implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String address;

    @TableField("avatar_url")
    private String avatarUrl;

    private String role;

    /** —— 安全相关 —— */

    /** 新字段：只存 KDF 后的口令哈希（argon2id/bcrypt 等） */
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 旧字段：迁移期临时保留，用于登录一次即重哈希回写；完成后请删除表列与本字段。
     * 注意：加了 @JsonIgnore，避免被序列化到接口响应。
     */
    @Deprecated
    @JsonIgnore
    @TableField("password")
    private String legacyPlainPassword;

    /** 修改密码用的临时入参，不落库 */
    @TableField(exist = false)
    @JsonIgnore
    private String newPassword;

    /* ======== getter / setter 省略，可保留你原有生成的 ======== */

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getLegacyPlainPassword() { return legacyPlainPassword; }
    public void setLegacyPlainPassword(String legacyPlainPassword) { this.legacyPlainPassword = legacyPlainPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    /** 千万别在 toString 打印口令字段 */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

