package com.piedrazul.notifications.infrastructure.persistence.repository;

import com.piedrazul.notifications.infrastructure.persistence.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataNotificacionRepository extends JpaRepository<NotificacionEntity, String> {

    List<NotificacionEntity> findByPersonaIdOrderByFechaCreacionDesc(Long personaId);

    List<NotificacionEntity> findByPersonaIdAndLeidaOrderByFechaCreacionDesc(Long personaId, boolean leida);

    long countByPersonaIdAndLeida(Long personaId, boolean leida);

    Optional<NotificacionEntity> findByIdAndPersonaId(String id, Long personaId);

    List<NotificacionEntity> findByPersonaIdAndLeida(Long personaId, boolean leida);
}
