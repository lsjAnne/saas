package backend.channel.controller;

import backend.channel.application.ChannelAuthService;
import backend.channel.dto.ChannelTokenView;
import backend.channel.model.ChannelType;
import backend.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelCallbackControllerTest {

    private ChannelAuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(ChannelAuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChannelCallbackController(authService)).build();
    }

    @Test
    void shouldReturnSuccessTextOnValidCallback() throws Exception {
        ChannelTokenView view = new ChannelTokenView("channel-token-1", "tenant-1", "org-1", "pdd", "shop",
                "access", "refresh", "Bearer", null, OffsetDateTime.now().plusDays(7),
                OffsetDateTime.now().plusDays(30), Map.of(), OffsetDateTime.now(), OffsetDateTime.now());
        org.mockito.Mockito.when(authService.completeAuthorization(eq(ChannelType.PDD), eq("code-1"), eq("state-1"), any()))
                .thenReturn(view);

        mockMvc.perform(get("/api/channels/callback/pdd")
                        .param("code", "code-1")
                        .param("state", "state-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("授权成功，可关闭页面"));
    }

    @Test
    void shouldReturn400TextWhenStateInvalid() throws Exception {
        doThrow(new BusinessException("8102", "invalid or expired authorization state", null))
                .when(authService).completeAuthorization(eq(ChannelType.PDD), any(), eq("bad-state"), any());

        mockMvc.perform(get("/api/channels/callback/pdd")
                        .param("code", "code-1")
                        .param("state", "bad-state"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("授权失败: invalid or expired authorization state"));
    }

    @Test
    void shouldReturn400TextWhenChannelTypeUnknown() throws Exception {
        mockMvc.perform(get("/api/channels/callback/fake")
                        .param("code", "c")
                        .param("state", "s"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("unsupported channel type: fake"));
    }
}
