package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查找用户
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据ID查找用户
     */
    User findById(@Param("id") Long id);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 新增用户
     */
    int insert(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 删除用户
     */
    int deleteById(@Param("id") Long id);
}
