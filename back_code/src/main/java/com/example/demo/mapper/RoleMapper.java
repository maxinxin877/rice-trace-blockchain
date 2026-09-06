package com.example.demo.mapper;

import com.example.demo.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper {

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> findByUserId(@Param("userId") Long userId);

    /**
     * 根据角色名称查找角色
     */
    Role findByName(@Param("name") String name);

    /**
     * 查询所有角色
     */
    List<Role> findAll();

    /**
     * 新增角色
     */
    int insert(Role role);

    /**
     * 为用户分配角色
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
