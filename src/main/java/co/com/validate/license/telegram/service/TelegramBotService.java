package co.com.validate.license.telegram.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import co.com.validate.license.model.License;
import co.com.validate.license.repository.LicenseRepository;
import co.com.validate.license.service.EmailService;
import co.com.validate.license.telegram.config.TelegramBotProperties;
import co.com.validate.license.telegram.model.BotSession;
import co.com.validate.license.telegram.model.TelegramAuthorizedUser;
import co.com.validate.license.telegram.repository.TelegramAuthorizedUserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramBotProperties botProperties;
    private final TelegramAuthorizedUserRepository authorizedUserRepository;
    private final LicenseRepository licenseRepository;
    private final EmailService emailService;
    private final TelegramClient telegramClient;

    private final ConcurrentHashMap<Long, BotSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public TelegramBotService(TelegramBotProperties botProperties,
                               TelegramAuthorizedUserRepository authorizedUserRepository,
                               LicenseRepository licenseRepository,
                               EmailService emailService) {
        this.botProperties = botProperties;
        this.authorizedUserRepository = authorizedUserRepository;
        this.licenseRepository = licenseRepository;
        this.emailService = emailService;
        this.telegramClient = new OkHttpTelegramClient(botProperties.getToken());
    }

    // Package-private constructor for testing — accepts injected TelegramClient
    TelegramBotService(TelegramBotProperties botProperties,
                       TelegramAuthorizedUserRepository authorizedUserRepository,
                       LicenseRepository licenseRepository,
                       EmailService emailService,
                       TelegramClient telegramClient) {
        this.botProperties = botProperties;
        this.authorizedUserRepository = authorizedUserRepository;
        this.licenseRepository = licenseRepository;
        this.emailService = emailService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingSingleThreadUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        String username = message.getFrom() != null ? message.getFrom().getUserName() : null;

        BotSession session = sessions.computeIfAbsent(chatId, id -> new BotSession());

        if (text.startsWith("/")) {
            handleCommand(chatId, username, text, session);
        } else {
            handleInput(chatId, text, session);
        }
    }

    private boolean isAdmin(Long chatId) {
        Long adminId = botProperties.getAdminChatId();
        return adminId != null && adminId != 0L && chatId.equals(adminId);
    }

    private boolean isAuthorized(Long chatId) {
        return isAdmin(chatId) || authorizedUserRepository.existsByChatId(chatId);
    }

    private void handleCommand(Long chatId, String username, String text, BotSession session) {
        String command = text.split("\\s+")[0].toLowerCase();

        switch (command) {
            case "/start" -> handleStart(chatId);
            case "/ayuda" -> handleAyuda(chatId);
            case "/miinfo" -> handleMiInfo(chatId);
            case "/crear" -> handleCrear(chatId, session);
            case "/cancelar" -> handleCancelar(chatId, session);
            case "/usuarios" -> handleUsuarios(chatId);
            case "/agregar" -> handleAgregar(chatId, session);
            case "/eliminar" -> handleEliminar(chatId, session);
            default -> sendMessage(chatId, "❓ Comando no reconocido. Usa /ayuda para ver los comandos disponibles.");
        }
    }

    private void handleStart(Long chatId) {
        boolean authorized = isAuthorized(chatId);
        String status = authorized
                ? "✅ Estás autorizado para crear licencias."
                : "🚫 No estás autorizado. Contacta al administrador.";
        sendMessage(chatId, "👋 ¡Bienvenido al Bot de Licencias!\n" + status);
    }

    private void handleAyuda(Long chatId) {
        StringBuilder sb = new StringBuilder("📋 Comandos disponibles:\n\n");
        sb.append("👋 /start — Bienvenida\n");
        sb.append("❓ /ayuda — Mostrar esta ayuda\n");
        sb.append("🪪 /miinfo — Ver tu chatId\n");

        if (isAuthorized(chatId)) {
            sb.append("🔑 /crear — Crear una licencia nueva\n");
            sb.append("❌ /cancelar — Cancelar la operación en curso\n");
        }

        if (isAdmin(chatId)) {
            sb.append("👥 /usuarios — Listar usuarios autorizados\n");
            sb.append("➕ /agregar — Autorizar un nuevo usuario\n");
            sb.append("🗑 /eliminar — Desautorizar un usuario\n");
        }

        sendMessage(chatId, sb.toString());
    }

    private void handleMiInfo(Long chatId) {
        sendMessage(chatId, "🪪 Tu chatId es: " + chatId);
    }

    private void handleCrear(Long chatId, BotSession session) {
        if (!isAuthorized(chatId)) {
            sendMessage(chatId, "🚫 No estás autorizado para crear licencias.");
            return;
        }
        String generatedKey = UUID.randomUUID().toString().toUpperCase();
        session.setLicenseKey(generatedKey);
        session.setEstado(BotSession.Estado.ESPERANDO_EMAIL);
        sendMessage(chatId, "📧 Ingresa el email del cliente:");
    }

    private void handleCancelar(Long chatId, BotSession session) {
        session.setEstado(BotSession.Estado.IDLE);
        session.setLicenseKey(null);
        session.setEmail(null);
        sendMessage(chatId, "❌ Operación cancelada.");
    }

    private void handleUsuarios(Long chatId) {
        if (!isAdmin(chatId)) {
            sendMessage(chatId, "🚫 No tienes permisos para este comando.");
            return;
        }

        List<TelegramAuthorizedUser> users = authorizedUserRepository.findAll();
        if (users.isEmpty()) {
            sendMessage(chatId, "👥 No hay usuarios autorizados.");
            return;
        }

        StringBuilder sb = new StringBuilder("👥 Usuarios autorizados:\n\n");
        for (TelegramAuthorizedUser user : users) {
            sb.append("• ChatId: ").append(user.getChatId());
            if (user.getUsername() != null) {
                sb.append(" (@").append(user.getUsername()).append(")");
            }
            sb.append("\n");
        }

        sendMessage(chatId, sb.toString());
    }

    private void handleAgregar(Long chatId, BotSession session) {
        if (!isAdmin(chatId)) {
            sendMessage(chatId, "🚫 No tienes permisos para este comando.");
            return;
        }
        session.setEstado(BotSession.Estado.ESPERANDO_CHAT_ID_AGREGAR);
        sendMessage(chatId, "➕ Ingresa el chatId del usuario a autorizar:");
    }

    private void handleEliminar(Long chatId, BotSession session) {
        if (!isAdmin(chatId)) {
            sendMessage(chatId, "🚫 No tienes permisos para este comando.");
            return;
        }
        session.setEstado(BotSession.Estado.ESPERANDO_CHAT_ID_ELIMINAR);
        sendMessage(chatId, "➖ Ingresa el chatId del usuario a desautorizar:");
    }

    private void handleInput(Long chatId, String text, BotSession session) {
        switch (session.getEstado()) {
            case ESPERANDO_EMAIL -> handleEmailInput(chatId, text, session);
            case ESPERANDO_VALID_DAYS -> handleValidDaysInput(chatId, text, session);
            case ESPERANDO_CHAT_ID_AGREGAR -> handleChatIdAgregar(chatId, text, session);
            case ESPERANDO_CHAT_ID_ELIMINAR -> handleChatIdEliminar(chatId, text, session);
            default -> sendMessage(chatId, "❓ Usa /ayuda para ver los comandos disponibles.");
        }
    }

    private void handleEmailInput(Long chatId, String text, BotSession session) {
        if (!text.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            sendMessage(chatId, "❌ Email inválido. Ingresa un email válido:");
            return;
        }

        session.setEmail(text);
        session.setEstado(BotSession.Estado.ESPERANDO_VALID_DAYS);
        sendMessage(chatId, "✅ Email aceptado.\n📅 ¿Cuántos días de validez tendrá la licencia? (mínimo 1):");
    }

    private void handleValidDaysInput(Long chatId, String text, BotSession session) {
        int days;
        try {
            days = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Por favor ingresa un número entero válido:");
            return;
        }

        if (days < 1) {
            sendMessage(chatId, "⚠️ El número de días debe ser al menos 1. Intenta de nuevo:");
            return;
        }

        License license = new License();
        license.setLicenseKey(session.getLicenseKey());
        license.setEmail(session.getEmail());
        license.setExpirationDate(LocalDate.now().plusDays(days));
        license.setActive(false);

        licenseRepository.save(license);

        emailService.sendLicenseCreationEmail(
                license.getEmail(),
                license.getLicenseKey(),
                license.getExpirationDate()
        );

        String successMsg = String.format(
                "🎉 ¡Licencia creada exitosamente!\n\n🔑 Clave: %s\n📧 Email: %s\n📅 Válida hasta: %s",
                license.getLicenseKey(),
                license.getEmail(),
                license.getExpirationDate()
        );

        sendMessage(chatId, successMsg);

        session.setEstado(BotSession.Estado.IDLE);
        session.setLicenseKey(null);
        session.setEmail(null);
    }

    private void handleChatIdAgregar(Long chatId, String text, BotSession session) {
        Long targetChatId;
        try {
            targetChatId = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Por favor ingresa un chatId válido (número entero):");
            return;
        }

        if (authorizedUserRepository.existsByChatId(targetChatId)) {
            sendMessage(chatId, "⚠️ Ese usuario ya está autorizado.");
            session.setEstado(BotSession.Estado.IDLE);
            return;
        }

        TelegramAuthorizedUser user = new TelegramAuthorizedUser();
        user.setChatId(targetChatId);
        user.setAddedAt(LocalDateTime.now());
        user.setAddedByChatId(chatId);

        authorizedUserRepository.save(user);

        sendMessage(chatId, "✅ Usuario " + targetChatId + " autorizado exitosamente.");
        session.setEstado(BotSession.Estado.IDLE);
    }

    private void handleChatIdEliminar(Long chatId, String text, BotSession session) {
        Long targetChatId;
        try {
            targetChatId = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Por favor ingresa un chatId válido (número entero):");
            return;
        }

        if (!authorizedUserRepository.existsByChatId(targetChatId)) {
            sendMessage(chatId, "⚠️ Ese usuario no está en la lista de autorizados.");
            session.setEstado(BotSession.Estado.IDLE);
            return;
        }

        authorizedUserRepository.deleteByChatId(targetChatId);

        sendMessage(chatId, "🗑 Usuario " + targetChatId + " desautorizado exitosamente.");
        session.setEstado(BotSession.Estado.IDLE);
    }

    private void sendMessage(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Error al enviar mensaje a chatId {}: {}", chatId, e.getMessage(), e);
        }
    }
}
