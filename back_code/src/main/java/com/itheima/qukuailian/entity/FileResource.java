package com.itheima.qukuailian.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件资源（接口文档 v1.1 §3.14 FileResource）
 */
@Data
@TableName("file_resource")
public class FileResource {

    @TableId(type = IdType.INPUT)
    private String fileId;

    /** 原始文件名 */
    private String fileName;

    /** MIME 类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件访问地址 */
    private String fileUrl;

    /** 文件 SHA-256（sha256: 前缀） */
    private String sha256;

    /** 存储类型：LOCAL/MINIO/IPFS */
    private String storageType;

    /** 文件业务类型：FIELD_PHOTO/SEED_CERT/INPUT_PROOF/QUALITY_REPORT/CERTIFICATION */
    private String bizType;

    /** 关联业务 ID */
    private String bizId;

    private LocalDateTime createTime;
}
