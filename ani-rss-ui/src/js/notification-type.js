export let notificationTypeList = [
    {
        name: 'TELEGRAM',
        label: 'TG通知'
    },
    {
        name: 'MAIL',
        label: '邮箱通知'
    },
    {
        name: 'SERVER_CHAN',
        label: 'Server酱'
    },
    {
        name: 'SYSTEM',
        label: '系统通知'
    },
    {
        name: 'WEB_HOOK',
        label: 'WebHook'
    },
    {
        name: 'EMBY_REFRESH',
        label: 'Emby媒体库刷新'
    },
    {
        name: 'SHELL',
        label: '执行外部程序'
    },
    {
        name: 'FILE_MOVE',
        label: '文件移动'
    }
]

export let getLabel = (name) => {
    return notificationTypeList.filter(item => item.name === name)[0].label;
}
