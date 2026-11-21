import request from './request'

/**
 * 上传文件（作业图片/视频/PDF），返回 { url, name } 列表
 * @param {File[]} files
 */
export function uploadFiles(files) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request({
    url: '/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000  // 视频文件较大，上传超时放宽到10分钟
  })
}
