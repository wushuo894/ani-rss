import api from "@/js/api.js";

/**
 * 获取设置
 * @returns {Promise<unknown>}
 */
export let config = () => api.post('api/config')

/**
 * 修改设置
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
 * @param ani
 * @returns {Promise<unknown>}
 */
export let addAni = (ani) => api.post('api/addAni', ani)

/**
 * 修改订阅
 * @returns {Promise<unknown>}
 */
export let setAni = (move, ani) => api.post(`api/setAni?move=${move}`, ani)

/**
 * 删除订阅
 * @param deleteFiles
 * @param ids
 * @returns {Promise<unknown>}
 */
export let deleteAni = (deleteFiles, ids) => api.post(`api/deleteAni?deleteFiles=${deleteFiles}`, ids)

/**
 * 关于
 * @returns {Promise<unknown>}
 */
export let about = () => api.post('api/about')

export let update = () => api.post('api/update')

export let mikan = (text, body) => api.post(`api/mikan?text=${text}`, body)

export let mikanGroup = (url) => api.post(`api/mikanGroup?url=${url}`)

export let refreshAll = () => api.post('api/refreshAll')

export let refreshAni = (ani) => api.post('api/refreshAni', ani)

export let rssToAni = (ani) => api.post('api/rssToAni', ani)

export let previewAni = (ani) => api.post('api/previewAni', ani)

export let logs = () => api.post('api/logs')

export let clearLogs = () => api.post('api/clearLogs')

export let getThemoviedbName = (ani) => api.post('api/getThemoviedbName', ani)

export let getThemoviedbGroup = (ani) => api.post('api/getThemoviedbGroup', ani)

export let testNotification = (notificationConfig) => api.post('api/testNotification', notificationConfig)

export let newNotification = () => api.post('api/newNotification')

export let getBgmTitle = (ani) => api.post('api/getBgmTitle', ani)

export let searchBgm = (name) => api.post(`api/searchBgm?name=${name}`)

export let testProxy = (url, config) => api.post(`api/testProxy?url=${url}`, config)

export let torrentsInfos = () => api.post('api/torrentsInfos')

export let verifyNo = (config) => api.post('api/verifyNo', config)

export let tryOut = (config) => api.post('api/tryOut', config)

export let updateTotalEpisodeNumber = (force, ids) => api.post(`api/updateTotalEpisodeNumber?force=${force}`, ids)

export let batchEnable = (value, ids) => api.post(`api/batchEnable?value=${value}`, ids)

export let importAni = (data) => api.post('api/importAni', data)

export let stop = (status) => api.post('api/stop', status)

export let cover = (ani) => api.post('api/cover', ani)

export let rate = (ani) => api.post('api/rate', ani)

export let downloadPath = (ani) => api.post('api/downloadPath', ani)

export let scrape = (force, ani) => api.post(`api/scrape?force=${force}`, ani)

export let meBgm = (ani) => api.post('api/meBgm', ani)

export let trackersUpdate = (config) => api.post('api/trackersUpdate', config)

export let getViews = (config) => api.post('api/getViews', config)

export let clearCache = () => api.post('api/clearCache')

export let downloadLoginTest = (config) => api.post('api/downloadLoginTest', config)

export let getUpdates = (config) => api.post('api/updates', config)

export let login = (v) => api.post('api/login', v)

export let test = () => fetch('api/test', {method: 'post'})

export let playList = (it) => api.post('api/playList', it)

export let getSubtitles = (file) => api.post('api/getSubtitles', {file})

export let startCollection = (info) => api.post('api/startCollection', info)

export let previewCollection = (info) => api.post('api/previewCollection', info)

export let getCollectionSubgroup = (info) => api.post('api/getCollectionSubgroup', info)

export let getAniBySubjectId = (id) => api.post(`api/getAniBySubjectId?id=${id}`)