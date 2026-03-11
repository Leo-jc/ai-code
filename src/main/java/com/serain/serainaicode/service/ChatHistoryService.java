package com.serain.serainaicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.serain.serainaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.serain.serainaicode.model.entity.ChatHistory;
import com.serain.serainaicode.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/Leo-jc">serain</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史
     *
     * @param appId       应用 id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户 id
     * @return 是否成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用 id 删除对话历史
     *
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 分页查询某 APP 的对话记录
     *
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载对话历史到内存
     *
     * @param appId
     * @param chatMemory
     * @param maxCount 最多加载多少条
     * @return 加载成功的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 保存用户消息
     */
    void saveUserMessage(Long appId, Long userId, String message);

    /**
     * 保存 AI 消息
     */
    void saveAiMessage(Long appId, Long userId, String message);

    /**
     * 保存错误消息（AI 回复失败时记录）
     */
    void saveErrorMessage(Long appId, Long userId, String errorMessage);

    /**
     * 分页查询某个应用的对话历史（仅指定 appId）
     */
    Page<ChatHistory> listByApp(ChatHistoryQueryRequest queryRequest);

    /**
     * 管理员分页查看所有应用的对话历史（按时间降序）
     */
    Page<ChatHistory> listAll(ChatHistoryQueryRequest queryRequest);
}
