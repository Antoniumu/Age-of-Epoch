package com.robertx22.age_of_exile.gamemaster;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Мини веб-сервер панели GameMaster.
 * <p>
 * Слушает localhost (по умолчанию), отдаёт страницу с графиками и JSON-состояние,
 * принимает переключатели. Никаких внешних библиотек и CDN — только JDK.
 * <p>
 * Все обращения к состоянию игры маршалятся в серверный поток Minecraft.
 */
public class GameMasterWebServer {

    private final GameMaster gameMaster;
    private final String host;
    private final int port;
    private final String token;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private ExecutorService executor;
    private volatile boolean running = false;

    private String cachedDashboard;

    public GameMasterWebServer(GameMaster gameMaster, String host, int port, String token) {
        this.gameMaster = gameMaster;
        this.host = host == null || host.isEmpty() ? "127.0.0.1" : host;
        this.port = port;
        this.token = token == null ? "" : token;
    }

    public void start() throws Exception {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(host, port), 16);
        running = true;

        executor = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r, "AgeOfEpoch-GameMaster-HTTP");
            thread.setDaemon(true);
            return thread;
        });

        acceptThread = new Thread(this::acceptLoop, "AgeOfEpoch-GameMaster-Listener");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        if (executor != null) {
            // без interrupt: запрос, который сейчас меняет настройки, должен успеть ответить
            executor.shutdown();
        }
        serverSocket = null;
        executor = null;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ExecutorService pool = executor;
                if (pool != null) {
                    pool.submit(() -> handle(socket));
                } else {
                    socket.close();
                }
            } catch (Exception e) {
                if (running) {
                    System.out.println("[GameMaster] Ошибка веб-сервера: " + e);
                }
                if (!running) {
                    return;
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (socket) {
            socket.setSoTimeout(8000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0].toUpperCase(Locale.ROOT) : "GET";
            String target = parts.length > 1 ? parts[1] : "/";

            Map<String, String> headers = new HashMap<>();
            int contentLength = 0;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx)
                    .trim()
                    .toLowerCase(Locale.ROOT);
                String value = line.substring(idx + 1)
                    .trim();
                headers.put(key, value);
                if (key.equals("content-length")) {
                    try {
                        contentLength = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            String body = "";
            if (contentLength > 0) {
                int toRead = Math.min(contentLength, 65536);
                char[] buffer = new char[toRead];
                int read = 0;
                while (read < toRead) {
                    int r = reader.read(buffer, read, toRead - read);
                    if (r < 0) {
                        break;
                    }
                    read += r;
                }
                body = new String(buffer, 0, read);
            }

            String path = target;
            String query = "";
            int q = target.indexOf('?');
            if (q >= 0) {
                path = target.substring(0, q);
                query = target.substring(q + 1);
            }
            Map<String, String> params = parseQuery(query);

            if (!token.isEmpty()) {
                String provided = params.getOrDefault("token", headers.getOrDefault("x-gm-token", ""));
                if (!token.equals(provided)) {
                    respond(socket, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
                    return;
                }
            }

            if (path.equals("/") || path.equals("/index.html")) {
                respond(socket, 200, "text/html; charset=utf-8", dashboardHtml());
                return;
            }

            if (path.equals("/api/state")) {
                respond(socket, 200, "application/json; charset=utf-8", stateJson());
                return;
            }

            if (path.equals("/api/telemetry")) {
                respond(socket, 200, "application/json; charset=utf-8", telemetryJson());
                return;
            }

            if (path.equals("/api/report") && method.equals("POST")) {
                JsonObject response = new JsonObject();
                try {
                    java.nio.file.Path report = onServerThread(() -> {
                        try {
                            return gameMaster.telemetry()
                                .writeSeasonReport(GameMaster.now());
                        } catch (Exception e) {
                            return null;
                        }
                    });
                    response.addProperty("ok", true);
                    response.addProperty("path", report == null ? "" : report.toString());
                } catch (Exception e) {
                    response.addProperty("ok", false);
                    response.addProperty("error", String.valueOf(e));
                }
                respond(socket, 200, "application/json; charset=utf-8", response.toString());
                return;
            }

            if (path.equals("/api/config") && method.equals("POST")) {
                JsonObject json;
                try {
                    json = new JsonParser().parse(body)
                        .getAsJsonObject();
                } catch (Exception e) {
                    respond(socket, 400, "application/json; charset=utf-8", "{\"error\":\"bad_json\"}");
                    return;
                }
                List<String> changed = onServerThread(() -> gameMaster.applyConfigJson(json));
                JsonObject response = new JsonObject();
                response.addProperty("ok", true);
                JsonArray array = new JsonArray();
                if (changed != null) {
                    changed.forEach(array::add);
                }
                response.add("changed", array);
                respond(socket, 200, "application/json; charset=utf-8", response.toString());
                return;
            }

            if (path.equals("/api/save") && method.equals("POST")) {
                onServerThread(() -> {
                    gameMaster.save();
                    return null;
                });
                respond(socket, 200, "application/json; charset=utf-8", "{\"ok\":true}");
                return;
            }

            respond(socket, 404, "application/json; charset=utf-8", "{\"error\":\"not_found\"}");
        } catch (Exception ignored) {
            // браузер закрыл соединение или таймаут — это нормально
        }
    }

    private String telemetryJson() {
        String json = onServerThread(gameMaster::buildTelemetryJson);
        if (json == null) {
            return "{\"error\":\"server_not_ready\"}";
        }
        return json;
    }

    private String stateJson() {
        String json = onServerThread(gameMaster::buildStateJson);
        if (json == null) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "server_not_ready");
            return error.toString();
        }
        return json;
    }

    /** Выполняет задачу в потоке сервера и ждёт результат. */
    private <T> T onServerThread(java.util.function.Supplier<T> supplier) {
        MinecraftServer server = gameMaster.server();
        if (server == null) {
            return null;
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    private String dashboardHtml() {
        if (cachedDashboard != null) {
            return cachedDashboard;
        }
        try (InputStream stream = GameMasterWebServer.class.getResourceAsStream(
            "/assets/mmorpg/gamemaster/dashboard.html")) {
            if (stream != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                cachedDashboard = out.toString(StandardCharsets.UTF_8);
                return cachedDashboard;
            }
        } catch (Exception e) {
            System.out.println("[GameMaster] Не удалось загрузить dashboard.html: " + e);
        }
        cachedDashboard = "<html><body><h1>Age of Epoch GameMaster</h1>"
            + "<p>dashboard.html не найден. Состояние: <a href=\"/api/state\">/api/state</a></p></body></html>";
        return cachedDashboard;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            try {
                if (eq > 0) {
                    params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                } else if (!pair.isEmpty()) {
                    params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {
            }
        }
        return params;
    }

    private static void respond(Socket socket, int code, String contentType, String body) throws Exception {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        String status;
        switch (code) {
            case 200 -> status = "200 OK";
            case 400 -> status = "400 Bad Request";
            case 401 -> status = "401 Unauthorized";
            case 404 -> status = "404 Not Found";
            default -> status = code + " Response";
        }
        String head = "HTTP/1.1 " + status + "\r\n"
            + "Content-Type: " + contentType + "\r\n"
            + "Content-Length: " + bytes.length + "\r\n"
            + "Cache-Control: no-store\r\n"
            + "Access-Control-Allow-Origin: *\r\n"
            + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    public String url() {
        return "http://" + host + ":" + port + "/";
    }
}
