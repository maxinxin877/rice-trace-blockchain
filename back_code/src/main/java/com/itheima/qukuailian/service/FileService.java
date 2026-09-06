package com.itheima.qukuailian.service;

import com.itheima.qukuailian.entity.FileResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件服务（接口文档 v1.1 §8 文件接口）
 */
public interface FileService {

    /**
     * 上传文件：计算 SHA-256，按哈希去重（重复上传直接返回既有记录）
     */
    FileResource upload(MultipartFile file, String bizType, String bizId);

    /**
     * 查询文件元数据，不存在抛出 NOT_FOUND
     */
    FileResource getFile(String fileId);

    /**
     * 定位磁盘文件（本地存储），供下载使用
     */
    File loadStoreFile(String fileId);
}
