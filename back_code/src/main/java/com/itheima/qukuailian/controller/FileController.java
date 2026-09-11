package com.itheima.qukuailian.controller;

import com.itheima.qukuailian.common.Result;
import com.itheima.qukuailian.entity.FileResource;
import com.itheima.qukuailian.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 通用文件接口（接口文档 v1.1 §8，需登录）
 * <ul>
 *   <li>POST /api/v1/files —— 上传（multipart/form-data: file + bizType [+ bizId]）</li>
 *   <li>POST /api/v1/files/upload —— 同上（兼容前端 action 写法）</li>
 *   <li>GET  /api/v1/files/{fileId} —— 元数据</li>
 *   <li>GET  /api/v1/files/{fileId}/download —— 下载</li>
 * </ul>
 * <p>bizType 缺省为 FIELD_PHOTO，避免前端未传时报 400。</p>
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /** 上传文件 */
    @PostMapping
    public Result<FileResource> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "bizType", required = false) String bizType,
                                       @RequestParam(value = "bizId", required = false) String bizId) {
        return Result.success(fileService.upload(file, bizType, bizId));
    }

    /** 上传文件（兼容 /files/upload 路径） */
    @PostMapping("/upload")
    public Result<FileResource> uploadAlias(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "bizType", required = false) String bizType,
                                            @RequestParam(value = "bizId", required = false) String bizId) {
        return Result.success(fileService.upload(file, bizType, bizId));
    }

    /** 文件元数据 */
    @GetMapping("/{fileId}")
    public Result<FileResource> metadata(@PathVariable String fileId) {
        return Result.success(fileService.getFile(fileId));
    }

    /** 下载文件 */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        File file = fileService.loadStoreFile(fileId);
        FileResource meta = fileService.getFile(fileId);
        String encodedName = URLEncoder.encode(meta.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.parseMediaType(
                        meta.getFileType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.getFileType()))
                .body(new FileSystemResource(file));
    }
}
