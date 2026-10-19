package backend.channel.model;

import backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelTypeTest {

    @Test
    void shouldExposeExactlySixPlatformCodes() {
        assertThat(ChannelType.values()).hasSize(6);
        assertThat(ChannelType.PDD.code()).isEqualTo("pdd");
        assertThat(ChannelType.TAOBAO.code()).isEqualTo("taobao");
        assertThat(ChannelType.DOUYIN.code()).isEqualTo("douyin");
        assertThat(ChannelType.WECHAT_CHANNELS.code()).isEqualTo("wechat_channels");
        assertThat(ChannelType.KUAISHOU.code()).isEqualTo("kuaishou");
        assertThat(ChannelType.JD.code()).isEqualTo("jd");
    }

    @Test
    void shouldResolveKnownCodes() {
        assertThat(ChannelType.fromCode("pdd")).isEqualTo(ChannelType.PDD);
        assertThat(ChannelType.fromCode("jd")).isEqualTo(ChannelType.JD);
        assertThat(ChannelType.tryFromCode("wechat_channels")).isEqualTo(Optional.of(ChannelType.WECHAT_CHANNELS));
    }

    @Test
    void shouldReturnEmptyForUnknownCodeWithoutThrowing() {
        assertThat(ChannelType.tryFromCode("unknown")).isEmpty();
        assertThat(ChannelType.tryFromCode(null)).isEmpty();
        assertThat(ChannelType.tryFromCode("  ")).isEmpty();
    }

    @Test
    void shouldThrow8104WhenFromCodeReceivesUnknownValue() {
        assertThatThrownBy(() -> ChannelType.fromCode("fake"))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8104"));
    }
}
