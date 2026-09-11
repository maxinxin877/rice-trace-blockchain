package com.itheima.qukuailian.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.qukuailian.common.ResultCode;
import com.itheima.qukuailian.common.exception.BizException;
import com.itheima.qukuailian.entity.FileResource;
import com.itheima.qukuailian.mapper.FileResourceMapper;
import com.itheima.qukuailian.service.FileService;
import com.itheima.qukuailian.utils.HashUtils;
import com.itheima.qukuailian.utils.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    /** 允许的文件业务类型 */
    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of(
            "FIELD_PHOTO", "SEED_CERT", "INPUT_PROOF", "QUALITY_REPORT", "CERTIFICATION");

    private final FileResourceMapper fileResourceMapper;

    @Value("${app.file.upload-dir:./upload}")
    private String uploadDir;

    @Value("${app.file.url-prefix:/api/v1/files}")
    private String urlPrefix;

    @Override
    public FileResource upload(MultipartFile file, String bizType, String bizId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "上传文件不能为空");
        }
        // bizType 缺省为 FIELD_PHOTO，兼容前端未传该参数的情况
        String effectiveBizType = StringUtils.hasText(bizType) ? bizType : "FIELD_PHOTO";
        if (!ALLOWED_BIZ_TYPES.contains(effectiveBizType)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "不支持的文件业务类型: " + effectiveBizType);
        }

        // 1. 计算 SHA-256
        final String sha256;
        try {
            sha256 = HashUtils.sha256(file.getInputStream());
        } catch (IOException e) {
            log.error("计算文件哈希失败", e);
            throw new BizException(ResultCode.ERROR.getCode(), "文件读取失败");
        }

        // 2. 按哈希去重：重复上传直接返回既有记录（接口文档 §11.5）
        FileResource exist = fileResourceMapper.selectOne(
                new LambdaQueryWrapper<FileResource>().eq(FileResource::getSha256, sha256));
        if (exist != null) {
            log.info("文件重复上传，复用既有文件: fileId={}", exist.getFileId());
            return exist;
        }

        // 3. 落盘（uploadDir/{fileId}_{原始文件名}）
        String fileId = IdGen.generate("FILE");
        String safeName = sanitize(file.getOriginalFilename());
        File dir = uploadDirPath();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException(ResultCode.ERROR.getCode(), "文件目录创建失败: " + dir);
        }
        saveToDisk(file, new File(dir, fileId + "_" + safeName), fileId);

        // 4. 入库
        FileResource resource = new FileResource();
        resource.setFileId(fileId);
        resource.setFileName(file.getOriginalFilename());
        resource.setFileType(file.getContentType());
        resource.setFileSize(file.getSize());
        resource.setFileUrl(urlPrefix + "/" + fileId + "/download");
        resource.setSha256(sha256);
        resource.setStorageType("LOCAL");
        resource.setBizType(effectiveBizType);
        resource.setBizId(bizId == null ? "" : bizId);
        fileResourceMapper.insert(resource);
        log.info("文件上传成功: fileId={}, name={}, sha256={}", fileId, resource.getFileName(), sha256);
        return resource;
    }

    @Override
    public FileResource getFile(String fileId) {
        FileResource resource = fileResourceMapper.selectById(fileId);
        if (resource == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文件不存在: " + fileId);
        }
        return resource;
    }

    @Override
    public File loadStoreFile(String fileId) {
        FileResource resource = getFile(fileId);
        File file = new File(uploadDirPath(), fileId + "_" + sanitize(resource.getFileName()));
        if (!file.exists()) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "文件不存在: " + fileId);
        }
        return file;
    }

    /**
     * 上传目录（统一转成绝对路径）。
     * <p>注意：MultipartFile#transferTo 传入相对路径时，Servlet 容器会按自己的临时目录解析，
     * 导致 "文件保存失败"，因此这里必须先转绝对路径。</p>
     */
    private File uploadDirPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize().toFile();
    }

    /** 落盘：优先 transferTo，失败则退回到流拷贝（兼容各种容器实现） */
    private void saveToDisk(MultipartFile file, File target, String fileId) {
        try {
            file.transferTo(target);
            return;
        } catch (IOException | IllegalStateException e) {
            log.warn("transferTo 落盘失败，改用流拷贝: target={}", target, e);
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存文件失败: fileId={}", fileId, e);
            throw new BizException(ResultCode.ERROR.getCode(), "文件保存失败");
        }
    }

    /** 去除文件名中的路径分隔符等危险字符 */
    private String sanitize(String name) {
        if (!StringUtils.hasText(name)) {
            return "unnamed";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
