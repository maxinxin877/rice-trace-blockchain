/**
 * 文件 API 模块
 * 下载接口需登录鉴权，故用带 token 的 blob 拉取后生成 objectURL，供 <img> 预览。
 */
import { get, getBlob } from '../request'

/** 文件资源元数据 */
export interface FileResource {
  fileId: string
  fileName: string
  fileType: string
  fileSize: number
  fileUrl: string
  sha256?: string
  storageType?: string
  bizType?: string
  bizId?: string
  createTime?: string
}

export const fileApi = {
  /** 获取文件预览地址（objectURL） */
  async getPreviewUrl(fileId: string): Promise<string> {
    const blob = await getBlob(`/files/${fileId}/download`)
    return URL.createObjectURL(blob)
  },

  /** 获取文件元数据 */
  async getMetadata(fileId: string): Promise<FileResource> {
    const res = await get<FileResource>(`/files/${fileId}`)
    return res.data.data
  },
}
