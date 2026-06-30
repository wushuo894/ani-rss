import {useColorMode, useDark, useDebounceFn, useEventListener, useLocalStorage} from "@vueuse/core";
import {ref} from "vue";

/**
 * 保存登录信息
 */
let rememberThePassword = useLocalStorage('rememberThePassword', {
    remember: false,
    username: '',
    password: ''
})

/**
 * 令牌
 */
const authorization = useLocalStorage('authorization', '')

/**
 * 主题管理
 */
const {store} = useColorMode()

/**
 * 最大内容宽度
 */
const maxContentWidth = useLocalStorage('max-content-width', 1600);

/**
 * 显示评分
 */
const showScore = useLocalStorage('show-score', true)

/**
 * 按星期展示
 */
const showWeek = useLocalStorage("show-week", true)

/**
 * 显示视频列表
 */
const showPlaylist = useLocalStorage('show-playlist', true)

/**
 * 显示更新时间
 */
const showLastDownloadTime = useLocalStorage("show-last-download-time", true);

/**
 * 强调色
 */
const color = useLocalStorage('--el-color-primary', '#409eff')

/**
 * 改动强调色
 */
const colorChange = (v) => {
    const el = document.documentElement
    el.style.setProperty('--el-color-primary', v)
}

/**
 * 是否非移动设备
 */
const isNotMobile = ref(false)

/**
 * el-icon的class
 *
 * 自动适应移动布局
 */
const elIconClass = ref('')

/**
 * 主题初始化
 */
const initTheme = () => {
    /**
     * 夜间模式
     */
    useDark({
        onChanged: dark => {
            // 自动根据夜间模式修改沉浸式状态栏
            const meta = document.getElementById('themeColorMeta');
            meta.content = dark ? '#000000' : '#ffffff';
        }
    })

    // 修改强调色
    colorChange(color.value)
}

/**
 * 布局初始化
 */
const initLayout = () => {
    let app = document.querySelector('#app');

    // 设置最大布局宽度
    maxContentWidth.value = Math.max(maxContentWidth.value, 1200)

    app
        .style.maxWidth = `${maxContentWidth.value}px`

    const el = document.documentElement
    el.style.setProperty('--max-content-width', `${maxContentWidth.value}px`)

    // 是否非移动设备
    isNotMobile.value = app.offsetWidth > 800

    if (isNotMobile.value) {
        elIconClass.value = 'el-icon--left'
    } else {
        // 用以控制图标与文字的间距 当为移动设备时便不需要间距了
        elIconClass.value = ''
    }
}

/**
 * 初始化
 */
const init = () => {
    initTheme()
    initLayout()
}

/**
 * 当页面大小变化时重新计算一下布局
 * 对方法做节流处理
 */
useEventListener(window, 'resize', useDebounceFn(initLayout, 500))

const base64Encode = s => {
    const encoder = new TextEncoder();
    const data = encoder.encode(s);
    return window.btoa(String.fromCharCode(...data));
}

const toApiUrl = (path, params) => {
    const searchParams = new URLSearchParams(params);
    const query = searchParams.toString();
    return query ? `${path}?${query}` : path;
}

const proxyImage = imgUrl => {
    return toApiUrl('api/proxyImage', {
        imgUrl: base64Encode(imgUrl),
        s: authorization.value
    })
}

const toApiFile = filename => {
    return toApiUrl('api/file', {
        filename: base64Encode(filename),
        s: authorization.value
    })
}

export {
    rememberThePassword,
    authorization,
    store,
    maxContentWidth,
    showScore,
    showWeek,
    showPlaylist,
    showLastDownloadTime,
    color,
    colorChange,
    isNotMobile,
    elIconClass,
    init,
    initTheme,
    initLayout,
    base64Encode,
    toApiUrl,
    proxyImage,
    toApiFile
};
