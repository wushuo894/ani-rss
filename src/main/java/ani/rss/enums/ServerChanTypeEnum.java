package ani.rss.enums;

public enum ServerChanTypeEnum {
    SERVER_CHAN("serverChan", "server酱", "https://sctapi.ftqq.com/<sendKey>.send", "SCT"),
    SERVER_CHAN_3("serverChan3", "server酱³", "https://<sendKey>.push.ft07.com/send", "sct");

    private String type;
    private String name;
    private String url;
    private String sendkeyPrefix;

    ServerChanTypeEnum(String type, String name, String url, String sendkeyPrefix) {
        this.type = type;
        this.name = name;
        this.url = url;
        this.sendkeyPrefix = sendkeyPrefix;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getSendkeyPrefix() {
        return sendkeyPrefix;
    }
}
