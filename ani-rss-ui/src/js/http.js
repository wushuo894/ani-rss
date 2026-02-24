import api from "@/js/api.js";

/**
 * 获取设置
 * @returns {Promise<unknown>}
 */
export let config = () => api.post('api/config')

/**
 * 修改设置
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let setConfig = (config) => api.post('api/setConfig', config);

/**
 * 订阅列表
 * @returns {Promise<unknown>}
 */
export let listAni = () => api.post('api/listAni')

/**
 * 添加订阅
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let addAni = (ani) => api.post('api/addAni', ani)

/**
 * 修改订阅
 * @param move 自动移动本地文件
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let setAni = (move, ani) => api.post(`api/setAni?move=${move}`, ani)

/**
 * 删除订阅
 * @param deleteFiles 同时删除本地文件
 * @param ids ids
 * @returns {Promise<unknown>}
 */
export let deleteAni = (deleteFiles, ids) => api.post(`api/deleteAni?deleteFiles=${deleteFiles}`, ids)

/**
 * 关于
 * @returns {Promise<unknown>}
 */
export let about = () => api.post('api/about')

/**
 * 更新
 * @returns {Promise<unknown>}
 */
export let update = () => api.post('api/update')

/**
 * 获取Mikan番剧列表
 * @param text 关键词
 * @param season 季度
 * @returns {Promise<unknown>}
 */
export let mikan = (text, season) => api.post(`api/mikan?text=${text}`, season)

/**
 * 获取Mikan番剧的字幕组列表
 * @param url 番剧url
 * @returns {Promise<unknown>}
 */
export let mikanGroup = (url) => api.post(`api/mikanGroup?url=${url}`)

/**
 * 刷新全部订阅
 * @returns {Promise<unknown>}
 */
export let refreshAll = () => api.post('api/refreshAll')

/**
 * 刷新订阅
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let refreshAni = (ani) => api.post('api/refreshAni', ani)

/**
 * 将RSS转换为订阅
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let rssToAni = (ani) => api.post('api/rssToAni', ani)

/**
 * 预览订阅
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let previewAni = (ani) => api.post('api/previewAni', ani)

/**
 * 日志
 * @returns {Promise<unknown>}
 */
export let logs = () => api.post('api/logs')

/**
 * 清理日志
 * @returns {Promise<unknown>}
 */
export let clearLogs = () => api.post('api/clearLogs')

/**
 * 获取TMDB标题
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let getThemoviedbName = (ani) => api.post('api/getThemoviedbName', ani)

/**
 * 获取TMDB剧集组
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let getThemoviedbGroup = (ani) => api.post('api/getThemoviedbGroup', ani)

/**
 * 测试通知
 * @param notificationConfig 通知设置
 * @returns {Promise<unknown>}
 */
export let testNotification = (notificationConfig) => api.post('api/testNotification', notificationConfig)

/**
 * 新的通知
 * @returns {Promise<unknown>}
 */
export let newNotification = () => api.post('api/newNotification')

/**
 * 获取BGM标题
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let getBgmTitle = (ani) => api.post('api/getBgmTitle', ani)


/**
 * 搜索BGM条目
 * @param name 关键词
 * @returns {Promise<unknown>}
 */
export let searchBgm = (name) => api.post(`api/searchBgm?name=${name}`)

/**
 * 代理测试
 * @param url url
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let testProxy = (url, config) => api.post(`api/testProxy?url=${url}`, config)

/**
 * 下载列表
 * @returns {Promise<unknown>}
 */
export let torrentsInfos = () => api.post('api/torrentsInfos')

/**
 * 订单号校验
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let verifyNo = (config) => api.post('api/verifyNo', config)

/**
 * 试用
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let tryOut = (config) => api.post('api/tryOut', config)

/**
 * 更新总集数
 * @param force 强制
 * @param ids ids
 * @returns {Promise<unknown>}
 */
export let updateTotalEpisodeNumber = (force, ids) => api.post(`api/updateTotalEpisodeNumber?force=${force}`, ids)

/**
 * 批量 启用/禁用 订阅
 * @param value true/false
 * @param ids ids
 * @returns {Promise<unknown>}
 */
export let batchEnable = (value, ids) => api.post(`api/batchEnable?value=${value}`, ids)

/**
 * 导入订阅
 * @param anis 订阅列表
 * @returns {Promise<unknown>}
 */
export let importAni = (anis) => api.post('api/importAni', anis)

/**
 * 停止服务
 * @param status 0:重启 2:关闭
 * @returns {Promise<unknown>}
 */
export let stop = (status) => api.post(`api/stop?status=${status}`)

/**
 * 刷新封面
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let refreshCover = (ani) => api.post('api/refreshCover', ani)

/**
 * 评分
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let rate = (ani) => api.post('api/rate', ani)

/**
 * 获取下载位置
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let downloadPath = (ani) => api.post('api/downloadPath', ani)

/**
 * 刮削
 * @param force 强制 true/false
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let scrape = (force, ani) => api.post(`api/scrape?force=${force}`, ani)

/**
 * 获取当前BGM账号信息
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let meBgm = (ani) => api.post('api/meBgm', ani)

/**
 * 更新trackers
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let trackersUpdate = (config) => api.post('api/trackersUpdate', config)

/**
 * 获取Emby媒体库
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let getEmbyViews = (config) => api.post('api/getEmbyViews', config)

/**
 * 清理缓存
 * @returns {Promise<unknown>}
 */
export let clearCache = () => api.post('api/clearCache')

/**
 * 下载器测试
 * @param config 设置
 * @returns {Promise<unknown>}
 */
export let downloadLoginTest = (config) => api.post('api/downloadLoginTest', config)

/**
 * 获取TG最近消息
 * @param notificationConfig 通知配置
 * @returns {Promise<unknown>}
 */
export let getTgUpdates = (notificationConfig) => api.post('api/getTgUpdates', notificationConfig)

/**
 * 登录
 * @param v
 * @returns {Promise<unknown>}
 */
export let login = (v) => api.post('api/login', v)

/**
 * 测试IP白名单
 * @returns {Promise<Response>}
 */
export let testIpWhitelist = () => fetch('api/testIpWhitelist', {method: 'post'}).then(res => res.json())

/**
 * 获取视频列表
 * @param ani 订阅
 * @returns {Promise<unknown>}
 */
export let playList = (ani) => api.post('api/playList', ani)

/**
 * 获取内封字幕
 * @param file 视频文件路径
 * @returns {Promise<unknown>}
 */
export let getSubtitles = (file) => api.post('api/getSubtitles', {file})

/**
 * 开始下载合集
 * @param info 合集
 * @returns {Promise<unknown>}
 */
export let startCollection = (info) => api.post('api/startCollection', info)

/**
 * 预览合集
 * @param info 合集
 * @returns {Promise<unknown>}
 */
export let previewCollection = (info) => api.post('api/previewCollection', info)

/**
 * 获取合集字幕组
 * @param info 合集
 * @returns {Promise<unknown>}
 */
export let getCollectionSubgroup = (info) => api.post('api/getCollectionSubgroup', info)

/**
 * 将指定id的BGM番剧转换为订阅
 * @param id BGM的ID
 * @returns {Promise<unknown>}
 */
export let getAniBySubjectId = (id) => api.post(`api/getAniBySubjectId?id=${id}`)

/**
 * 删除缓存的种子
 * @param id 订阅id
 * @param hash 种子hash
 * @returns {Promise<unknown>}
 */
export let deleteTorrent = (id, hash) => api.post(`api/deleteTorrent?id=${id}&hash=${hash}`)