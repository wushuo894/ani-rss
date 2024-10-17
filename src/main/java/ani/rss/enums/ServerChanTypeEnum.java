package ani.rss.enums;

import lombok.Getter;

@Getter
public enum ServerChanTypeEnum {
    SERVER_CHAN("serverChan", "server酱", "https://sctapi.ftqq.com/<sendKey>.send", "SCT"),
    SERVER_CHAN_3("serverChan3", "server酱³", "https://<sendKey>.push.ft07.com/send", "sct");

    private final String type;
    private final String name;
    private final String url;
    private final String sendkeyPrefix;

    ServerChanTypeEnum(String type, String name, String url, String sendKeyPrefix) {
        this.type = type;
        this.name = name;
        this.url = url;
        this.sendkeyPrefix = sendKeyPrefix;
    }

}
