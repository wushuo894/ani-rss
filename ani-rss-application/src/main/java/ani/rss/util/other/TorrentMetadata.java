package ani.rss.util.other;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class TorrentMetadata {
    private final Map<String, Object> info;
    private final byte[] infoBytes;

    private TorrentMetadata(Map<String, Object> info, byte[] infoBytes) {
        this.info = info;
        this.infoBytes = infoBytes;
    }

    /**
     * 读取并解析种子元数据。
     *
     * @param file 种子文件
     * @return 种子元数据适配对象
     * @throws IOException 种子文件无法读取或 bencode 格式无效
     */
    public static TorrentMetadata from(File file) throws IOException {
        byte[] torrent = Files.readAllBytes(file.toPath());
        try {
            Map<String, Object> metadata = new Bencode(StandardCharsets.ISO_8859_1)
                    .decode(torrent, Type.DICTIONARY);
            Object info = metadata.get("info");
            if (!(info instanceof Map<?, ?>)) {
                throw new IOException("Torrent metadata does not contain an info dictionary");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> infoDictionary = (Map<String, Object>) info;
            return new TorrentMetadata(infoDictionary, extractInfoBytes(torrent));
        } catch (IllegalArgumentException e) {
            throw new IOException("Unable to decode torrent metadata", e);
        }
    }

    /**
     * 获取供 qBittorrent API 使用的信息哈希。
     *
     * @return v1 或 hybrid 种子的 SHA-1 哈希；纯 v2 种子的 SHA-256 哈希
     */
    public String getHash() {
        if (isV2()) {
            return getHashV2();
        }
        return getHashV1();
    }

    public String getHashV1() {
        return shaHex("SHA-1");
    }

    public String getHashV2() {
        return shaHex("SHA-256").substring(0, 40);
    }

    /**
     * 生成兼容种子版本的磁力链接。
     *
     * @return v1 种子包含 {@code btih}，v2 种子包含 {@code btmh}，hybrid 种子包含两者
     */
    public String getMagnetUri() {
        List<String> topics = new ArrayList<>();
        if (hasV1InfoHash()) {
            topics.add("xt=urn:btih:" + shaHex("SHA-1"));
        }
        if (isV2()) {
            // BEP 52: btmh stores the SHA-256 multihash prefix (0x12, 0x20) as 1220.
            topics.add("xt=urn:btmh:1220" + shaHex("SHA-256"));
        }
        return "magnet:?" + String.join("&", topics);
    }

    /**
     * 获取种子内的相对文件路径。
     *
     * @return 与 {@link #getLengths()} 下标一一对应的文件路径
     */
    public String[] getFilenames() {
        return files().stream().map(TorrentFileEntry::name).toArray(String[]::new);
    }

    /**
     * 获取种子内各文件的字节大小。
     *
     * @return 与 {@link #getFilenames()} 下标一一对应的文件大小
     */
    public long[] getLengths() {
        return files().stream().mapToLong(TorrentFileEntry::length).toArray();
    }

    /**
     * 判断种子是否同时携带 v1 信息哈希所需的 pieces 字段。
     *
     * @return 包含 v1 元数据时返回 {@code true}
     */
    private boolean hasV1InfoHash() {
        return info.containsKey("pieces");
    }

    /**
     * 判断种子是否声明 BEP 52 v2 元数据。
     *
     * @return 元数据版本为 2 时返回 {@code true}
     */
    private boolean isV2() {
        return Long.valueOf(2).equals(info.get("meta version"));
    }

    /**
     * 将 v1 平铺文件列表或 v2 文件树统一为文件条目。
     *
     * @return 种子中的文件条目
     */
    private List<TorrentFileEntry> files() {
        Object files = info.get("files");
        if (files instanceof List<?> fileList) {
            return v1Files(fileList);
        }
        if (info.containsKey("file tree")) {
            // Pure v2 torrents replace the v1 flat files list with a nested file tree.
            List<TorrentFileEntry> entries = new ArrayList<>();
            collectV2Files(dictionary(info.get("file tree")), new ArrayList<>(), entries);
            return entries;
        }
        return List.of(new TorrentFileEntry(string(info.get("name")), number(info.get("length"))));
    }

    /**
     * 转换 v1/hybrid 种子的平铺文件列表。
     *
     * @param fileList bencode 解码后的 {@code files} 列表
     * @return 文件条目
     */
    private List<TorrentFileEntry> v1Files(List<?> fileList) {
        List<TorrentFileEntry> entries = new ArrayList<>(fileList.size());
        for (Object file : fileList) {
            Map<String, Object> fileInfo = dictionary(file);
            Object path = fileInfo.get("path");
            if (!(path instanceof List<?> segments)) {
                throw new IllegalArgumentException("Torrent file entry does not contain a path");
            }
            String name = segments.stream().map(TorrentMetadata::string).reduce((a, b) -> a + "/" + b)
                    .orElseThrow(() -> new IllegalArgumentException("Torrent file path is empty"));
            entries.add(new TorrentFileEntry(name, number(fileInfo.get("length"))));
        }
        return entries;
    }

    /**
     * 递归展开 v2 种子的 {@code file tree}。
     *
     * @param tree    当前目录字典
     * @param path    当前相对路径片段
     * @param entries 收集到的文件条目
     */
    private void collectV2Files(Map<String, Object> tree, List<String> path, List<TorrentFileEntry> entries) {
        for (Map.Entry<String, Object> entry : tree.entrySet()) {
            if (entry.getKey().isEmpty()) {
                entries.add(new TorrentFileEntry(String.join("/", path), number(dictionary(entry.getValue()).get("length"))));
                continue;
            }
            List<String> childPath = new ArrayList<>(path);
            childPath.add(entry.getKey());
            collectV2Files(dictionary(entry.getValue()), childPath, entries);
        }
    }

    /**
     * 对原始 {@code info} 字典编码计算十六进制摘要。
     *
     * @param algorithm JCA 消息摘要算法
     * @return 小写十六进制摘要
     */
    private String shaHex(String algorithm) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(infoBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing digest algorithm " + algorithm, e);
        }
    }

    /**
     * 从完整种子字节中提取未重编码的 {@code info} 字典。
     *
     * @param torrent 完整 bencode 种子内容
     * @return 原始 {@code info} 字典编码
     * @throws IOException 种子不符合 bencode 格式或缺少 {@code info}
     */
    private static byte[] extractInfoBytes(byte[] torrent) throws IOException {
        // The info hash is calculated from the exact bencoded info bytes. Re-encoding the
        // decoded map could change binary fields or dictionary ordering and produce a wrong hash.
        BencodeCursor cursor = new BencodeCursor(torrent);
        cursor.expect('d');
        while (!cursor.atEnd('e')) {
            byte[] key = cursor.readBytes();
            int valueStart = cursor.position();
            cursor.skipValue();
            if (Arrays.equals(key, "info".getBytes(StandardCharsets.US_ASCII))) {
                return Arrays.copyOfRange(torrent, valueStart, cursor.position());
            }
        }
        throw new IOException("Torrent metadata does not contain an info dictionary");
    }

    /**
     * 将 bencode 字典值转换为字符串键字典。
     *
     * @param value bencode 解码值
     * @return 字典值
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> dictionary(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException("Expected a bencoded dictionary");
    }

    /**
     * 读取 bencode 字符串值。
     *
     * @param value bencode 解码值
     * @return 字符串值
     */
    private static String string(Object value) {
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("Expected a bencoded string");
    }

    /**
     * 读取 bencode 整数值。
     *
     * @param value bencode 解码值
     * @return 长整型值
     */
    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected a bencoded number");
    }

    private record TorrentFileEntry(String name, long length) {
    }

    private static final class BencodeCursor {
        private final byte[] source;
        private int position;

        private BencodeCursor(byte[] source) {
            this.source = source;
        }

        /**
         * 获取当前读取位置。
         *
         * @return 原始 bencode 字节数组中的偏移量
         */
        private int position() {
            return position;
        }

        /**
         * 读取指定的 bencode 类型标记。
         *
         * @param expected 预期标记
         * @throws IOException 当前字节不符合预期
         */
        private void expect(char expected) throws IOException {
            if (position >= source.length || source[position++] != (byte) expected) {
                throw new IOException("Invalid bencoded torrent data");
            }
        }

        /**
         * 判断并消费容器结束标记。
         *
         * @param expected 容器结束标记
         * @return 已到达结束标记时返回 {@code true}
         * @throws IOException 输入在容器结束前终止
         */
        private boolean atEnd(char expected) throws IOException {
            if (position >= source.length) {
                throw new IOException("Unexpected end of bencoded torrent data");
            }
            if (source[position] == (byte) expected) {
                position++;
                return true;
            }
            return false;
        }

        /**
         * 读取一个 bencode 字节字符串。
         *
         * @return 未经字符集转换的原始字节
         * @throws IOException 字符串长度或边界无效
         */
        private byte[] readBytes() throws IOException {
            int length = readLength();
            if (length > source.length - position) {
                throw new IOException("Invalid bencoded string length");
            }
            byte[] value = Arrays.copyOfRange(source, position, position + length);
            position += length;
            return value;
        }

        /**
         * 读取字节字符串的长度前缀。
         *
         * @return 字节字符串长度
         * @throws IOException 长度前缀无效
         */
        private int readLength() throws IOException {
            int start = position;
            while (position < source.length && source[position] != ':') {
                if (source[position] < '0' || source[position] > '9') {
                    throw new IOException("Invalid bencoded string length");
                }
                position++;
            }
            if (position == start || position == source.length) {
                throw new IOException("Invalid bencoded string length");
            }
            try {
                int length = Integer.parseInt(new String(source, start, position - start, StandardCharsets.US_ASCII));
                position++;
                return length;
            } catch (NumberFormatException e) {
                throw new IOException("Invalid bencoded string length", e);
            }
        }

        /**
         * 跳过一个完整的 bencode 值并停在其末尾。
         *
         * @throws IOException bencode 值不完整或格式无效
         */
        private void skipValue() throws IOException {
            // Only locate the raw info dictionary boundaries here; Bencode decodes the metadata.
            if (position >= source.length) {
                throw new IOException("Unexpected end of bencoded torrent data");
            }
            byte valueType = source[position];
            if (valueType == 'i') {
                position++;
                while (position < source.length && source[position] != 'e') {
                    position++;
                }
                expect('e');
            } else if (valueType == 'l') {
                position++;
                while (!atEnd('e')) {
                    skipValue();
                }
            } else if (valueType == 'd') {
                position++;
                while (!atEnd('e')) {
                    readBytes();
                    skipValue();
                }
            } else {
                readBytes();
            }
        }
    }
}
