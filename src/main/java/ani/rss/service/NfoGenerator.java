package ani.rss.service;

import ani.rss.entity.tmdb.*;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
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

public class NfoGenerator {

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

        saveXmlDocument(doc, outputPath);
    }

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
            addElement(doc, rootElement, "tmdbegid", tmdbGroupId);
        }

        List<TmdbGenres> genres = tmdb.getGenres();

        for (TmdbGenres genre : genres) {
            addElement(doc, rootElement, "genre", genre.getName());
        }

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
            addElement(doc, rootElement, "tmdbegid", tmdbGroupId);
        }

        List<TmdbGenres> genres = tmdb.getGenres();

        for (TmdbGenres genre : genres) {
            addElement(doc, rootElement, "genre", genre.getName());
        }

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

    private static void addElement(Document doc, Element parent, String tagName, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value));
        parent.appendChild(element);
    }

    private static void saveXmlDocument(Document doc, String filePath) throws Exception {
        FileUtil.mkdir(new File(filePath).getParentFile());

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
    }
}
