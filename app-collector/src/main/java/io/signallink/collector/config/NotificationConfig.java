package io.signallink.collector.config;

import io.signallink.common.notify.DiscordNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 배치 실패 알림용 Discord 웹훅 빈. webhook-url이 비면 no-op(로컬). */
@Configuration
public class NotificationConfig {

    @Bean
    public DiscordNotifier discordNotifier(
            @Value("${signallink.discord.webhook-url:}") String webhookUrl) {
        return new DiscordNotifier(webhookUrl);
    }
}
