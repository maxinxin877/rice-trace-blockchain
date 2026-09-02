package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（MD5(salt + 明文密码)） */
    private String password;

    /** 加盐 */
    private String salt;

    private String nickname;

    private String phone;

    /** 角色：ADMIN / FARMER / PROCESSOR / LOGISTICS / CONSUMER */
    private String role;

    /** 状态：1 正常，0 禁用 */
    private Integer status;

    /** 逻辑删除：0 未删除，1 已删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
