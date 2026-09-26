package win.cntier.tag.internal.http;

import win.cntier.tag.api.model.ProfileResult;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.internal.config.PluginSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.GZIPInputStream;

public final class CnTierHttpClient {

    private final Object rankingLock = new Object();
    private volatile HttpClient client;
    private volatile RankingCache rankingCache;
    private CompletableFuture<RankingLoadResult> rankingInFlight;

    public CnTierHttpClient(PluginSettings settings) {
        reload(settings);
    }

    public void reload(PluginSettings settings) {
        this.client = HttpClient.newBuilder()
            .connectTimeout(settings.requestTimeout())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        clearCache();
    }

    public CompletableFuture<ProfileResult> fetch(
        UUID uuid,
        String playerName,
        PluginSettings settings,
        boolean forceRefresh
    ) {
        return loadRanking(settings, forceRefresh).thenCompose(ranking -> {
            ProfileResult result = rankingResult(uuid, playerName, ranking);
            if (result.status() == ProfileStatus.AVAILABLE || !settings.hasApiKey()) {
                return CompletableFuture.completedFuture(result);
            }
            return fetchAuthenticated(uuid, settings);
        });
    }

    public void clearCache() {
        synchronized (rankingLock) {
            rankingCache = null;
            rankingInFlight = null;
        }
    }

    private CompletableFuture<RankingLoadResult> loadRanking(
        PluginSettings settings,
        boolean forceRefresh
    ) {
        String endpoint = settings.apiBaseUrl() + "/api/ranking/overall";
        long now = System.currentTimeMillis();
        RankingCache cached = rankingCache;
        if (!forceRefresh && cached != null && cached.isFresh(endpoint, now)) {
            return CompletableFuture.completedFuture(RankingLoadResult.available(cached.index()));
        }

        synchronized (rankingLock) {
            cached = rankingCache;
            if (!forceRefresh && cached != null && cached.isFresh(endpoint, now)) {
                return CompletableFuture.completedFuture(RankingLoadResult.available(cached.index()));
            }
            if (rankingInFlight != null) {
                return rankingInFlight;
            }

            final HttpRequest request;
            try {
                request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(settings.requestTimeout())
                    .header("Accept", "application/json")
                    .header("Accept-Encoding", "gzip")
                    .header("User-Agent", "CNTierTag/1.0.0")
                    .GET()
                    .build();
            } catch (IllegalArgumentException exception) {
                return CompletableFuture.completedFuture(RankingLoadResult.failed(
                    ProfileStatus.ERROR,
                    "总榜地址无效：" + exception.getMessage()
                ));
            }

            HttpClient requestClient = client;
            CompletableFuture<RankingLoadResult> future = requestClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::mapRankingResponse)
                .exceptionally(throwable -> RankingLoadResult.failed(
                    ProfileStatus.ERROR,
                    "总榜请求失败：" + usefulMessage(throwable)
                ));
            rankingInFlight = future;
            future.whenComplete((result, throwable) -> {
                synchronized (rankingLock) {
                    if (rankingInFlight != future) {
                        return;
                    }
                    rankingInFlight = null;
                    if (throwable == null && result.status() == ProfileStatus.AVAILABLE) {
                        rankingCache = new RankingCache(
                            endpoint,
                            result.index(),
                            System.currentTimeMillis() + settings.successTtl().toMillis()
                        );
                    }
                }
            });
            return future;
        }
    }

    private ProfileResult rankingResult(
        UUID uuid,
        String playerName,
        RankingLoadResult ranking
    ) {
        if (ranking.status() != ProfileStatus.AVAILABLE) {
            return ProfileResult.of(ranking.status(), ranking.detail());
        }
        return ranking.index().find(uuid, playerName)
            .map(ProfileResult::available)
            .orElseGet(() -> ProfileResult.of(
                ProfileStatus.NOT_FOUND,
                "总榜中未找到该玩家"
            ));
    }

    private RankingLoadResult mapRankingResponse(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        if (status == 429) {
            return RankingLoadResult.failed(ProfileStatus.RATE_LIMITED, "请求过于频繁，服务端返回 HTTP 429");
        }
        if (status == 401 || status == 403) {
            return RankingLoadResult.failed(ProfileStatus.UNAUTHORIZED, "总榜拒绝访问，HTTP " + status);
        }
        if (status < 200 || status >= 300) {
            return RankingLoadResult.failed(ProfileStatus.ERROR, "总榜返回 HTTP " + status);
        }

        try {
            return RankingLoadResult.available(OverallRankingParser.parse(responseBody(response)));
        } catch (RuntimeException exception) {
            return RankingLoadResult.failed(ProfileStatus.ERROR, "总榜响应解析失败：" + usefulMessage(exception));
        }
    }

    private CompletableFuture<ProfileResult> fetchAuthenticated(UUID uuid, PluginSettings settings) {

        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                .uri(URI.create(settings.apiBaseUrl() + "/api/v1/player/" + uuid))
                .timeout(settings.requestTimeout())
                .header("Accept", "application/json")
                .header("Accept-Encoding", "gzip")
                .header("X-Api-Key", settings.apiKey())
                .header("User-Agent", "CNTierTag/1.0.0")
                .GET()
                .build();
        } catch (IllegalArgumentException exception) {
            return CompletableFuture.completedFuture(
                ProfileResult.of(ProfileStatus.ERROR, "API 地址无效：" + exception.getMessage())
            );
        }

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(response -> mapResponse(uuid, response))
            .exceptionally(throwable -> ProfileResult.of(
                ProfileStatus.ERROR,
                "玩家接口请求失败：" + usefulMessage(throwable)
            ));
    }

    private ProfileResult mapResponse(UUID expectedUuid, HttpResponse<byte[]> response) {
        int status = response.statusCode();
        if (status == 404) {
            return ProfileResult.of(ProfileStatus.NOT_FOUND, "玩家接口中未找到该 UUID");
        }
        if (status == 401 || status == 403) {
            return ProfileResult.of(ProfileStatus.UNAUTHORIZED, "API Key 验证失败");
        }
        if (status == 429) {
            return ProfileResult.of(ProfileStatus.RATE_LIMITED, "请求过于频繁，服务端返回 HTTP 429");
        }
        if (status < 200 || status >= 300) {
            return ProfileResult.of(ProfileStatus.ERROR, "玩家接口返回 HTTP " + status);
        }

        try {
            var profile = ProfileParser.parse(responseBody(response));
            if (!profile.uuid().equals(expectedUuid)) {
                return ProfileResult.of(ProfileStatus.ERROR, "玩家接口返回的 UUID 与请求不一致");
            }
            return ProfileResult.available(profile);
        } catch (RuntimeException exception) {
            return ProfileResult.of(ProfileStatus.ERROR, "玩家响应解析失败：" + usefulMessage(exception));
        }
    }

    private static String responseBody(HttpResponse<byte[]> response) {
        byte[] body = response.body();
        String encoding = response.headers().firstValue("Content-Encoding").orElse("");
        if (encoding.toLowerCase().contains("gzip")) {
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(body))) {
                body = input.readAllBytes();
            } catch (IOException exception) {
                throw new IllegalArgumentException("gzip 响应解压失败", exception);
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private static String usefulMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
            ? current.getClass().getSimpleName()
            : message;
    }

    private record RankingCache(
        String endpoint,
        OverallRankingParser.RankingIndex index,
        long expiresAtMillis
    ) {
        private boolean isFresh(String expectedEndpoint, long now) {
            return endpoint.equals(expectedEndpoint) && expiresAtMillis > now;
        }
    }

    private record RankingLoadResult(
        ProfileStatus status,
        OverallRankingParser.RankingIndex index,
        String detail
    ) {
        private static RankingLoadResult available(OverallRankingParser.RankingIndex index) {
            return new RankingLoadResult(ProfileStatus.AVAILABLE, index, "");
        }

        private static RankingLoadResult failed(ProfileStatus status, String detail) {
            return new RankingLoadResult(status, null, detail);
        }
    }
}
