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

import java.util.List;

/**
 * 渠道适配器契约。后续 6 个平台适配器（拼多多/淘宝/抖音/视频号/快手/京东）各自实现本接口并注册为 Spring Bean。
 * 本签名已冻结，后续并行任务不得修改。
 */
public interface ChannelClient {

    ChannelType channelType();

    String buildAuthorizeUrl(ChannelAuthContext context);

    ChannelToken handleCallback(ChannelCallbackRequest request);

    ChannelToken refreshToken(ChannelToken current);

    ChannelConnectivity checkConnectivity(ChannelAuthContext context);

    List<ChannelProduct> pullProducts(ChannelAuthContext context);

    List<ChannelOrder> pullOrders(ChannelAuthContext context);

    ChannelShipResult shipOrder(ChannelShipRequest request);

    List<ChannelAfterSale> pullAfterSales(ChannelAuthContext context);
}
