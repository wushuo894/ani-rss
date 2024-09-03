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
    return await fetch(url, {
        'method': method,
        'body': body ? JSON.stringify(body) : null,
        'headers': {
            'Authorization': window.authorization
        }
    })
        .then(res => res.json())
        .then(res => {
            if (res.code === 403) {
                localStorage.removeItem("authorization")
                location.reload()
            }
            if (res.code >= 200 && res.code < 300){
                return res
            }
            ElMessage.error(res.message)
            return new Promise((resolve, reject) => {
                reject(new Error(res.message));
            });
        })
}

export default {post, get, del, put}