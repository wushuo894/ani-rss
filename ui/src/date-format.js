let formatTime = timestamp => {
    const now = Date.now();
    const elapsedMs = now - timestamp;
    const elapsedMin = Math.floor(elapsedMs / 60000);

    if (elapsedMin < 1) return "刚刚";
    if (elapsedMin < 60) return `${elapsedMin}分钟前`;

    const target = new Date(timestamp);
    const nowDate = new Date();

    // 是否为当前年
    const isCurrentYear = target.getFullYear() === nowDate.getFullYear();

    return target.toLocaleString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        month: isCurrentYear ? '2-digit' : undefined,
        day: isCurrentYear ? '2-digit' : undefined,
        year: isCurrentYear ? undefined : 'numeric',
        formatMatcher: 'best fit'
    }).replace(',', ' ');
}

export default formatTime;