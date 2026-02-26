package co.com.validate.license.telegram.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import co.com.validate.license.telegram.model.TelegramAuthorizedUser;

public interface TelegramAuthorizedUserRepository extends JpaRepository<TelegramAuthorizedUser, Long> {

    boolean existsByChatId(Long chatId);

    Optional<TelegramAuthorizedUser> findByChatId(Long chatId);

    @Transactional
    void deleteByChatId(Long chatId);
}
