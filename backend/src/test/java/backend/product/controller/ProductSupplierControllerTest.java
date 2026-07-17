package backend.product.controller;

import backend.audit.application.AuditLogService;
import backend.auth.dto.LoginResponse;
import backend.common.api.ApiResponse;
import backend.organization.application.OrganizationService;
import backend.product.application.ProductMappingService;
import backend.product.application.ProductService;
import backend.product.domain.repository.ProductSourceMappingRepository;
import backend.product.model.ProductSourceMapping;
import backend.saas.application.SaasTenantService;
import backend.store.application.StoreChannelService;
import backend.supplier.application.SupplierService;
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

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = "app.auth.allow-legacy-header-context=false")
class ProductSupplierControllerTest {

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
    private ProductService productService;

    @Autowired
    private ProductMappingService productMappingService;

    @Autowired
    private ProductSourceMappingRepository productSourceMappingRepository;

    @Autowired
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        productMappingService.clear();
        productService.clear();
        supplierService.clear();
        storeChannelService.clear();
        saasTenantService.clear();
        organizationService.clear();
        auditLogService.clear();
    }

    @Test
    void shouldManageCandidateProductsSuppliersAndProductDrafts() throws Exception {
        TenantFixture fixture = prepareStore("product-center");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        MvcResult candidateCreateResult = mockMvc.perform(post("/api/candidate-products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/p/1001",
                                  "title": "insulated-cup",
                                  "category": "household",
                                  "estimatedProfit": 26.50,
                                  "riskLevel": "low",
                                  "recommendationReason": "stable order and repeat purchase",
                                  "aiSummary": "fit for short video seeding"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andReturn();

        String candidateProductId = objectMapper.readTree(candidateCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("candidateProductId")
                .asText();

        mockMvc.perform(get("/api/candidate-products")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(put("/api/candidate-products/{id}", candidateProductId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceUrl": "https://source.example.com/p/1001?v=2",
                                  "title": "insulated-cup-pro",
                                  "category": "kitchen",
                                  "estimatedProfit": 29.90,
                                  "riskLevel": "low",
                                  "recommendationReason": "better video material",
                                  "aiSummary": "fit for short video and live commerce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("insulated-cup-pro"));

        mockMvc.perform(post("/api/candidate-products/{id}/status", candidateProductId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "testable"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("testable"));

        MvcResult supplierCreateResult = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierPlatformType": "1688",
                                  "supplierPlatformId": "sp-1001",
                                  "supplierName": "yiwu-source-factory",
                                  "sourceUrl": "https://supplier.example.com/1001",
                                  "priceScore": 92,
                                  "deliveryScore": 88,
                                  "stabilityScore": 90,
                                  "riskLevel": "low",
                                  "dropshipSupportFlag": true
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary").value(false))
                .andReturn();

        String supplierId = objectMapper.readTree(supplierCreateResult.getResponse().getContentAsString())
                .path("data")
                .path("supplierId")
                .asText();

        mockMvc.perform(put("/api/suppliers/{id}", supplierId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierName": "yiwu-source-factory-flagship",
                                  "sourceUrl": "https://supplier.example.com/1001/v2",
                                  "priceScore": 95,
                                  "deliveryScore": 89,
                                  "stabilityScore": 93,
                                  "riskLevel": "low",
                                  "dropshipSupportFlag": true,
                                  "blacklistFlag": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierName").value("yiwu-source-factory-flagship"));

        mockMvc.perform(post("/api/suppliers/{id}/set-primary", supplierId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primary").value(true));

        mockMvc.perform(post("/api/suppliers/{id}/set-backup", supplierId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.backup").value(true));

        MvcResult draftGenerateResult = mockMvc.perform(post("/api/product-drafts/generate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-stage2-v1",
                                  "suggestedPrice": 99.90
                                }
                                """.formatted(candidateProductId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_publish"))
                .andReturn();

        String draftId = objectMapper.readTree(draftGenerateResult.getResponse().getContentAsString())
                .path("data")
                .path("productDraftId")
                .asText();

        mockMvc.perform(get("/api/product-drafts")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(put("/api/product-drafts/{id}", draftId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "insulated-cup-best-seller",
                                  "sellingPoints": "portable, insulated, office friendly",
                                  "detailContent": "304 stainless liner and lightweight body",
                                  "faqContent": "supports custom engraving",
                                  "suggestedPrice": 109.90,
                                  "status": "pending_publish"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("insulated-cup-best-seller"));

        mockMvc.perform(post("/api/product-drafts/{id}/publish", draftId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformProductId": "dp-2001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("online"))
                .andExpect(jsonPath("$.data.platformProductId").value("dp-2001"));
    }

    @Test
    void shouldExposeCatalogAndFlagOrphanMappings() throws Exception {
        TenantFixture fixture = prepareStore("product-mapping-catalog");
        String ownerToken = login(OWNER_MOBILE, BOOTSTRAP_PASSWORD);

        String supplierId = objectMapper.readTree(mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "supplierPlatformType": "1688",
                                  "supplierPlatformId": "sp-2001",
                                  "supplierName": "mapping-source-a",
                                  "sourceUrl": "https://supplier.example.com/mapping/2001",
                                  "priceScore": 90,
                                  "deliveryScore": 91,
                                  "stabilityScore": 92,
                                  "riskLevel": "low",
                                  "dropshipSupportFlag": true
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("supplierId").asText();

        String candidateProductId = objectMapper.readTree(mockMvc.perform(post("/api/candidate-products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "sourceType": "1688",
                                  "sourceUrl": "https://source.example.com/p/2001",
                                  "title": "mapping-kettle",
                                  "category": "kitchen",
                                  "estimatedProfit": 32.50,
                                  "riskLevel": "low",
                                  "recommendationReason": "stable conversion",
                                  "aiSummary": "fit for source mapping catalog verification"
                                }
                                """.formatted(fixture.storeId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("candidateProductId").asText();

        String draftId = objectMapper.readTree(mockMvc.perform(post("/api/product-drafts/generate")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "candidateProductId": "%s",
                                  "aiVersion": "ai-stage2-v2",
                                  "suggestedPrice": 129.90
                                }
                                """.formatted(candidateProductId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("productDraftId").asText();

        String productId = objectMapper.readTree(mockMvc.perform(post("/api/product-drafts/{id}/publish", draftId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platformProductId": "dp-3001"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data").path("productId").asText();

        mockMvc.perform(post("/api/product-mappings")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "productId": "%s",
                                  "supplierId": "%s",
                                  "mappingType": "primary"
                                }
                                """.formatted(fixture.storeId(), productId, supplierId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mappingType").value("primary"));

        productSourceMappingRepository.save(new ProductSourceMapping(
                null,
                fixture.storeId(),
                "product-900001",
                supplierId,
                "backup",
                true,
                false,
                OffsetDateTime.parse("2026-07-16T09:30:00Z")
        ));

        JsonNode catalogData = objectMapper.readTree(mockMvc.perform(get("/api/product-mappings/catalog")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data");

        JsonNode normalProduct = findCatalogItem(catalogData, productId);
        assertNotNull(normalProduct);
        assertEquals("mapping-kettle", normalProduct.path("title").asText());
        assertEquals("dp-3001", normalProduct.path("platformProductId").asText());
        assertFalse(normalProduct.path("productMissing").asBoolean());
        assertEquals(1, normalProduct.path("activePrimaryCount").asInt());

        JsonNode orphanProduct = findCatalogItem(catalogData, "product-900001");
        assertNotNull(orphanProduct);
        assertTrue(orphanProduct.path("productMissing").asBoolean());
        assertEquals("product master data missing", orphanProduct.path("title").asText());
        assertEquals("mapping_orphaned", orphanProduct.path("status").asText());
        assertEquals(1, orphanProduct.path("activeBackupCount").asInt());
    }

    private TenantFixture prepareStore(String tenantName) throws Exception {
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
        String tenantId = tenantData.path("tenantId").asText();
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
                                  "shopName": "product-store",
                                  "profitThreshold": 18.00,
                                  "riskThreshold": 70.00
                                }
                                """.formatted(organizationId)))
                .andExpect(status().isOk())
                .andReturn();
        String storeId = objectMapper.readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("storeId")
                .asText();
        return new TenantFixture(tenantId, organizationId, storeId);
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
        ApiResponse<LoginResponse> response = objectMapper.readValue(
                content,
                new TypeReference<>() {
                }
        );
        return response.data().token();
    }

    private JsonNode findCatalogItem(JsonNode catalogData, String productId) {
        for (JsonNode item : catalogData) {
            if (productId.equals(item.path("productId").asText())) {
                return item;
            }
        }
        return null;
    }
}

record TenantFixture(String tenantId, String organizationId, String storeId) {
}
