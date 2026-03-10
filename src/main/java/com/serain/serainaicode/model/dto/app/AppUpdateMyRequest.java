package com.serain.serainaicode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新自己应用请求
 */
@Data
public class AppUpdateMyRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long id;

    /**
     * 应用名称（目前仅支持修改名称）
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}

