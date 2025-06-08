let formatTime = timestamp => {
    const now = Date.now();
    const elapsedMs = now - timestamp;
    const elapsedMin = Math.floor(elapsedMs / 60000);

    if (elapsedMin < 1) return "刚刚";
    if (elapsedMin < 60) return `${elapsedMin}分钟前`;

    const target = new Date(timestamp);
    const nowDate = new Date();

    return target.toLocaleString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        month: target.getFullYear() === nowDate.getFullYear() ? 'numeric' : undefined,
        day: target.getFullYear() === nowDate.getFullYear() ? 'numeric' : undefined,
        year: target.getFullYear() === nowDate.getFullYear() ? undefined : 'numeric',
        formatMatcher: 'best fit'
    }).replace(',', ' ');
}

export default formatTime;