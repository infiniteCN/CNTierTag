package win.cntier.tag.internal.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.internal.config.PluginSettings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CnTierHttpClientTest {

    private static final UUID FIRST = UUID.fromString("fce48c86-3519-463c-ae0f-1fabb39fc280");
    private static final UUID SECOND = UUID.fromString("f4c6cc7f-bba5-46fd-b8fb-6b1f6b5b4921");

    private HttpServer server;
    private AtomicInteger rankingRequests;
    private volatile String receivedApiKey;

    @BeforeEach
    void setUp() throws IOException {
        rankingRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/ranking/overall", this::serveRanking);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void 多名玩家共用一次匿名总榜而且请求里没有Key() throws Exception {
        PluginSettings settings = settings();
        CnTierHttpClient client = new CnTierHttpClient(settings);

        var first = client.fetch(FIRST, "Axelili", settings, false).get(3, TimeUnit.SECONDS);
        var second = client.fetch(SECOND, "PeakPlayer", settings, false).get(3, TimeUnit.SECONDS);

        assertEquals(ProfileStatus.AVAILABLE, first.status(), first.detail());
        assertEquals(ProfileStatus.AVAILABLE, second.status(), second.detail());
        assertEquals(1, rankingRequests.get());
        assertNull(receivedApiKey);
    }

    @Test
    void 强制刷新会重新拉榜但并发请求仍然合并() throws Exception {
        PluginSettings settings = settings();
        CnTierHttpClient client = new CnTierHttpClient(settings);
        client.fetch(FIRST, "Axelili", settings, false).get(3, TimeUnit.SECONDS);

        var firstRefresh = client.fetch(FIRST, "Axelili", settings, true);
        var secondRefresh = client.fetch(SECOND, "PeakPlayer", settings, true);
        firstRefresh.get(3, TimeUnit.SECONDS);
        secondRefresh.get(3, TimeUnit.SECONDS);

        assertEquals(2, rankingRequests.get());
    }

    private PluginSettings settings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("api.base-url", "http://127.0.0.1:" + server.getAddress().getPort());
        config.set("api.timeout-seconds", 3);
        config.set("cache.success-seconds", 60);
        return PluginSettings.from(config);
    }

    private void serveRanking(HttpExchange exchange) throws IOException {
        rankingRequests.incrementAndGet();
        receivedApiKey = exchange.getRequestHeaders().getFirst("X-Api-Key");
        byte[] body = ("""
            [
              {"uuid":"%s","name":"Axelili","region":"华东","modeTiers":{"Sword":"HT3"}},
              {"uuid":"%s","name":"PeakPlayer","region":"华南","modeTiers":{"NPOT":"Peak HT3|LT3"}}
            ]
            """.formatted(FIRST, SECOND)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
