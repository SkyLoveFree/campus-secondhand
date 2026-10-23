package com.campus.secondhand.mapper;

import com.campus.secondhand.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品数据访问接口（SQL 见 resources/mapper/ItemMapper.xml）。
 *
 * <p>M2 只提供骨架，供 M3/M4 的 ItemService/ItemController 使用；本接口不包含业务判断与权限逻辑。</p>
 */
@Mapper
public interface ItemMapper {

    /**
     * 按主键查询商品（详情/改删前的存在性校验使用）。
     */
    Item selectById(@Param("id") Long id);

    /**
     * 分页查询商品列表。
     *
     * @param keyword 已转义的搜索关键词（null 或空串表示不筛选）
     * @param sort    排序枚举值 price_asc / price_desc，其他值按默认创建时间倒序
     * @param offset  偏移量 (page - 1) * size
     * @param size    每页条数（服务层保证 1-50）
     */
    List<Item> selectList(@Param("keyword") String keyword,
                          @Param("sort") String sort,
                          @Param("offset") int offset,
                          @Param("size") int size);

    /**
     * 统计商品总数，WHERE 条件与 {@link #selectList} 保持一致。
     */
    long countList(@Param("keyword") String keyword);

    /**
     * 新增商品，自增主键回填到 {@code item.id}；user_id 由 Service 从 Session 当前用户写入。
     */
    int insert(Item item);

    /**
     * 按主键全量更新商品（不改 user_id，updated_at 由数据库刷新）。
     *
     * @return 影响行数，0 表示商品不存在
     */
    int update(Item item);

    /**
     * 按主键物理删除商品。
     *
     * @return 影响行数，0 表示商品不存在
     */
    int deleteById(@Param("id") Long id);
}
