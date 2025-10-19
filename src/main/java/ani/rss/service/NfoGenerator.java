package ani.rss.service;

import ani.rss.entity.tmdb.*;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * nfo生成
 */
@Slf4j
public class NfoGenerator {

    /**
     * SxxExx.nfo
     *
     * @param tmdbEpisode tmdb集
     * @param outputPath  输出位置
     * @throws Exception
     */
    public static void generateEpisodeNfo(TmdbEpisode tmdbEpisode, String outputPath) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("episodedetails");
        doc.appendChild(rootElement);

        addElement(doc, rootElement, "title", tmdbEpisode.getName());
        addElement(doc, rootElement, "plot", tmdbEpisode.getOverview());
        addElement(doc, rootElement, "rating", tmdbEpisode.getVoteAverage());
        addElement(doc, rootElement, "year", DateUtil.year(tmdbEpisode.getAirDate()));
        addElement(doc, rootElement, "aired", DateUtil.format(tmdbEpisode.getAirDate(), DatePattern.NORM_DATE_PATTERN));
        addElement(doc, rootElement, "episode", tmdbEpisode.getEpisodeNumber());
        addElement(doc, rootElement, "season", tmdbEpisode.getSeasonNumber());
        addElement(doc, rootElement, "runtime", tmdbEpisode.getRuntime());

        saveXmlDocument(doc, outputPath);
    }

    /**
     * season.nfo
     *
     * @param tmdbSeason
     * @param outputPath
     * @throws Exception
     */
    public static void generateSeasonNfo(TmdbSeason tmdbSeason, String outputPath) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("season");
        doc.appendChild(rootElement);

        addElement(doc, rootElement, "title", tmdbSeason.getName());
        addElement(doc, rootElement, "plot", tmdbSeason.getOverview());
        addElement(doc, rootElement, "outline", tmdbSeason.getOverview());
        addElement(doc, rootElement, "seasonnumber", tmdbSeason.getSeasonNumber());

        Date airDate = tmdbSeason.getAirDate();

        if (Objects.nonNull(airDate)) {
            addElement(doc, rootElement, "year", DateUtil.year(airDate));
            addElement(doc, rootElement, "releasedate", DateUtil.format(airDate, DatePattern.NORM_DATE_PATTERN));
        }

        saveXmlDocument(doc, outputPath);
    }

    /**
     * tvshown.nfo
     *
     * @param tmdb       tmdb
     * @param outputPath 输出位置
     * @throws Exception
     */
    public static void generateTvShowNfo(Tmdb tmdb, String outputPath) throws Exception {
        generateNfo(tmdb, outputPath, "tvshow");
    }

    /**
     * 电影nfo
     *
     * @param tmdb       tmdb
     * @param outputPath 输出位置
     * @throws Exception
     */
    public static void generateMovieNfo(Tmdb tmdb, String outputPath) throws Exception {
        generateNfo(tmdb, outputPath, "movie");
    }

    /**
     * nfo
     *
     * @param tmdb       tmdb
     * @param outputPath 输出位置
     * @param rootTag    根标签
     * @throws Exception
     */
    public static void generateNfo(Tmdb tmdb, String outputPath, String rootTag) throws Exception {
        // 创建 XML 文档
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // 创建根元素
        Element rootElement = doc.createElement(rootTag);
        doc.appendChild(rootElement);

        // 添加基本信息
        addElement(doc, rootElement, "tmdbid", tmdb.getId());
        addElement(doc, rootElement, "title", tmdb.getName());
        addElement(doc, rootElement, "originaltitle", tmdb.getOriginalName());
        addElement(doc, rootElement, "year", DateUtil.year(tmdb.getDate()));
        addElement(doc, rootElement, "plot", tmdb.getOverview());
        addElement(doc, rootElement, "rating", tmdb.getVoteAverage());
        addElement(doc, rootElement, "votes", tmdb.getVoteCount());
        addElement(doc, rootElement, "releasedate", DateUtil.format(tmdb.getDate(), DatePattern.NORM_DATE_PATTERN));
        addElement(doc, rootElement, "tagline", tmdb.getTagline());

        Integer runtime = tmdb.getRuntime();
        // 时长
        addElement(doc, rootElement, "runtime", runtime);

        // 剧集组编号
        String tmdbGroupId = tmdb.getTmdbGroupId();
        addElement(doc, rootElement, "tmdbegid", tmdbGroupId);

        // 类型
        List<TmdbGenres> genres = tmdb.getGenres();
        for (TmdbGenres genre : genres) {
            addElement(doc, rootElement, "genre", genre.getName());
        }

        // 演职人员
        List<TmdbCreditsCast> cast = tmdb.getCredits().getCast();
        for (TmdbCreditsCast item : cast) {
            Element actor = doc.createElement("actor");
            rootElement.appendChild(actor);

            addElement(doc, actor, "name", item.getName());
            addElement(doc, actor, "role", item.getCharacter());
            addElement(doc, actor, "type", "Actor");
            addElement(doc, actor, "tmdbid", item.getId());
        }

        //  预告片
        List<TmdbVideo> videos = tmdb.getVideos();
        for (TmdbVideo video : videos) {
            String site = video.getSite();
            if (!"YouTube".equals(site)) {
                continue;
            }

            String url = "https://www.youtube.com/watch?v=" + video.getKey();
            addElement(doc, rootElement, "trailer", url);
        }

        // 工作室
        List<TmdbNetwork> networks = tmdb.getNetworks();
        for (TmdbNetwork network : networks) {
            String name = network.getName();
            addElement(doc, rootElement, "studio", name);
        }

        // 保存 NFO 文件
        saveXmlDocument(doc, outputPath);
    }

    /**
     * 添加元素
     *
     * @param doc     Document
     * @param parent  父级
     * @param tagName 标签名
     * @param value   文本值
     */
    private static void addElement(Document doc, Element parent, String tagName, Object value) {
        if (Objects.isNull(value)) {
            return;
        }

        String s = value.toString();

        if (StrUtil.isBlank(s)) {
            return;
        }

        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(s));
        parent.appendChild(element);
    }

    /**
     * 保存
     *
     * @param doc      Document
     * @param savePath 保存位置
     * @throws Exception
     */
    private static void saveXmlDocument(Document doc, String savePath) throws Exception {
        FileUtil.mkdir(new File(savePath).getParentFile());

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(savePath));
        transformer.transform(source, result);

        log.info("已保存NFO {}", savePath);
    }
}
