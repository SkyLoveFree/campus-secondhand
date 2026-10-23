package com.campus.secondhand.controller;

import com.campus.secondhand.common.BusinessException;
import com.campus.secondhand.common.Constants;
import com.campus.secondhand.common.ErrorCode;
import com.campus.secondhand.common.Result;
import com.campus.secondhand.dto.ItemQueryRequest;
import com.campus.secondhand.dto.ItemSaveRequest;
import com.campus.secondhand.dto.ItemVO;
import com.campus.secondhand.dto.PageResult;
import com.campus.secondhand.service.ItemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口：列表 / 搜索 / 排序 / 分页、详情（公开），发布 / 修改 / 删除（需登录）。
 *
 * <p>对应 docs/api-spec.md 3.5-3.10。Controller 只做路由、Bean Validation 与 Session 读取；
 * 权限与业务规则在 {@link ItemService}（归属只取 Session，请求中的 userId 一律忽略）。
 * 未登录访问写接口由 AuthInterceptor 返回 401/10002，不会进入本类。</p>
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

    /**
     * 发布商品（需登录）：成功 200 + code=0，返回完整商品信息；归属取 Session 当前用户。
     */
    @PostMapping
    public Result<ItemVO> create(@Valid @RequestBody ItemSaveRequest request, HttpServletRequest httpRequest) {
        Long currentUserId = currentUserId(httpRequest);
        return Result.success(itemService.createItem(request, currentUserId));
    }

    /**
     * 修改商品（需登录）：仅本人或管理员；不存在 404，无权限 403。
     */
    @PutMapping("/{id}")
    public Result<ItemVO> update(@PathVariable("id") Long id,
                                 @Valid @RequestBody ItemSaveRequest request,
                                 HttpServletRequest httpRequest) {
        Long currentUserId = currentUserId(httpRequest);
        String currentRole = currentRole(httpRequest);
        return Result.success(itemService.updateItem(id, request, currentUserId, currentRole));
    }

    /**
     * 删除商品（需登录）：仅本人或管理员；成功返回 data=null；不存在 404，无权限 403。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        Long currentUserId = currentUserId(httpRequest);
        String currentRole = currentRole(httpRequest);
        itemService.deleteItem(id, currentUserId, currentRole);
        return Result.success();
    }

    /**
     * 读取 Session 当前用户 ID；拦截器已保证登录，此处防御性兜底。
     */
    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = session == null ? null : (Long) session.getAttribute(Constants.SESSION_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private String currentRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(Constants.SESSION_ROLE);
    }
}
