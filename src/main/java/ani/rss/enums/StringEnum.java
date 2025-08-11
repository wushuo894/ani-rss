package ani.rss.enums;

public class StringEnum {
    public static final String SEASON_REG = "[Ss](\\d+)[Ee](\\d+(\\.5)?)";
    public static final String YEAR_REG = " ?\\(((19|20)\\d{2})\\)";
    public static final String TMDB_ID_REG = " ?\\[tmdbid=(\\d+)]";
    public static final String PLEX_TMDB_ID_REG = " ?\\{tmdb-(\\d+)}";
    public static final String MAGNET_REG = "^magnet\\:\\?xt=urn:btih\\:(\\w+)";
    public static final String SUBGROUP_REG_STR = "^\\{\\{(.+)}}:(.+)$";
}
