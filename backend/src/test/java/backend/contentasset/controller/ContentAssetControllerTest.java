package backend.contentasset.controller;

import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.common.api.ApiResponse;
import backend.contentasset.application.ContentAssetService;
import backend.organization.application.OrganizationService;
import backend.saas.application.SaasTenantService;
import backend.store.application.StoreChannelService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class ContentAssetControllerTest {

    private static final String OWNER_MOBILE = "13800000000";
    private static final String BOOTSTRAP_PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaasTenantService saasTenantService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private StoreChannelService storeChannelService;

    @Autowired
    private ContentAssetService contentAssetService;

    @BeforeEach
    void setUp() {
        contentAssetService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldManageContentAssetsLifecycle() throws Exception {
        String storeId = prepareStore("content-asset-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        MvcResult uploadResult = mockMvc.perform(post("/api/content-assets/upload")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "assetCategory": "product",
                                  "assetType": "image",
                                  "assetName": "product-main-cover",
                                  "sourceUrl": "https://assets.example.com/p/main-cover.png",
                                  "previewText": "used by product detail and short video cover",
                                  "changeSummary": "initial upload",
                                  "referenceHint": "product detail hero"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asset.assetCategory").value("product"))
                .andExpect(jsonPath("$.data.versions.length()").value(1))
                .andExpect(jsonPath("$.data.references.length()").value(2))
                .andReturn();

        String assetId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("asset")
                .path("assetId")
                .asText();

        mockMvc.perform(get("/api/content-assets")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/content-assets/{id}", assetId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asset.assetName").value("product-main-cover"));

        MvcResult generateResult = mockMvc.perform(post("/api/content-assets/generate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "assetCategory": "live",
                                  "assetType": "script",
                                  "assetName": "live-opening-script",
                                  "brief": "explain material, bonus and order reason in 30 seconds",
                                  "tone": "fast-paced",
                                  "referenceHint": "live opening"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asset.generated").value(true))
                .andReturn();

        String generatedAssetId = objectMapper.readTree(generateResult.getResponse().getContentAsString())
                .path("data")
                .path("asset")
                .path("assetId")
                .asText();

        mockMvc.perform(post("/api/content-assets/{id}/archive-version", generatedAssetId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "live script archived after acceptance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asset.assetStatus").value("archived"))
                .andExpect(jsonPath("$.data.versions[0].versionStatus").value("archived"));

        mockMvc.perform(post("/api/content-assets/{id}/copy-reference", assetId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value(assetId))
                .andExpect(jsonPath("$.data.referenceText").value(org.hamcrest.Matchers.containsString("asset://")));
    }

    @Test
    void shouldGenerateProductVideoFromImageAndPublishIt() throws Exception {
        String storeId = prepareStore("content-video-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        MvcResult uploadResult = mockMvc.perform(post("/api/content-assets/upload")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "assetCategory": "product",
                                  "assetType": "image",
                                  "assetName": "tea-set-main-image",
                                  "sourceUrl": "https://assets.example.com/p/tea-set-main-image.png",
                                  "previewText": "hero image for short video generation",
                                  "changeSummary": "product image uploaded",
                                  "referenceHint": "product hero image"
                                }
                                """.formatted(storeId)))
                .andExpect(status().isOk())
                .andReturn();

        String sourceAssetId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data")
                .path("asset")
                .path("assetId")
                .asText();

        MvcResult generateResult = mockMvc.perform(post("/api/content-assets/generate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "assetCategory": "product",
                                  "assetType": "video",
                                  "assetName": "tea-set-short-video",
                                  "brief": "turn the hero image into a 20-second product short video",
                                  "tone": "conversion-first",
                                  "referenceHint": "short video launch",
                                  "sourceAssetId": "%s",
                                  "clipTemplate": "highlight-carousel",
                                  "durationSeconds": 20,
                                  "publishPlatform": "douyin"
                                }
                                """.formatted(storeId, sourceAssetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asset.assetType").value("video"))
                .andExpect(jsonPath("$.data.asset.previewUrl").value(org.hamcrest.Matchers.containsString("generated://video/")))
                .andExpect(jsonPath("$.data.asset.previewUrl").value(org.hamcrest.Matchers.containsString("source=")))
                .andExpect(jsonPath("$.data.versions.length()").value(2))
                .andExpect(jsonPath("$.data.references.length()").value(3))
                .andReturn();

        String videoAssetId = objectMapper.readTree(generateResult.getResponse().getContentAsString())
                .path("data")
                .path("asset")
                .path("assetId")
                .asText();

        mockMvc.perform(post("/api/content-assets/{id}/publish", videoAssetId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformCode": "douyin",
                                  "publishTitle": "tea-set-20s-short-video",
                                  "publishRemark": "publish after clip review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.asset.assetId").value(videoAssetId))
                .andExpect(jsonPath("$.data.versions[0].changeSummary").value(org.hamcrest.Matchers.containsString("published to douyin")))
                .andExpect(jsonPath("$.data.references[0].referenceType").value("publish"));
    }

    private String prepareStore(String tenantName) throws Exception {
        MvcResult tenantResult = mockMvc.perform(post("/api/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantName": "%s",
                                  "ownerName": "owner-a",
                                  "mobile": "13800000000"
                                }
                                """.formatted(tenantName)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode tenantData = objectMapper.readTree(tenantResult.getResponse().getContentAsString()).path("data");
        String organizationId = tenantData.path("defaultOrganizationId").asText();
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        MvcResult storeResult = mockMvc.perform(post("/api/stores/connect")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organizationId": "%s",
                                  "ownerUserId": "tenant-admin",
                                  "platformType": "douyin",
                                  "platformShopId": "shop-001",
                                  "shopName": "content-store",
                                  "profitThreshold": 18.00,
                                  "riskThreshold": 70.00
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
    }

    private String login(String username, String password) throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(content, new TypeReference<>() {
        });
        return response.data().token();
    }
}
