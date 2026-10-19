package backend.channel.application;

import backend.channel.model.ChannelAfterSale;
import backend.channel.model.ChannelAuthContext;
import backend.channel.model.ChannelCallbackRequest;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelOrder;
import backend.channel.model.ChannelProduct;
import backend.channel.model.ChannelShipRequest;
import backend.channel.model.ChannelShipResult;
import backend.channel.model.ChannelToken;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChannelClientRegistryTest {

    @Test
    void shouldResolveRegisteredClientAndExposeSupportedTypes() {
        ChannelClient pddClient = fake(ChannelType.PDD);
        ChannelClient jdClient = fake(ChannelType.JD);
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of(pddClient, jdClient));

        assertThat(registry.resolve(ChannelType.PDD)).isSameAs(pddClient);
        assertThat(registry.supports(ChannelType.JD)).isTrue();
        assertThat(registry.supports(ChannelType.DOUYIN)).isFalse();
        assertThat(registry.supportedTypes()).containsExactlyInAnyOrder(ChannelType.PDD, ChannelType.JD);
    }

    @Test
    void shouldThrow8101WhenNoClientRegisteredForType() {
        ChannelClientRegistry registry = new ChannelClientRegistry(List.of());
        assertThatThrownBy(() -> registry.resolve(ChannelType.DOUYIN))
                .isInstanceOf(BusinessException.class)
                .matches(error -> ((BusinessException) error).code().equals("8101"));
    }

    private ChannelClient fake(ChannelType channelType) {
        return new ChannelClient() {
            @Override
            public ChannelType channelType() {
                return channelType;
            }

            @Override
            public String buildAuthorizeUrl(ChannelAuthContext context) {
                return "http://mock/" + channelType.code();
            }

            @Override
            public ChannelToken handleCallback(ChannelCallbackRequest request) {
                return null;
            }

            @Override
            public ChannelToken refreshToken(ChannelToken current) {
                return current;
            }

            @Override
            public ChannelConnectivity checkConnectivity(ChannelAuthContext context) {
                return null;
            }

            @Override
            public List<ChannelProduct> pullProducts(ChannelAuthContext context) {
                return List.of();
            }

            @Override
            public List<ChannelOrder> pullOrders(ChannelAuthContext context) {
                return List.of();
            }

            @Override
            public ChannelShipResult shipOrder(ChannelShipRequest request) {
                return null;
            }

            @Override
            public List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context) {
                return List.of();
            }
        };
    }
}
