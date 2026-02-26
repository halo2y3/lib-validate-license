package co.com.validate.license.telegram.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotSession {

    public enum Estado {
        IDLE,
        ESPERANDO_EMAIL,
        ESPERANDO_VALID_DAYS,
        ESPERANDO_CHAT_ID_AGREGAR,
        ESPERANDO_CHAT_ID_ELIMINAR
    }

    private Estado estado = Estado.IDLE;
    private String licenseKey;
    private String email;
}
