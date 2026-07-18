/**
 * Mock 图片缓存
 * fileId -> base64 data URL
 * 用于在页面间共享上传的图片数据
 */
export const mockImageCache = new Map<string, string>()
