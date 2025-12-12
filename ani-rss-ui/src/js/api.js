import {ElMessage} from "element-plus";
import {authorization} from "@/js/global.js";

let post = async (url, body) => {
    return await fetch_(url, 'POST', body);
}

let get = async (url) => {
    return await fetch_(url, 'GET', '');
}

let del = async (url, body) => {
    return await fetch_(url, 'DELETE', body);
}

let put = async (url, body) => {
    return await fetch_(url, 'PUT', body);
}

let fetch_ = async (url, method, body) => {
    let headers = {}
    if (authorization.value) {
        headers['Authorization'] = authorization.value
    }
    return await fetch(url, {
        'method': method,
        'body': body ? JSON.stringify(body) : null,
        'headers': headers
    })
        .then(res => res.json())
        .then(res => {
            let {code, message, t} = res

            if (!checkTimestampRange(t, true)) {
                console.warn('与服务端时差超过30分钟')
            }

            if (code >= 200 && code < 300) {
                return res
            }

            ElMessage.error(message)
            if (code === 403) {
                authorization.value = ''
                setTimeout(() => {
                    location.reload()
                }, 1000)
            }
            return new Promise((resolve, reject) => {
                reject(new Error(message));
            });
        })
}

export default {post, get, del, put}

let checkTimestampRange = (timestamp, isMilli = true) => {
    const ts = Math.floor(Number(timestamp));
    if (Number.isNaN(ts)) return false;
    const targetTime = isMilli ? ts : ts * 1000;
    const now = Date.now();
    // 30 分钟
    const range = 30 * 60 * 1000;
    const diff = Math.abs(now - targetTime);
    return diff <= range;
}
