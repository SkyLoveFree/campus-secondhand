package com.campus.secondhand.controller;

import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.dto.PageResult;
import com.campus.secondhand.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品查询接口：列表 / 搜索 / 排序 / 分页、详情（对应 docs/api-spec.md 3.5、3.6）。
 *
 * <p>两个接口均为公开白名单，游客可访问；写接口（发布/修改/删除）属 M4，不在本类实现。</p>
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * 商品列表（公开）：query 参数 page/size/keyword/sort，参数非法返回 400/10001。
     */
    @GetMapping
    public Result<PageResult<ItemVO>> list(@Valid @ModelAttribute ItemQueryRequest query) {
        return Result.success(itemService.listItems(query));
    }

    /**
     * 商品详情（公开）：id 非数字由全局异常处理为 400/10001，不存在返回 404/10005。
     */
    @GetMapping("/{id}")
    public Result<ItemVO> detail(@PathVariable("id") Long id) {
        return Result.success(itemService.getItem(id));
    }
}
