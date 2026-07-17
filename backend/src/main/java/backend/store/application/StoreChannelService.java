package backend.store.application;

import backend.audit.application.AuditLogService;
import backend.common.exception.BusinessException;
import backend.organization.domain.repository.OrganizationRepository;
import backend.organization.model.Organization;
import backend.saas.domain.repository.UsageQuotaRepository;
import backend.saas.model.UsageQuota;
import backend.store.domain.repository.ChannelAccountRepository;
import backend.store.domain.repository.StoreRepository;
import backend.store.dto.ConnectStoreRequest;
import backend.store.dto.CreateChannelAccountRequest;
import backend.store.dto.UpdateStoreSettingsRequest;
import backend.store.model.ChannelAccount;
import backend.store.model.Store;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StoreChannelService {

    private final AuditLogService auditLogService;
    private final OrganizationRepository organizationRepository;
    private final UsageQuotaRepository usageQuotaRepository;
    private final StoreRepository storeRepository;
    private final ChannelAccountRepository channelAccountRepository;

    public StoreChannelService(AuditLogService auditLogService,
                               OrganizationRepository organizationRepository,
                               UsageQuotaRepository usageQuotaRepository,
                               StoreRepository storeRepository,
                               ChannelAccountRepository channelAccountRepository) {
        this.auditLogService = auditLogService;
        this.organizationRepository = organizationRepository;
        this.usageQuotaRepository = usageQuotaRepository;
        this.storeRepository = storeRepository;
        this.channelAccountRepository = channelAccountRepository;
    }

    public Store connectStore(String tenantId, ConnectStoreRequest request) {
        requireOwnedOrganization(tenantId, request.organizationId());
        storeRepository.findByTenantAndPlatformShop(tenantId, request.platformType(), request.platformShopId())
                .ifPresent(store -> {
                    throw new BusinessException("7001", "store already exists for the same platform shop", HttpStatus.BAD_REQUEST);
                });
        consumeStoreQuota(tenantId);
        Store store = storeRepository.save(new Store(
                null,
                tenantId,
                request.organizationId(),
                request.ownerUserId(),
                request.platformType(),
                request.platformShopId(),
                request.shopName(),
                "connected",
                request.profitThreshold(),
                request.riskThreshold(),
                normalizeMap(request.defaultShipConfig()),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CONNECT_STORE", "store", store.storeId());
        return store;
    }

    public List<Store> listStores(String tenantId) {
        return storeRepository.findByTenantId(tenantId);
    }

    public Store getStore(String tenantId, String storeId) {
        return requireOwnedStore(tenantId, storeId);
    }

    public Store updateStoreSettings(String tenantId, String storeId, UpdateStoreSettingsRequest request) {
        Store current = requireOwnedStore(tenantId, storeId);
        Store updated = storeRepository.save(current.withSettings(
                request.profitThreshold(),
                request.riskThreshold(),
                normalizeMap(request.defaultShipConfig())
        ));
        auditLogService.recordForTenant(tenantId, "UPDATE_STORE_SETTINGS", "store", storeId);
        return updated;
    }

    public List<ChannelAccount> listChannelAccounts(String tenantId) {
        List<String> organizationIds = organizationRepository.findByTenantId(tenantId).stream()
                .map(Organization::id)
                .toList();
        return channelAccountRepository.findByOrganizationIds(organizationIds);
    }

    public ChannelAccount createChannelAccount(String tenantId, CreateChannelAccountRequest request) {
        requireOwnedOrganization(tenantId, request.organizationId());
        ChannelAccount channelAccount = channelAccountRepository.save(new ChannelAccount(
                null,
                request.organizationId(),
                request.channelType(),
                request.accountName(),
                "connected",
                OffsetDateTime.now().plusDays(30),
                normalizeMap(request.extraConfig()),
                OffsetDateTime.now()
        ));
        auditLogService.recordForTenant(tenantId, "CREATE_CHANNEL_ACCOUNT", "channel_account", channelAccount.channelAccountId());
        return channelAccount;
    }

    public ChannelAccount refreshChannelAuth(String tenantId, String channelAccountId) {
        ChannelAccount current = requireOwnedChannelAccount(tenantId, channelAccountId);
        ChannelAccount updated = channelAccountRepository.save(
                current.withAuthStatus("connected", OffsetDateTime.now().plusDays(30))
        );
        auditLogService.recordForTenant(tenantId, "REFRESH_CHANNEL_AUTH", "channel_account", channelAccountId);
        return updated;
    }

    public void clear() {
        channelAccountRepository.deleteAll();
        storeRepository.deleteAll();
    }

    private Store requireOwnedStore(String tenantId, String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(store.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return store;
    }

    private ChannelAccount requireOwnedChannelAccount(String tenantId, String channelAccountId) {
        ChannelAccount channelAccount = channelAccountRepository.findByChannelAccountId(channelAccountId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        Organization organization = requireOwnedOrganization(tenantId, channelAccount.organizationId());
        if (!organization.id().equals(channelAccount.organizationId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return channelAccount;
    }

    private Organization requireOwnedOrganization(String tenantId, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException("1003", "object not found", HttpStatus.NOT_FOUND));
        if (!tenantId.equals(organization.tenantId())) {
            throw new BusinessException("1005", "tenant access denied", HttpStatus.FORBIDDEN);
        }
        return organization;
    }

    private void consumeStoreQuota(String tenantId) {
        List<UsageQuota> quotas = usageQuotaRepository.findByTenantId(tenantId);
        for (int index = 0; index < quotas.size(); index++) {
            UsageQuota current = quotas.get(index);
            if (!"store_count".equals(current.quotaCode())) {
                continue;
            }
            int nextUsedAmount = current.usedAmount() + 1;
            if (nextUsedAmount > current.quotaLimit()) {
                throw new BusinessException(
                        "6002",
                        "quota exceeded",
                        Map.of(
                                "quotaCode", current.quotaCode(),
                                "quotaLimit", current.quotaLimit(),
                                "usedAmount", current.usedAmount()
                        ),
                        HttpStatus.BAD_REQUEST
                );
            }
            ArrayList<UsageQuota> updated = new ArrayList<>(quotas);
            updated.set(index, current.withUsedAmount(nextUsedAmount));
            usageQuotaRepository.saveAll(tenantId, updated);
            return;
        }
    }

    private Map<String, Object> normalizeMap(Map<String, Object> values) {
        return values == null ? Map.of() : values;
    }
}

