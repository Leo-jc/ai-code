package com.serain.serainaicode.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.serain.serainaicode.annotation.AuthCheck;
import com.serain.serainaicode.common.BaseResponse;
import com.serain.serainaicode.common.ResultUtils;
import com.serain.serainaicode.constant.UserConstant;
import com.serain.serainaicode.exception.ErrorCode;
import com.serain.serainaicode.exception.ThrowUtils;
import com.serain.serainaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.serain.serainaicode.model.entity.App;
import com.serain.serainaicode.model.entity.ChatHistory;
import com.serain.serainaicode.model.entity.User;
import com.serain.serainaicode.model.enums.UserRoleEnum;
import com.serain.serainaicode.service.AppService;
import com.serain.serainaicode.service.ChatHistoryService;
import com.serain.serainaicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author <a href="https://github.com/Leo-jc">serain</a>
 */
@RestController
@RequestMapping("/chat/history")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 【用户】分页查询某个应用的对话历史（仅应用创建者或管理员可见）
     * 默认按创建时间降序，每次加载最新 10 条，前端通过翻页向前加载更多。
     */
    @PostMapping("/app/list/page")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@RequestBody ChatHistoryQueryRequest queryRequest,
                                                               HttpServletRequest request) {
        ThrowUtils.throwIf(queryRequest == null || queryRequest.getAppId() == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long appId = queryRequest.getAppId();
        // 校验应用是否存在
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 仅应用创建者或管理员可查看
        boolean isAppOwner = app.getUserId() != null && app.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!isAppOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR);
        // 限制每页最多 20 条，默认 10 条
        int pageSize = queryRequest.getPageSize();
        if (pageSize <= 0) {
            queryRequest.setPageSize(10);
        } else if (pageSize > 20) {
            queryRequest.setPageSize(20);
        }
        // 只查当前应用的对话
        queryRequest.setUserId(null);
        Page<ChatHistory> pageResult = chatHistoryService.listByApp(queryRequest);
        return ResultUtils.success(pageResult);
    }

    /**
     * 【管理员】分页查看所有应用的对话历史，按时间倒序，便于内容监管
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistory(@RequestBody ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        // 管理员场景可不限制 pageSize，由前端自行控制
        Page<ChatHistory> pageResult = chatHistoryService.listAll(queryRequest);
        return ResultUtils.success(pageResult);
    }

    /**
     * 【用户】根据应用 id 获取最新 10 条对话历史（进入聊天页首次加载）
     */
    @GetMapping("/app/latest")
    public BaseResponse<Page<ChatHistory>> listLatestAppChatHistory(Long appId, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR);
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setPageNum(1);
        queryRequest.setPageSize(10);
        return listAppChatHistory(queryRequest, request);
    }

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }


}

