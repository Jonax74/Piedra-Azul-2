package com.piedrazul.citas.infrastructure.persistence.repository;

import com.piedrazul.citas.infrastructure.persistence.entity.ConfiguracionSistemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataConfiguracionRepository
        extends JpaRepository<ConfiguracionSistemaEntity, Long> {
}