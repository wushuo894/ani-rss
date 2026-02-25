package ani.rss.util.basic;

import cn.hutool.core.lang.PatternPool;

import java.math.BigInteger;
import java.util.regex.Pattern;

public class CidrRangeChecker {

    /**
     * 验证 CIDR 表示法的正则表达式
     */
    public static final Pattern CIDR_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/([0-9]|[12][0-9]|3[0-2])$"
    );

    /**
     * 判断 IP 地址是否在指定的 CIDR 范围内
     *
     * @param ipAddress 要检查的 IPv4 地址
     * @param cidrRange CIDR 表示法的网络范围 (如 "10.0.0.0/8")
     * @return 如果 IP 在范围内返回 true，否则返回 false
     * @throws IllegalArgumentException 如果输入格式无效
     */
    public static boolean isIpInRange(String ipAddress, String cidrRange) {
        if (!PatternPool.IPV4.matcher(ipAddress).matches()) {
            return false;
        }

        if (!CIDR_PATTERN.matcher(cidrRange).matches()) {
            return false;
        }

        // 分割 CIDR 表示法
        String[] cidrParts = cidrRange.split("/");
        String networkAddress = cidrParts[0];
        int prefixLength = Integer.parseInt(cidrParts[1]);

        // 将 IP 地址转换为 32 位整数
        BigInteger ip = ipToBigInteger(ipAddress);

        // 将网络地址转换为 32 位整数
        BigInteger network = ipToBigInteger(networkAddress);

        // 计算子网掩码
        BigInteger mask = BigInteger.valueOf(0xFFFFFFFFL)
                .shiftLeft(32 - prefixLength)
                .and(BigInteger.valueOf(0xFFFFFFFFL));

        // 网络地址的最低有效位应为 0
        BigInteger networkBase = network.and(mask);

        // 计算 IP 地址所属的网络范围
        BigInteger ipNetwork = ip.and(mask);

        // 比较网络地址
        return ipNetwork.equals(networkBase);
    }

    /**
     * 将点分十进制 IP 地址转换为 32 位无符号整数（BigInteger）
     */
    private static BigInteger ipToBigInteger(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value <<= 8;
            value |= Integer.parseInt(octets[i]);
        }
        return BigInteger.valueOf(value).and(BigInteger.valueOf(0xFFFFFFFFL));
    }
}
