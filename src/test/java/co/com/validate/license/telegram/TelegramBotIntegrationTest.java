package co.com.validate.license.telegram;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TelegramBotIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void telegramBotService_notRegistered_whenDisabled() {
        assertFalse(applicationContext.containsBean("telegramBotService"),
                "TelegramBotService should NOT be registered when telegram.bot.enabled=false");
    }
}
