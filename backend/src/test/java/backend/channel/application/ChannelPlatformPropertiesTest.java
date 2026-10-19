package backend.channel.application;

import backend.channel.model.ChannelType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelPlatformPropertiesTest {

    @Test
    void shouldFallbackToMockDefaultsWhenPlatformNotConfigured() {
        ChannelPlatformProperties properties = new ChannelPlatformProperties();

        ChannelPlatformProperties.ChannelPlatformSettings settings = properties.settings(ChannelType.PDD);
        assertThat(settings.isMockMode()).isTrue();
        assertThat(settings.isEnabled()).isTrue();
        assertThat(settings.getEndpoint()).isEqualTo("internal://mock/pdd");
        assertThat(settings.getCallbackPath()).isEqualTo("/api/channels/callback/pdd");
        assertThat(properties.isMockMode(ChannelType.PDD)).isTrue();
        assertThat(properties.endpoint(ChannelType.JD)).isEqualTo("internal://mock/jd");
    }

    @Test
    void shouldBindConfiguredPlatformOverrides() {
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        Map<String, ChannelPlatformProperties.ChannelPlatformSettings> platforms = new LinkedHashMap<>();
        ChannelPlatformProperties.ChannelPlatformSettings taobao = new ChannelPlatformProperties.ChannelPlatformSettings();
        taobao.setMockMode(false);
        taobao.setEndpoint("https://eco.taobao.com/router/rest");
        taobao.setAppKey("tb-key");
        taobao.setAppSecret("tb-secret");
        taobao.setCallbackPath("/api/channels/callback/taobao");
        platforms.put("taobao", taobao);
        properties.setPlatforms(platforms);

        assertThat(properties.isMockMode(ChannelType.TAOBAO)).isFalse();
        assertThat(properties.endpoint(ChannelType.TAOBAO)).isEqualTo("https://eco.taobao.com/router/rest");
        assertThat(properties.appKey(ChannelType.TAOBAO)).isEqualTo("tb-key");
        assertThat(properties.appSecret(ChannelType.TAOBAO)).isEqualTo("tb-secret");
        assertThat(properties.callbackPath(ChannelType.TAOBAO)).isEqualTo("/api/channels/callback/taobao");
    }

    @Test
    void shouldListAllSixPlatforms() {
        ChannelPlatformProperties properties = new ChannelPlatformProperties();
        assertThat(properties.listSettings()).hasSize(6);
    }
}
