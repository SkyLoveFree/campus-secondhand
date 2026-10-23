package com.campus.secondhand.mapper;

import com.campus.secondhand.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问接口（SQL 见 resources/mapper/UserMapper.xml）。
 *
 * <p>只负责 SQL 与结果映射，不做业务判断；所有参数均使用 {@code #{}} 参数化。</p>
 */
@Mapper
public interface UserMapper {

    /**
     * 按用户名查询用户（登录使用）。
     *
     * @param username 已 trim 的用户名
     * @return 用户实体，不存在返回 null
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 按主键查询用户（预留：后续接口需要按 ID 取用户时使用）。
     */
    User selectById(@Param("id") Long id);

    /**
     * 统计用户名数量（注册查重；数据库排序规则不区分大小写）。
     */
    long countByUsername(@Param("username") String username);

    /**
     * 新增用户，自增主键回填到 {@code user.id}。
     *
     * @return 影响行数
     */
    int insert(User user);
}
