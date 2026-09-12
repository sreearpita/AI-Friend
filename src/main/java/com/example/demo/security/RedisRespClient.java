package com.example.demo.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.example.demo.config.AiFriendProperties;
import com.example.demo.exception.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RedisRespClient {
    private final AiFriendProperties properties;

    public RedisRespClient(AiFriendProperties properties) {
        this.properties = properties;
    }

    public boolean putIfAbsent(String key, String value, Duration ttl) {
        Object response = command("SET", prefixed(key), value, "NX", "EX", String.valueOf(Math.max(1, ttl.toSeconds())));
        if ("OK".equals(response)) {
            return true;
        }
        if (response == null) {
            return false;
        }
        throw unavailable();
    }

    public LimitResult incrementAndCheck(String key, Duration window, int limit) {
        String redisKey = prefixed(key);
        Object increment = command("INCR", redisKey);
        if (!(increment instanceof Long current)) {
            throw unavailable();
        }
        if (current == 1L) {
            command("EXPIRE", redisKey, String.valueOf(Math.max(1, window.toSeconds())));
        }
        Object ttl = command("TTL", redisKey);
        int retryAfter = ttl instanceof Long seconds && seconds > 0 ? seconds.intValue() : (int) window.toSeconds();
        return new LimitResult(current <= limit, current, limit, Math.max(1, retryAfter));
    }

    private Object command(String... args) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(properties.getRedis().getHost(), properties.getRedis().getPort()),
                    properties.getRedis().getTimeoutMs());
            socket.setSoTimeout(properties.getRedis().getTimeoutMs());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            if (StringUtils.hasText(properties.getRedis().getPassword())) {
                writeCommand(out, "AUTH", properties.getRedis().getPassword());
                readResponse(in);
            }
            writeCommand(out, args);
            return readResponse(in);
        } catch (IOException exception) {
            throw unavailable();
        }
    }

    private void writeCommand(BufferedOutputStream out, String... args) throws IOException {
        out.write(("*" + args.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            out.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    private Object readResponse(BufferedInputStream in) throws IOException {
        int type = in.read();
        if (type == -1) {
            throw unavailable();
        }
        String line = readLine(in);
        return switch ((char) type) {
            case '+' -> line;
            case ':' -> Long.parseLong(line);
            case '$' -> readBulkString(in, Integer.parseInt(line));
            case '-' -> throw unavailable();
            default -> throw unavailable();
        };
    }

    private String readBulkString(BufferedInputStream in, int length) throws IOException {
        if (length < 0) {
            return null;
        }
        byte[] data = in.readNBytes(length);
        in.readNBytes(2);
        return new String(data, StandardCharsets.UTF_8);
    }

    private String readLine(BufferedInputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        int previous = -1;
        int current;
        while ((current = in.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                builder.setLength(builder.length() - 1);
                return builder.toString();
            }
            builder.append((char) current);
            previous = current;
        }
        throw unavailable();
    }

    private String prefixed(String key) {
        return properties.getRedis().getKeyPrefix() + ":" + key;
    }

    private ApiException unavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "RATE_LIMIT_SERVICE_UNAVAILABLE",
                "Rate limiting service is unavailable.");
    }
}
