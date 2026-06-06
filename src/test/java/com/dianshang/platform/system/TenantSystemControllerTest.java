package com.dianshang.platform.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantSystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnTenantHealth() throws Exception {
        mockMvc.perform(get("/api/tenant/system/health")
                        .header("X-Tenant-Id", "tenant-a")
                        .header("X-Operator-Id", "owner-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
