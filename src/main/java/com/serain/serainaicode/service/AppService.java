package com.serain.serainaicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.serain.serainaicode.model.dto.app.AppAddRequest;
import com.serain.serainaicode.model.dto.app.AppQueryRequest;
import com.serain.serainaicode.model.dto.app.AppUpdateMyRequest;
import com.serain.serainaicode.model.dto.app.AppUpdateRequest;
import com.serain.serainaicode.model.entity.App;
import com.serain.serainaicode.model.entity.User;
import com.serain.serainaicode.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/Leo-jc">serain</a>
 */
public interface AppService extends IService<App> {

    /**
     * 获取查询条件（管理员通用查询）
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 用户创建应用
     */
    long addApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 用户根据 id 更新自己的应用（仅支持修改名称）
     */
    boolean updateMyApp(AppUpdateMyRequest appUpdateMyRequest, User loginUser);

    /**
     * 用户根据 id 删除自己的应用
     */
    boolean deleteMyApp(long id, User loginUser);

    /**
     * 用户根据 id 查看自己的应用详情
     */
    AppVO getMyAppById(long id, User loginUser);

    /**
     * 用户分页查询自己的应用列表（支持根据名称查询，pageSize 已在调用处限 20）
     */
    Page<AppVO> listMyAppByPage(AppQueryRequest appQueryRequest, User loginUser);

    /**
     * 用户分页查询精选应用列表（支持根据名称查询，pageSize 已在调用处限 20）
     */
    Page<AppVO> listFeaturedAppByPage(AppQueryRequest appQueryRequest);

    /**
     * 管理员分页查询应用列表（支持根据除时间外的任何字段查询）
     */
    Page<AppVO> listAppByPage(AppQueryRequest appQueryRequest);

    /**
     * 管理员根据 id 更新任意应用
     */
    boolean updateAppByAdmin(AppUpdateRequest appUpdateRequest);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);
}

