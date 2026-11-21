import request from './request'

// ============ 敦煌舞数字展馆（Java代理Python评分服务，只读） ============

/** 8大敦煌舞动作类别元数据 + 标志性关键姿态特征 */
export function getDunhuangCategories() {
  return request({
    url: '/dunhuang/categories',
    method: 'get',
    timeout: 30000
  })
}

/** 指定类别的参考骨架序列（150帧×33关键点，payload较大，单独放宽超时） */
export function getDunhuangReference(category) {
  return request({
    url: `/dunhuang/reference/${category}`,
    method: 'get',
    timeout: 60000
  })
}

/**
 * 关键姿态挑战：上传一张全身照（FormData，字段名file），
 * 返回四大敦煌标志姿态匹配评分与归一化关键点。首次调用模型冷启动，超时放宽到120s。
 */
export function challengePose(formData) {
  return request({
    url: '/dunhuang/pose-challenge',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
