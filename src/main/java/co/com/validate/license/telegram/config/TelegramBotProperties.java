package co.com.validate.license.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter
@Setter
public class TelegramBotProperties {

    private boolean enabled = false;
    private String token = "";
    private String username = "";
    private Long adminChatId = 0L;
}
