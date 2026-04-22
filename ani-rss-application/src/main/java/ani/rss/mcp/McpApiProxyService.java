package ani.rss.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class McpApiProxyService {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public JsonObject callEndpoint(McpEndpoint endpoint, JsonObject arguments, HttpServletRequest request) throws Exception {
        JsonObject query = getObject(arguments, "query");
        JsonObject headers = getObject(arguments, "headers");
        JsonElement body = arguments.has("body") ? arguments.get("body") : null;

        URI uri = buildUri(request, endpoint.path(), query);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "*/*");

        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            builder.header("Authorization", authorization);
        }

        for (Map.Entry<String, JsonElement> entry : headers.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                builder.header(entry.getKey(), entry.getValue().getAsString());
            }
        }

        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();

        if ("POST".equalsIgnoreCase(endpoint.method())) {
            if (endpoint.multipart()) {
                MultipartPayload multipartPayload = buildMultipartPayload(arguments, body);
                builder.header("Content-Type", "multipart/form-data; boundary=" + multipartPayload.boundary());
                publisher = multipartPayload.publisher();
            } else if (Objects.nonNull(body) && !body.isJsonNull()) {
                builder.header("Content-Type", "application/json");
                publisher = HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);
            }
        }

        HttpRequest httpRequest = builder.method(endpoint.method(), publisher).build();
        HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

        JsonObject result = new JsonObject();
        result.addProperty("endpoint", endpoint.key());
        result.addProperty("path", endpoint.path());
        result.addProperty("status", response.statusCode());
        result.addProperty("ok", response.statusCode() >= 200 && response.statusCode() < 300);

        String contentType = response.headers().firstValue("content-type").orElse("");
        result.addProperty("contentType", contentType);

        byte[] responseBody = response.body();

        if (looksLikeJson(contentType)) {
            String text = new String(responseBody, StandardCharsets.UTF_8);
            JsonElement jsonElement = JsonParser.parseString(text);
            result.add("data", jsonElement);
            return result;
        }

        result.addProperty("binary", true);
        result.addProperty("size", responseBody.length);
        result.addProperty("base64", Base64.getEncoder().encodeToString(responseBody));

        return result;
    }

    private URI buildUri(HttpServletRequest request, String path, JsonObject query) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme())
                .append("://")
                .append(request.getServerName());

        boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443);

        if (!defaultPort) {
            sb.append(":").append(request.getServerPort());
        }

        sb.append(path);

        if (!query.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, JsonElement> entry : query.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                String value = entry.getValue().isJsonPrimitive()
                        ? URLEncoder.encode(entry.getValue().getAsString(), StandardCharsets.UTF_8)
                        : URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8);
                sb.append(key).append('=').append(value);
            }
        }

        return URI.create(sb.toString());
    }

    private static JsonObject getObject(JsonObject root, String key) {
        if (root == null || !root.has(key)) {
            return new JsonObject();
        }

        JsonElement element = root.get(key);
        if (!element.isJsonObject()) {
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static boolean looksLikeJson(String contentType) {
        if (contentType == null) {
            return false;
        }

        String lower = contentType.toLowerCase();
        return lower.contains("application/json") || lower.contains("+json") || lower.contains("text/json");
    }

    private static MultipartPayload buildMultipartPayload(JsonObject arguments, JsonElement body) throws IOException {
        if (!arguments.has("fileBase64") || arguments.get("fileBase64").isJsonNull()) {
            throw new IllegalArgumentException("multipart endpoint requires fileBase64");
        }

        String fileName = arguments.has("fileName") && !arguments.get("fileName").isJsonNull()
                ? arguments.get("fileName").getAsString()
                : "upload.bin";

        String base64 = arguments.get("fileBase64").getAsString();
        byte[] fileBytes = Base64.getDecoder().decode(base64);

        String boundary = "AniRssMcpBoundary" + UUID.randomUUID().toString().replace("-", "");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeBinaryPart(output, boundary, "file", fileName, fileBytes);

        if (Objects.nonNull(body) && body.isJsonObject()) {
            JsonObject fields = body.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
                writeTextPart(output, boundary, entry.getKey(), entry.getValue().toString());
            }
        }

        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return new MultipartPayload(boundary, HttpRequest.BodyPublishers.ofByteArray(output.toByteArray()));
    }

    private static void writeTextPart(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: text/plain; charset=UTF-8\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBinaryPart(
            ByteArrayOutputStream output,
            String boundary,
            String name,
            String fileName,
            byte[] bytes
    ) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        output.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private record MultipartPayload(String boundary, HttpRequest.BodyPublisher publisher) {
    }
}
