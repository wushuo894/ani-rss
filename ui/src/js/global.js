import {useColorMode, useLocalStorage} from "@vueuse/core";
import {ref, watch} from "vue";

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

watch(isNotMobile, () => {
    if (isNotMobile.value) {
        elIconClass.value = 'el-icon--left'
    }
})

export {authorization, store, maxContentWidth, color, colorChange, isNotMobile, elIconClass};
