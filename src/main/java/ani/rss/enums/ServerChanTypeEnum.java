package ani.rss.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServerChanTypeEnum {
    SERVER_CHAN("serverChan", "server酱", "https://sctapi.ftqq.com/<sendKey>.send", "SCT"),
    SERVER_CHAN_3("serverChan3", "server酱³", "https://<uid>.push.ft07.com/send/<sendKey>.send", "sct");

    private final String type;
    private final String name;
    private final String url;
    private final String sendkeyPrefix;
}
