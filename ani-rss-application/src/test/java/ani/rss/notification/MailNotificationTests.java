package ani.rss.notification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 邮箱收件人解析测试
 */
class MailNotificationTests {

    @Test
    void testSingleAddressee() {
        List<String> addressees = MailNotification.parseMailAddressees("xx@qq.com");
        assertEquals(List.of("xx@qq.com"), addressees);
    }

    @Test
    void testMultipleAddressees() {
        List<String> addressees = MailNotification.parseMailAddressees(
                "a@xx.com,b@xx.com,c@xx.com");
        assertEquals(
                List.of("a@xx.com", "b@xx.com", "c@xx.com"),
                addressees);
    }

    @Test
    void testTrimAndIgnoreEmpty() {
        List<String> addressees = MailNotification.parseMailAddressees(" a@xx.com , ,b@xx.com ");
        assertEquals(List.of("a@xx.com", "b@xx.com"), addressees);
    }

    @Test
    void testBlank() {
        assertThrows(IllegalArgumentException.class, () -> MailNotification.parseMailAddressees(null));
        assertThrows(IllegalArgumentException.class, () -> MailNotification.parseMailAddressees(""));
        assertThrows(IllegalArgumentException.class, () -> MailNotification.parseMailAddressees("   "));
    }

    @Test
    void testOnlySeparators() {
        assertThrows(IllegalArgumentException.class, () -> MailNotification.parseMailAddressees(",,,"));
    }
}
