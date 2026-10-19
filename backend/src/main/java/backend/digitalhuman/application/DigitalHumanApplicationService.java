package backend.digitalhuman.application;

import backend.common.exception.BusinessException;
import backend.digitalhuman.domain.repository.DigitalHumanAvatarRepository;
import backend.digitalhuman.domain.repository.DigitalHumanScheduleRepository;
import backend.digitalhuman.domain.repository.DigitalHumanScriptRepository;
import backend.digitalhuman.domain.repository.DigitalHumanSessionRepository;
import backend.digitalhuman.model.DigitalHumanAvatar;
import backend.digitalhuman.model.DigitalHumanSchedule;
import backend.digitalhuman.model.DigitalHumanScript;
import backend.digitalhuman.model.DigitalHumanSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数字人无人直播编排服务。
 * 当前实现以 mock 状态机跑通完整闭环（形象/话术/排班/开播会话），
 * 真实平台驱动（抖音/视频号/快手等开放平台的数字人开播能力）待接入联调，
 * 接入点见 startSchedule 与 heartbeat 的 TODO 注释。
 */
@Service
public class DigitalHumanApplicationService {

    private final DigitalHumanAvatarRepository avatarRepository;
    private final DigitalHumanScriptRepository scriptRepository;
    private final DigitalHumanScheduleRepository scheduleRepository;
    private final DigitalHumanSessionRepository sessionRepository;

    public DigitalHumanApplicationService(DigitalHumanAvatarRepository avatarRepository,
                                          DigitalHumanScriptRepository scriptRepository,
                                          DigitalHumanScheduleRepository scheduleRepository,
                                          DigitalHumanSessionRepository sessionRepository) {
        this.avatarRepository = avatarRepository;
        this.scriptRepository = scriptRepository;
        this.scheduleRepository = scheduleRepository;
        this.sessionRepository = sessionRepository;
    }

    // ---------- 数字人形象 ----------

    public DigitalHumanAvatar createAvatar(String tenantId, CreateAvatarRequest request) {
        return avatarRepository.save(new DigitalHumanAvatar(
                null,
                tenantId,
                request.organizationId(),
                request.avatarName(),
                request.avatarType(),
                request.voiceName(),
                request.previewUrl(),
                request.enabled() == null || request.enabled(),
                OffsetDateTime.now()
        ));
    }

    public DigitalHumanAvatar updateAvatar(String tenantId, String avatarId, CreateAvatarRequest request) {
        DigitalHumanAvatar existing = requireAvatar(tenantId, avatarId);
        return avatarRepository.save(existing.withEditableFields(
                request.avatarName(),
                request.avatarType(),
                request.voiceName(),
                request.previewUrl(),
                request.enabled() == null || request.enabled()
        ));
    }

    public List<DigitalHumanAvatar> listAvatars(String tenantId) {
        return avatarRepository.listByTenant(tenantId);
    }

    public void deleteAvatar(String tenantId, String avatarId) {
        requireAvatar(tenantId, avatarId);
        avatarRepository.deleteById(tenantId, avatarId);
    }

    // ---------- 话术/剧本 ----------

    public DigitalHumanScript createScript(String tenantId, CreateScriptRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        return scriptRepository.save(new DigitalHumanScript(
                null,
                tenantId,
                request.organizationId(),
                request.scriptName(),
                request.sceneType(),
                request.contentText(),
                request.version() == null || request.version().isBlank() ? "v1" : request.version(),
                request.status() == null || request.status().isBlank() ? "draft" : request.status(),
                now,
                now
        ));
    }

    public DigitalHumanScript updateScript(String tenantId, String scriptId, CreateScriptRequest request) {
        DigitalHumanScript existing = requireScript(tenantId, scriptId);
        return scriptRepository.save(existing.withEditableFields(
                request.scriptName(),
                request.sceneType(),
                request.contentText(),
                request.version() == null || request.version().isBlank() ? "v1" : request.version(),
                request.status() == null || request.status().isBlank() ? "draft" : request.status()
        ));
    }

    public List<DigitalHumanScript> listScripts(String tenantId) {
        return scriptRepository.listByTenant(tenantId);
    }

    // ---------- 无人排班 ----------

    public DigitalHumanSchedule createSchedule(String tenantId, CreateScheduleRequest request) {
        return scheduleRepository.save(new DigitalHumanSchedule(
                null,
                tenantId,
                request.organizationId(),
                request.scheduleName(),
                request.avatarId(),
                request.scriptId(),
                request.platformType(),
                request.repeatType(),
                request.startAt(),
                request.endAt(),
                request.enabled() == null || request.enabled(),
                "scheduled",
                OffsetDateTime.now()
        ));
    }

    public DigitalHumanSchedule updateSchedule(String tenantId, String scheduleId, CreateScheduleRequest request) {
        DigitalHumanSchedule existing = requireSchedule(tenantId, scheduleId);
        return scheduleRepository.save(existing.withEditableFields(
                request.scheduleName(),
                request.avatarId(),
                request.scriptId(),
                request.platformType(),
                request.repeatType(),
                request.startAt(),
                request.endAt(),
                request.enabled() == null || request.enabled(),
                existing.status()
        ));
    }

    public List<DigitalHumanSchedule> listSchedules(String tenantId) {
        return scheduleRepository.listByTenant(tenantId);
    }

    /**
     * 按排班启动一场数字人无人直播。
     * 当前为 mock 开播（mock_flag=1，房间号 mock-room-*）；
     * TODO(联调)：接入平台开放平台的数字人开播接口，替换 mock 房间号与状态回读。
     */
    public DigitalHumanSession startSchedule(String tenantId, String scheduleId) {
        DigitalHumanSchedule schedule = requireSchedule(tenantId, scheduleId);
        DigitalHumanAvatar avatar = requireAvatar(tenantId, schedule.avatarId());
        DigitalHumanScript script = requireScript(tenantId, schedule.scriptId());
        OffsetDateTime now = OffsetDateTime.now();
        DigitalHumanSchedule updated = schedule.withStatus("running");
        scheduleRepository.save(updated);

        String roomId = "mock-room-" + ThreadLocalRandom.current().nextInt(100000, 999999);
        DigitalHumanSession session = sessionRepository.save(new DigitalHumanSession(
                null,
                tenantId,
                schedule.organizationId(),
                scheduleId,
                avatar.avatarId(),
                roomId,
                "live",
                now,
                null,
                0,
                0,
                BigDecimal.ZERO,
                now,
                true,
                now
        ));
        // TODO(联调)：按 script.contentText() 编排数字人话术并推送到平台直播间
        return session;
    }

    public DigitalHumanSession stopSchedule(String tenantId, String scheduleId) {
        DigitalHumanSchedule schedule = requireSchedule(tenantId, scheduleId);
        scheduleRepository.save(schedule.withStatus("stopped"));
        List<DigitalHumanSession> liveSessions = sessionRepository.listLiveByTenant(tenantId);
        OffsetDateTime now = OffsetDateTime.now();
        DigitalHumanSession ended = null;
        for (DigitalHumanSession session : liveSessions) {
            if (scheduleId.equals(session.scheduleId())) {
                ended = sessionRepository.save(session.withLiveState("ended", session.startedAt(), now, now));
            }
        }
        if (ended == null) {
            throw new BusinessException("2001", "no live session for schedule: " + scheduleId, HttpStatus.NOT_FOUND);
        }
        return ended;
    }

    // ---------- 开播会话 ----------

    public List<DigitalHumanSession> listSessions(String tenantId) {
        return sessionRepository.listByTenant(tenantId);
    }

    /**
     * 心跳保活：更新心跳时间并模拟指标增长。
     * TODO(联调)：真实模式下改为回读平台直播间实时数据（观众/订单/成交）。
     */
    public DigitalHumanSession heartbeat(String tenantId, String sessionId) {
        DigitalHumanSession session = requireSession(tenantId, sessionId);
        OffsetDateTime now = OffsetDateTime.now();
        long viewers = session.viewerPeak() + ThreadLocalRandom.current().nextInt(1, 10);
        long orders = session.orderCount() + ThreadLocalRandom.current().nextInt(0, 3);
        BigDecimal gmv = session.gmvAmount().add(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(0, 120)));
        DigitalHumanSession updated = session.withMetrics(viewers, orders, gmv)
                .withLiveState("live", session.startedAt(), null, now);
        return sessionRepository.save(updated);
    }

    public DigitalHumanOverviewView getOverview(String tenantId) {
        List<DigitalHumanAvatar> avatars = avatarRepository.listByTenant(tenantId);
        List<DigitalHumanScript> scripts = scriptRepository.listByTenant(tenantId);
        List<DigitalHumanSchedule> schedules = scheduleRepository.listByTenant(tenantId);
        List<DigitalHumanSession> sessions = sessionRepository.listByTenant(tenantId);
        long liveCount = sessions.stream().filter(s -> "live".equals(s.liveStatus())).count();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BigDecimal todayGmv = sessions.stream()
                .filter(s -> s.startedAt() != null && s.startedAt().toLocalDate().equals(today))
                .map(DigitalHumanSession::gmvAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long todayOrders = sessions.stream()
                .filter(s -> s.startedAt() != null && s.startedAt().toLocalDate().equals(today))
                .mapToLong(DigitalHumanSession::orderCount)
                .sum();
        return new DigitalHumanOverviewView(
                avatars.size(),
                scripts.size(),
                schedules.size(),
                liveCount,
                sessions.size(),
                todayGmv,
                todayOrders
        );
    }

    // ---------- 辅助 ----------

    private DigitalHumanAvatar requireAvatar(String tenantId, String avatarId) {
        return avatarRepository.findById(tenantId, avatarId)
                .orElseThrow(() -> new BusinessException("2002", "avatar not found: " + avatarId, HttpStatus.NOT_FOUND));
    }

    private DigitalHumanScript requireScript(String tenantId, String scriptId) {
        return scriptRepository.findById(tenantId, scriptId)
                .orElseThrow(() -> new BusinessException("2003", "script not found: " + scriptId, HttpStatus.NOT_FOUND));
    }

    private DigitalHumanSchedule requireSchedule(String tenantId, String scheduleId) {
        return scheduleRepository.findById(tenantId, scheduleId)
                .orElseThrow(() -> new BusinessException("2004", "schedule not found: " + scheduleId, HttpStatus.NOT_FOUND));
    }

    private DigitalHumanSession requireSession(String tenantId, String sessionId) {
        return sessionRepository.findById(tenantId, sessionId)
                .orElseThrow(() -> new BusinessException("2005", "session not found: " + sessionId, HttpStatus.NOT_FOUND));
    }

    // ---------- 请求与视图 ----------

    public record CreateAvatarRequest(
            @NotBlank String organizationId,
            @NotBlank String avatarName,
            @NotBlank String avatarType,
            String voiceName,
            String previewUrl,
            Boolean enabled
    ) {
    }

    public record CreateScriptRequest(
            @NotBlank String organizationId,
            @NotBlank String scriptName,
            @NotBlank String sceneType,
            @NotBlank String contentText,
            String version,
            String status
    ) {
    }

    public record CreateScheduleRequest(
            @NotBlank String organizationId,
            @NotBlank String scheduleName,
            @NotBlank String avatarId,
            @NotBlank String scriptId,
            @NotBlank String platformType,
            @NotBlank String repeatType,
            @NotNull OffsetDateTime startAt,
            OffsetDateTime endAt,
            Boolean enabled
    ) {
    }

    public record DigitalHumanOverviewView(
            long avatarCount,
            long scriptCount,
            long scheduleCount,
            long liveSessionCount,
            long totalSessionCount,
            BigDecimal todayGmv,
            long todayOrders
    ) {
    }
}
