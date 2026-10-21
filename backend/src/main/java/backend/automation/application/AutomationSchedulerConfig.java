package backend.automation.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用定时调度（主类 DianShangPingTaiApplication 未开启 @EnableScheduling，在此开启）。
 */
@Configuration
@EnableScheduling
public class AutomationSchedulerConfig {
}
