import dayjs from "dayjs";

export function formatSize(bytes) {
    if (!bytes) {
        return '-'
    }

    if (bytes < 1024) return `${bytes} B`

    const units = ['KiB', 'MiB', 'GiB', 'TiB', 'PiB']
    let v = bytes / 1024
    let i = 0
    while (v >= 1024 && i < units.length - 1) {
        v /= 1024
        i++
    }
    return `${v.toFixed(2)} ${units[i]}`
}

export let fromNow = (timestamp, template) => {
    if (template) {
        return dayjs(new Date(timestamp)).format(template);
    }

    const now = Date.now();
    const elapsedMs = now - timestamp;
    const elapsedMin = Math.floor(elapsedMs / (1000 * 60));

    if (elapsedMin < 1) {
        return "刚刚";
    }

    if (elapsedMin < 60) {
        return `${elapsedMin}分钟前`;
    }

    const hour = Math.floor(elapsedMs / (1000 * 60 * 60));

    if (hour < 24) {
        return `${hour}小时前`;
    }

    const day = Math.floor(elapsedMs / (1000 * 60 * 60 * 24));

    if (day >= 1 && day <= 3) {
        return `${day}天前`;
    }

    const target = new Date(timestamp);
    const nowDate = new Date();

    // 是否为当前年
    const isCurrentYear = target.getFullYear() === nowDate.getFullYear();

    template = isCurrentYear ? 'MM/DD HH:mm' : 'YYYY/MM/DD HH:mm';

    return dayjs(target).format(template);
}

export let formatTime = ts => {
    return dayjs(new Date(ts)).format('YYYY-MM-DD HH:mm:ss')
}

export let formatDate = ts => {
    return dayjs(new Date(ts)).format('YYYY-MM-DD')
};
