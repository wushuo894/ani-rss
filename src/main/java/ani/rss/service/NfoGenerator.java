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
        addElement(doc, rootElement, "rating", String.valueOf(tmdbEpisode.getVoteAverage()));
        addElement(doc, rootElement, "year", String.valueOf(DateUtil.year(tmdbEpisode.getAirDate())));
        addElement(doc, rootElement, "aired", DateUtil.format(tmdbEpisode.getAirDate(), DatePattern.NORM_DATE_PATTERN));
        addElement(doc, rootElement, "episode", String.valueOf(tmdbEpisode.getEpisodeNumber()));
        addElement(doc, rootElement, "season", String.valueOf(tmdbEpisode.getSeasonNumber()));
        addElement(doc, rootElement, "runtime", String.valueOf(tmdbEpisode.getRuntime()));

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
        addElement(doc, rootElement, "seasonnumber", tmdbSeason.getSeasonNumber().toString());

        Date airDate = tmdbSeason.getAirDate();

        if (Objects.nonNull(airDate)) {
            addElement(doc, rootElement, "year", String.valueOf(DateUtil.year(airDate)));
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
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element rootElement = doc.createElement("tvshow");
        doc.appendChild(rootElement);

        addElement(doc, rootElement, "tmdbid", String.valueOf(tmdb.getId()));
        addElement(doc, rootElement, "title", tmdb.getName());
        addElement(doc, rootElement, "originaltitle", tmdb.getOriginalName());
        addElement(doc, rootElement, "year", String.valueOf(DateUtil.year(tmdb.getDate())));
        addElement(doc, rootElement, "plot", tmdb.getOverview());
        addElement(doc, rootElement, "rating", String.valueOf(tmdb.getVoteAverage()));
        addElement(doc, rootElement, "trailer", tmdb.getHomepage());
        addElement(doc, rootElement, "releasedate", DateUtil.format(tmdb.getDate(), DatePattern.NORM_DATE_PATTERN));

        String tmdbGroupId = tmdb.getTmdbGroupId();
        if (StrUtil.isNotBlank(tmdbGroupId)) {
            // 剧集组编号
            addElement(doc, rootElement, "tmdbegid", tmdbGroupId);
        }

        // 种类
        List<TmdbGenres> genres = tmdb.getGenres();
        for (TmdbGenres genre : genres) {
            addElement(doc, rootElement, "genre", genre.getName());
        }

        // 演职人员
        List<TmdbCredit> credits = tmdb.getCredits();
        for (TmdbCredit credit : credits) {
            Element actor = doc.createElement("actor");
            rootElement.appendChild(actor);

            addElement(doc, actor, "name", credit.getName());
            addElement(doc, actor, "role", credit.getCharacter());
            addElement(doc, actor, "type", "Actor");
            addElement(doc, actor, "tmdbid", String.valueOf(credit.getId()));
        }

        saveXmlDocument(doc, outputPath);
    }

    /**
     * 电影nfo
     *
     * @param tmdb       tmdb
     * @param outputPath 输出位置
     * @throws Exception
     */
    public static void generateMovieNfo(Tmdb tmdb, String outputPath) throws Exception {
        // 创建 XML 文档
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // 创建根元素
        Element rootElement = doc.createElement("movie");
        doc.appendChild(rootElement);

        // 添加基本信息
        addElement(doc, rootElement, "tmdbid", String.valueOf(tmdb.getId()));
        addElement(doc, rootElement, "title", tmdb.getName());
        addElement(doc, rootElement, "originaltitle", tmdb.getOriginalName());
        addElement(doc, rootElement, "year", String.valueOf(DateUtil.year(tmdb.getDate())));
        addElement(doc, rootElement, "plot", tmdb.getOverview());
        addElement(doc, rootElement, "rating", String.valueOf(tmdb.getVoteAverage()));
        addElement(doc, rootElement, "votes", String.valueOf(tmdb.getVoteCount()));
        addElement(doc, rootElement, "trailer", tmdb.getHomepage());
        addElement(doc, rootElement, "releasedate", DateUtil.format(tmdb.getDate(), DatePattern.NORM_DATE_PATTERN));

        String tmdbGroupId = tmdb.getTmdbGroupId();
        if (StrUtil.isNotBlank(tmdbGroupId)) {
            // 剧集组编号
            addElement(doc, rootElement, "tmdbegid", tmdbGroupId);
        }

        // 类型
        List<TmdbGenres> genres = tmdb.getGenres();
        for (TmdbGenres genre : genres) {
            addElement(doc, rootElement, "genre", genre.getName());
        }

        // 演职人员
        List<TmdbCredit> credits = tmdb.getCredits();
        for (TmdbCredit credit : credits) {
            Element actor = doc.createElement("actor");
            rootElement.appendChild(actor);

            addElement(doc, actor, "name", credit.getName());
            addElement(doc, actor, "role", credit.getCharacter());
            addElement(doc, actor, "type", "Actor");
            addElement(doc, actor, "tmdbid", String.valueOf(credit.getId()));
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
    private static void addElement(Document doc, Element parent, String tagName, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value));
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

        log.info("已保存nfo {}", savePath);
    }
}
