package win.cntier.tag.internal.http;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import win.cntier.tag.api.model.ProfileStatus;
import win.cntier.tag.internal.config.PluginSettings;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CnTierLiveTest {

    @Test
    void 公开总榜无需Key即可返回玩家数据() throws Exception {
        assumeTrue(
            "true".equalsIgnoreCase(System.getenv("CNTIER_RUN_LIVE_TESTS")),
            "普通构建不碰公网；要联调就把 CNTIER_RUN_LIVE_TESTS 设成 true"
        );
        String testUuid = System.getenv().getOrDefault(
            "CNTIER_TEST_UUID",
            "fce48c86-3519-463c-ae0f-1fabb39fc280"
        );
        String testName = System.getenv().getOrDefault("CNTIER_TEST_NAME", "Axelili");

        YamlConfiguration config = new YamlConfiguration();
        config.set("api.timeout-seconds", 15);
        PluginSettings settings = PluginSettings.from(config);
        var result = new CnTierHttpClient(settings)
            .fetch(UUID.fromString(testUuid), testName, settings, true)
            .get(Duration.ofSeconds(20).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

        assertEquals(ProfileStatus.AVAILABLE, result.status(), result.detail());
        assertEquals(UUID.fromString(testUuid), result.profile().uuid());
        assertFalse(result.profile().tierRecords().isEmpty(), "测试玩家没有段位记录，请更新测试样本");
    }
}
