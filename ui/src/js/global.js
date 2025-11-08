import {useColorMode, useLocalStorage} from "@vueuse/core";
import {ref, watch} from "vue";

const authorization = useLocalStorage('authorization', '')

const {store} = useColorMode()

let maxContentWidth = useLocalStorage('max-content-width', 1600);

let color = useLocalStorage('--el-color-primary', '#409eff')

let colorChange = (v) => {
    const el = document.documentElement
    el.style.setProperty('--el-color-primary', v)
}

let isNotMobile = ref(false)
let elIconClass = ref('')

watch(isNotMobile, () => {
    if (isNotMobile.value) {
        elIconClass.value = 'el-icon--left'
    }
})

export {authorization, store, maxContentWidth, color, colorChange, isNotMobile,elIconClass};
