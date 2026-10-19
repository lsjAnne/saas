package backend.channel.controller;

import backend.channel.application.ChannelAuthService;
import backend.channel.application.ChannelSyncService;
import backend.channel.dto.ChannelAuthorizeView;
import backend.channel.dto.ChannelSyncResult;
import backend.channel.dto.ChannelTokenView;
import backend.channel.model.ChannelConnectivity;
import backend.channel.model.ChannelShipResult;
import backend.tenant.context.TenantContext;
import backend.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelControllerTest {

    private ChannelAuthService authService;
    private ChannelSyncService syncService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(ChannelAuthService.class);
        syncService = mock(ChannelSyncService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelController(authService, syncService)).build();
        TenantContextHolder.set(new TenantContext("tenant-1", "op", "admin"));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldStartAuthorization() throws Exception {
        when(authService.startAuthorization(eq("tenant-1"), eq("org-1"), any()))
                .thenReturn(new ChannelAuthorizeView("pdd", "http://auth.pdd", "state-1"));

        mockMvc.perform(post("/api/channels/pdd/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationId\":\"org-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelType").value("pdd"))
                .andExpect(jsonPath("$.data.authorizeUrl").value("http://auth.pdd"))
                .andExpect(jsonPath("$.data.state").value("state-1"));
    }

    @Test
    void shouldSyncOrders() throws Exception {
        when(syncService.syncOrders(eq("tenant-1"), eq("ch-1"), eq("store-1")))
                .thenReturn(new ChannelSyncResult("pdd", "store-1", "orders", 2, 1, 1, List.of()));

        mockMvc.perform(post("/api/channels/sync/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelAccountId\":\"ch-1\",\"storeId\":\"store-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncType").value("orders"))
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    void shouldShipOrder() throws Exception {
        when(syncService.shipOrder(eq("tenant-1"), any()))
                .thenReturn(new ChannelShipResult("pdd-order-1001", true, "ok", "pdd-ship-1"));

        mockMvc.perform(post("/api/channels/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store-1\",\"channelAccountId\":\"ch-1\",\"platformOrderId\":\"pdd-order-1001\",\"logisticsCompany\":\"sf\",\"logisticsNo\":\"SF1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.platformShipmentId").value("pdd-ship-1"));
    }

    @Test
    void shouldCheckConnectivity() throws Exception {
        when(syncService.checkConnectivity(eq("tenant-1"), eq("ch-1")))
                .thenReturn(new ChannelConnectivity(true, "mock ok", OffsetDateTime.now()));

        mockMvc.perform(post("/api/channels/pdd/connectivity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelAccountId\":\"ch-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reachable").value(true));
    }

    @Test
    void shouldListTokens() throws Exception {
        ChannelTokenView view = new ChannelTokenView("channel-token-1", "tenant-1", "org-1", "pdd",
                "shop", "access", "refresh", "Bearer", null, OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(30), Map.of(), OffsetDateTime.now(), OffsetDateTime.now());
        when(authService.listTokens("tenant-1")).thenReturn(List.of(view));

        mockMvc.perform(get("/api/channels/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountName").value("shop"));
    }

    @Test
    void shouldRefreshToken() throws Exception {
        when(authService.refreshToken(eq("tenant-1"), eq("channel-token-1")))
                .thenReturn(new ChannelTokenView("channel-token-1", "tenant-1", "org-1", "pdd", "shop",
                        "access-v2", "refresh", "Bearer", null, OffsetDateTime.now().plusDays(7),
                        OffsetDateTime.now().plusDays(30), Map.of(), OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/channels/pdd/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelTokenId\":\"channel-token-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-v2"));
    }

    @Test
    void shouldSyncProductsAndAfterSales() throws Exception {
        when(syncService.syncProducts(eq("tenant-1"), eq("ch-1"), eq("store-1")))
                .thenReturn(new ChannelSyncResult("pdd", "store-1", "products", 1, 0, 0, List.of()));
        when(syncService.syncAfterSales(eq("tenant-1"), eq("ch-1"), eq("store-1")))
                .thenReturn(new ChannelSyncResult("pdd", "store-1", "after_sales", 1, 0, 0, List.of()));

        mockMvc.perform(post("/api/channels/sync/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelAccountId\":\"ch-1\",\"storeId\":\"store-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncType").value("products"));

        mockMvc.perform(post("/api/channels/sync/after-sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channelAccountId\":\"ch-1\",\"storeId\":\"store-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncType").value("after_sales"));
    }
}
