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
export let setAni = (ani) => api.post('api/setAni', ani)

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

export let getBgmTitle = (ani) => api.post('api/getBgmTitle',ani)

export let testProxy = (url,config) => api.post(`api/testProxy?url=${url}`, config)

export let torrentsInfos = () => api.post('api/torrentsInfos')

export let verifyNo = (config) => api.post('api/verifyNo',config)

export let tryOut = (config) => api.post('api/tryOut',config)