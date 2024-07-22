import {ElMessage} from "element-plus";

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
    let authorization = window.authorization
    if (authorization) {
        headers['Authorization'] = authorization
    }
    return await fetch(url, {
        'method': method,
        'body': body ? JSON.stringify(body) : null,
        'headers': headers
    })
        .then(res => res.json())
        .then(res => {
            if (res.code >= 200 && res.code < 300) {
                return res
            }

            ElMessage.error(res.message)
            if (res.code === 403) {
                localStorage.removeItem("authorization")
                setTimeout(() => {
                    location.reload()
                }, 1000)
            }
            return new Promise((resolve, reject) => {
                reject(new Error(res.message));
            });
        })
}

export default {post, get, del, put}