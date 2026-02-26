package co.com.validate.license.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import co.com.validate.license.repository.LicenseRepository;
import co.com.validate.license.service.EmailService;
import co.com.validate.license.telegram.config.TelegramBotProperties;
import co.com.validate.license.telegram.model.TelegramAuthorizedUser;
import co.com.validate.license.telegram.repository.TelegramAuthorizedUserRepository;
import co.com.validate.license.telegram.service.TelegramBotService;

/**
 * Unit tests for TelegramBotService.
 * Uses reflection to invoke the package-private testing constructor.
 *
 * Flujo /crear (sin ingreso manual de clave):
 *   /crear  → msg[0]: "📧 Ingresa el email del cliente:"
 *   email   → msg[1]: "✅ Email aceptado.\n📅 ¿Cuántos meses..."
 *   meses   → msg[2]: "🎉 ¡Licencia creada exitosamente!\n\n🔑 Clave: {UUID}\n..."
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramBotServiceTest {

    @Mock
    private TelegramBotProperties botProperties;

    @Mock
    private TelegramAuthorizedUserRepository authorizedUserRepository;

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TelegramClient telegramClient;

    private TelegramBotService botService;

    private final List<String> sentMessages = new ArrayList<>();

    private static final Long ADMIN_CHAT_ID = 111L;
    private static final Long AUTHORIZED_CHAT_ID = 222L;
    private static final Long UNAUTHORIZED_CHAT_ID = 333L;

    @BeforeEach
    void setUp() throws Exception {
        when(botProperties.getToken()).thenReturn("test-token");
        when(botProperties.getAdminChatId()).thenReturn(ADMIN_CHAT_ID);

        var constructor = TelegramBotService.class.getDeclaredConstructor(
                TelegramBotProperties.class,
                TelegramAuthorizedUserRepository.class,
                LicenseRepository.class,
                EmailService.class,
                TelegramClient.class);
        constructor.setAccessible(true);
        botService = (TelegramBotService) constructor.newInstance(
                botProperties, authorizedUserRepository, licenseRepository, emailService, telegramClient);

        sentMessages.clear();
        when(telegramClient.execute(any(SendMessage.class))).thenAnswer(inv -> {
            sentMessages.add(((SendMessage) inv.getArgument(0)).getText());
            return null;
        });
    }

    private Update buildUpdate(Long chatId, String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getFrom()).thenReturn(user);
        when(user.getUserName()).thenReturn(null);

        return update;
    }

    // ---------- /miinfo ----------

    @Test
    void testMiInfo_sendsChatId() {
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/miinfo"));

        assertEquals(1, sentMessages.size());
        assertEquals("🪪 Tu chatId es: " + AUTHORIZED_CHAT_ID, sentMessages.get(0));
    }

    // ---------- /crear — unauthorized ----------

    @Test
    void testCrear_unauthorizedUser_rejected() {
        when(authorizedUserRepository.existsByChatId(UNAUTHORIZED_CHAT_ID)).thenReturn(false);

        botService.consume(buildUpdate(UNAUTHORIZED_CHAT_ID, "/crear"));

        assertEquals(1, sentMessages.size());
        assert sentMessages.get(0).contains("autorizado");
        verify(licenseRepository, never()).save(any());
    }

    // ---------- /crear — clave se genera automáticamente ----------

    @Test
    void testCrear_authorizedUser_generatesUUID_andPromptsEmail() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));

        assertEquals(1, sentMessages.size());
        String msg = sentMessages.get(0);
        assert !msg.contains("Clave") : "No debe mostrar la clave antes de crear la licencia";
        assert msg.contains("email") : "Debe pedir el email";
    }

    @Test
    void testCrear_admin_generatesUUID_andPromptsEmail() {
        botService.consume(buildUpdate(ADMIN_CHAT_ID, "/crear"));

        assertEquals(1, sentMessages.size());
        assert !sentMessages.get(0).contains("Clave generada:");
        assertFalse(sentMessages.get(0).contains("No estás autorizado"));
        assert sentMessages.get(0).contains("email");
    }

    // ---------- Email: invalid → stays in ESPERANDO_EMAIL ----------

    @Test
    void testEmail_invalido_permaneceEnEsperandoEmail() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "not-an-email"));

        // msg[0] = clave generada + pide email; msg[1] = error email
        assertEquals(2, sentMessages.size());
        assert sentMessages.get(1).contains("nválido");
        verify(licenseRepository, never()).save(any());
    }

    // ---------- Email: valid → moves to ESPERANDO_VALID_DAYS ----------

    @Test
    void testEmail_valido_pasaAEsperandoValidDays() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "user@example.com"));

        assertEquals(2, sentMessages.size());
        assert sentMessages.get(1).contains("meses");
        verify(licenseRepository, never()).save(any());
    }

    // ---------- Valid months: zero → stays in ESPERANDO_VALID_DAYS ----------

    @Test
    void testValidDays_cero_permaneceEnEsperandoValidDays() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "user@example.com"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "0"));

        assertEquals(3, sentMessages.size());
        assert sentMessages.get(2).contains("entre 1 y 12");
        verify(licenseRepository, never()).save(any());
    }

    // ---------- Valid days: non-integer → stays in ESPERANDO_VALID_DAYS ----------

    @Test
    void testValidDays_texto_permaneceEnEsperandoValidDays() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "user@example.com"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "not-a-number"));

        assertEquals(3, sentMessages.size());
        assert sentMessages.get(2).contains("número entero");
        verify(licenseRepository, never()).save(any());
    }

    // ---------- Full flow: creates license + sends email ----------

    @Test
    void testFullFlow_creaLicenciaConClaveGenerada_enviaEmail() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "user@example.com"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "1"));

        // Capturar la licencia guardada para obtener la clave generada
        var captor = org.mockito.ArgumentCaptor.forClass(co.com.validate.license.model.License.class);
        verify(licenseRepository).save(captor.capture());
        String generatedKey = captor.getValue().getLicenseKey();
        assertEquals("user@example.com", captor.getValue().getEmail());

        // Verificar que se envió el email con la clave generada
        verify(emailService).sendLicenseCreationEmail(
                org.mockito.ArgumentMatchers.eq("user@example.com"),
                org.mockito.ArgumentMatchers.eq(generatedKey),
                any());

        assertEquals(3, sentMessages.size());
        // La clave NO aparece en el primer mensaje
        assert !sentMessages.get(0).contains(generatedKey) : "La clave no debe mostrarse al inicio";
        // La clave SÍ aparece solo en el mensaje final
        assert sentMessages.get(2).contains("exitosamente");
        assert sentMessages.get(2).contains(generatedKey) : "La clave debe mostrarse en el mensaje final";
    }

    // ---------- /cancelar ----------

    @Test
    void testCancelar_resetSession_idleIgnoresText() {
        when(authorizedUserRepository.existsByChatId(AUTHORIZED_CHAT_ID)).thenReturn(true);

        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/crear"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/cancelar"));
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "algo")); // ignored in IDLE

        assert sentMessages.get(1).contains("cancelada");
        verify(licenseRepository, never()).save(any());
    }

    @Test
    void testCancelar_desdeIdle_respondeOk() {
        botService.consume(buildUpdate(AUTHORIZED_CHAT_ID, "/cancelar"));

        assertEquals(1, sentMessages.size());
        assert sentMessages.get(0).contains("cancelada");
    }

    // ---------- Admin /agregar ----------

    @Test
    void testAgregar_admin_guardaEnBD() {
        when(authorizedUserRepository.existsByChatId(500L)).thenReturn(false);

        botService.consume(buildUpdate(ADMIN_CHAT_ID, "/agregar"));
        botService.consume(buildUpdate(ADMIN_CHAT_ID, "500"));

        verify(authorizedUserRepository).save(argThat(u -> u.getChatId().equals(500L)));
        assert sentMessages.get(1).contains("autorizado exitosamente");
    }

    @Test
    void testAgregar_noAdmin_rechazado() {
        when(authorizedUserRepository.existsByChatId(UNAUTHORIZED_CHAT_ID)).thenReturn(false);

        botService.consume(buildUpdate(UNAUTHORIZED_CHAT_ID, "/agregar"));

        verify(authorizedUserRepository, never()).save(any(TelegramAuthorizedUser.class));
        assert sentMessages.get(0).contains("permisos");
    }

    // ---------- Admin /eliminar ----------

    @Test
    void testEliminar_admin_eliminaDeDB() {
        when(authorizedUserRepository.existsByChatId(500L)).thenReturn(true);

        botService.consume(buildUpdate(ADMIN_CHAT_ID, "/eliminar"));
        botService.consume(buildUpdate(ADMIN_CHAT_ID, "500"));

        verify(authorizedUserRepository).deleteByChatId(500L);
        assert sentMessages.get(1).contains("desautorizado exitosamente");
    }

    @Test
    void testEliminar_noAdmin_rechazado() {
        when(authorizedUserRepository.existsByChatId(UNAUTHORIZED_CHAT_ID)).thenReturn(false);

        botService.consume(buildUpdate(UNAUTHORIZED_CHAT_ID, "/eliminar"));

        verify(authorizedUserRepository, never()).deleteByChatId(any());
        assert sentMessages.get(0).contains("permisos");
    }

    // ---------- getBotToken / getUpdatesConsumer ----------

    @Test
    void testGetBotToken() {
        assertEquals("test-token", botService.getBotToken());
    }

    @Test
    void testGetUpdatesConsumer_returnsSelf() {
        assertEquals(botService, botService.getUpdatesConsumer());
    }

    // ---------- Update without message ----------

    @Test
    void testUpdateSinMensaje_ignorado() {
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(false);

        botService.consume(update);

        assertEquals(0, sentMessages.size());
    }
}
