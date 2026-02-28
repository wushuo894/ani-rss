export const PATH_SAVE_MODES = {
  LEGACY_LETTER_TITLE_SEASON: 'LegacyLetterTitleSeason',
  BANGUMI_SUBJECT_ID: 'BangumiSubjectId',
}

export const PATH_SAVE_MODE_META = {
  [PATH_SAVE_MODES.LEGACY_LETTER_TITLE_SEASON]: {
    label: '拼音首字母 / 标题 / Season',
    key: PATH_SAVE_MODES.LEGACY_LETTER_TITLE_SEASON,
    recommendedTemplates: {
      downloadPathTemplate: '/Users/wushuo/Movies/番剧/${letter}/${title}/Season ${season}',
      ovaDownloadPathTemplate: '/Users/wushuo/Movies/剧场版/${letter}/${title}',
    },
    description: '按番剧标题拼音首字母分组，再按标题和季数建立目录，是 ani-rss 默认路径结构，适合本地媒体库按标题浏览。',
    dataSources: [],
  },
  [PATH_SAVE_MODES.BANGUMI_SUBJECT_ID]: {
    label: 'Bangumi subject ID 目录',
    key: PATH_SAVE_MODES.BANGUMI_SUBJECT_ID,
    recommendedTemplates: {
      downloadPathTemplate: '/your/base/path/${bgmId}',
      ovaDownloadPathTemplate: '/your/base/path/movie/${bgmId}',
    },
    description: '使用 Bangumi 条目 ID（例如 https://bangumi.tv/subject/123456 中的 123456）作为目录名，方便与追番工具和外部数据库做一一对应。此模式下不会区分剧场版和TV版。',
    dataSources: [
      {
        name: 'bangumi-data',
        url: 'https://github.com/bangumi-data/bangumi-data',
        description: '以 JSON 维护的番剧与播放站点映射表，可用于自定义媒体路径或与 OpenList 等工具同步本地状态。',
      },
      {
        name: 'Bangumi API',
        url: 'https://github.com/bangumi/api',
        description: 'bgm.tv 官方 API 规格，可实时获取番剧的标准集数、类型（SP / OVA 等）和放送时间，用于精确命名和分类。',
      },
      {
        name: 'Anime-Offline-Database',
        url: 'https://github.com/manami-project/anime-offline-database',
        description: '跨 MyAnimeList、AniDB、Kitsu 等多个社区的 ID 离线映射表，可帮助在不同站点 ID 之间进行转换。',
      },
    ],
  },
}
