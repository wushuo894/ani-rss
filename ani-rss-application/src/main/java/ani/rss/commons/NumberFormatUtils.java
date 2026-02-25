package ani.rss.commons;

import java.text.NumberFormat;

/**
 * 数字格式化
 */
public class NumberFormatUtils {

    /**
     * @param number                数字
     * @param maximumFractionDigits 最大小数位
     * @param minimumFractionDigits 最小小数位
     * @return
     */
    public static String format(Number number, Integer maximumFractionDigits, Integer minimumFractionDigits) {
        NumberFormat numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(maximumFractionDigits);
        numberInstance.setMinimumFractionDigits(minimumFractionDigits);
        return numberInstance.format(number);
    }
}
