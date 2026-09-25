-- Datos de demostracion para los tres requisitos funcionales de Piedrazul.
-- Es seguro ejecutarlo varias veces: busca registros por DNI, nombre o fecha.

BEGIN;

DO $$
DECLARE
    v_paciente_id INTEGER;
    v_medico_id INTEGER;
    v_terapeuta_id INTEGER;
    medicina_id INTEGER;
    fisioterapia_id INTEGER;
    disponibilidad_id INTEGER;
BEGIN
    SELECT per_id INTO v_paciente_id
    FROM persona
    WHERE per_dni = 10000001;

    IF v_paciente_id IS NULL THEN
        INSERT INTO persona (
            per_primer_nombre, per_segundo_nombre, per_primer_apellido,
            per_segundo_apellido, per_genero, per_fecha_nac,
            per_telefono, per_dni, per_correo
        ) VALUES (
            'Laura', NULL, 'Gomez', 'Rios', 'MUJER',
            '1995-04-12', '3000000001', 10000001,
            'laura.gomez@demo.piedrazul'
        ) RETURNING per_id INTO v_paciente_id;
    END IF;

    INSERT INTO paciente (per_id)
    VALUES (v_paciente_id)
    ON CONFLICT (per_id) DO NOTHING;

    UPDATE usuario
    SET per_id = v_paciente_id
    WHERE username = 'paciente.prueba'
      AND per_id IS NULL;

    INSERT INTO especialidad (esp_nombre)
    VALUES ('Medicina General')
    ON CONFLICT (esp_nombre) DO NOTHING;

    INSERT INTO especialidad (esp_nombre)
    VALUES ('Fisioterapia')
    ON CONFLICT (esp_nombre) DO NOTHING;

    SELECT esp_id INTO medicina_id
    FROM especialidad
    WHERE esp_nombre = 'Medicina General';

    SELECT esp_id INTO fisioterapia_id
    FROM especialidad
    WHERE esp_nombre = 'Fisioterapia';

     SELECT p.per_id INTO v_medico_id
     FROM persona p
     JOIN medico m ON m.per_id = p.per_id
     WHERE p.per_dni = 90000001
         OR (p.per_primer_nombre = 'Valentina' AND p.per_primer_apellido = 'Pabon')
     ORDER BY CASE WHEN p.per_dni = 90000001 THEN 0 ELSE 1 END, p.per_id
     LIMIT 1;

    IF v_medico_id IS NULL THEN
        INSERT INTO persona (
            per_primer_nombre, per_segundo_nombre, per_primer_apellido,
            per_segundo_apellido, per_genero, per_fecha_nac,
            per_telefono, per_dni, per_correo
        ) VALUES (
            'Valentina', NULL, 'Pabon', NULL, 'MUJER',
            '1988-08-20', '3000000002', 90000001,
            'valentina.pabon@demo.piedrazul'
        ) RETURNING per_id INTO v_medico_id;
    END IF;

    INSERT INTO medico (per_id, med_tipo_profesional, med_estado)
    VALUES (v_medico_id, 'MEDICO', 'ACTIVO')
    ON CONFLICT (per_id) DO UPDATE
    SET med_tipo_profesional = EXCLUDED.med_tipo_profesional,
        med_estado = EXCLUDED.med_estado;

    INSERT INTO medicoespecialidad (per_id, esp_id)
    VALUES (v_medico_id, medicina_id)
    ON CONFLICT DO NOTHING;

     SELECT p.per_id INTO v_terapeuta_id
     FROM persona p
     JOIN medico m ON m.per_id = p.per_id
     WHERE p.per_dni = 90000002
         OR (p.per_primer_nombre = 'Carlos' AND p.per_primer_apellido = 'Mora')
     ORDER BY CASE WHEN p.per_dni = 90000002 THEN 0 ELSE 1 END, p.per_id
     LIMIT 1;

    IF v_terapeuta_id IS NULL THEN
        INSERT INTO persona (
            per_primer_nombre, per_segundo_nombre, per_primer_apellido,
            per_segundo_apellido, per_genero, per_fecha_nac,
            per_telefono, per_dni, per_correo
        ) VALUES (
            'Carlos', NULL, 'Mora', NULL, 'HOMBRE',
            '1985-02-15', '3000000003', 90000002,
            'carlos.mora@demo.piedrazul'
        ) RETURNING per_id INTO v_terapeuta_id;
    END IF;

    INSERT INTO medico (per_id, med_tipo_profesional, med_estado)
    VALUES (v_terapeuta_id, 'TERAPISTA', 'ACTIVO')
    ON CONFLICT (per_id) DO UPDATE
    SET med_tipo_profesional = EXCLUDED.med_tipo_profesional,
        med_estado = EXCLUDED.med_estado;

    INSERT INTO medicoespecialidad (per_id, esp_id)
    VALUES (v_terapeuta_id, fisioterapia_id)
    ON CONFLICT DO NOTHING;

    disponibilidad_id := NULL;
    INSERT INTO disponibilidad (
        disp_dia_semana, disp_hora_inicio, disp_hora_fin, disp_intervalo
    )
    SELECT 'LUNES', '08:00', '17:00', 30
    WHERE NOT EXISTS (
        SELECT 1 FROM disponibilidad
        WHERE disp_dia_semana = 'LUNES'
          AND disp_hora_inicio = '08:00'
          AND disp_hora_fin = '17:00'
          AND disp_intervalo = 30
    )
    RETURNING disp_id INTO disponibilidad_id;

    IF disponibilidad_id IS NULL THEN
        SELECT disp_id INTO disponibilidad_id
        FROM disponibilidad
        WHERE disp_dia_semana = 'LUNES'
          AND disp_hora_inicio = '08:00'
          AND disp_hora_fin = '17:00'
          AND disp_intervalo = 30
        LIMIT 1;
    END IF;

    INSERT INTO medicodisponibilidad (per_id, disp_id)
    VALUES (v_medico_id, disponibilidad_id)
    ON CONFLICT DO NOTHING;

    disponibilidad_id := NULL;
    INSERT INTO disponibilidad (
        disp_dia_semana, disp_hora_inicio, disp_hora_fin, disp_intervalo
    )
    SELECT 'MARTES', '09:00', '16:00', 45
    WHERE NOT EXISTS (
        SELECT 1 FROM disponibilidad
        WHERE disp_dia_semana = 'MARTES'
          AND disp_hora_inicio = '09:00'
          AND disp_hora_fin = '16:00'
          AND disp_intervalo = 45
    )
    RETURNING disp_id INTO disponibilidad_id;

    IF disponibilidad_id IS NULL THEN
        SELECT disp_id INTO disponibilidad_id
        FROM disponibilidad
        WHERE disp_dia_semana = 'MARTES'
          AND disp_hora_inicio = '09:00'
          AND disp_hora_fin = '16:00'
          AND disp_intervalo = 45
        LIMIT 1;
    END IF;

    INSERT INTO medicodisponibilidad (per_id, disp_id)
    VALUES (v_terapeuta_id, disponibilidad_id)
    ON CONFLICT DO NOTHING;

    disponibilidad_id := NULL;
    INSERT INTO disponibilidad (
        disp_dia_semana, disp_hora_inicio, disp_hora_fin, disp_intervalo
    )
    SELECT 'MIERCOLES', '08:00', '12:00', 30
    WHERE NOT EXISTS (
        SELECT 1 FROM disponibilidad
        WHERE disp_dia_semana = 'MIERCOLES'
          AND disp_hora_inicio = '08:00'
          AND disp_hora_fin = '12:00'
          AND disp_intervalo = 30
    )
    RETURNING disp_id INTO disponibilidad_id;

    IF disponibilidad_id IS NULL THEN
        SELECT disp_id INTO disponibilidad_id
        FROM disponibilidad
        WHERE disp_dia_semana = 'MIERCOLES'
          AND disp_hora_inicio = '08:00'
          AND disp_hora_fin = '12:00'
          AND disp_intervalo = 30
        LIMIT 1;
    END IF;

    INSERT INTO medicodisponibilidad (per_id, disp_id)
    VALUES (v_medico_id, disponibilidad_id)
    ON CONFLICT DO NOTHING;

    INSERT INTO configuracionsistema (semanas_agendamiento, activo)
    SELECT 4, TRUE
    WHERE NOT EXISTS (SELECT 1 FROM configuracionsistema);

    UPDATE configuracionsistema
    SET semanas_agendamiento = 4,
        activo = TRUE;

    INSERT INTO cita (
        usu_id, paciente_id, medico_id, fecha_hora_cita,
        cita_estado, observacion
    )
    SELECT u.usu_id, v_paciente_id, v_medico_id, '2026-09-28 08:00:00+00',
           'PROGRAMADA', 'Control de medicina general'
    FROM usuario u
    WHERE u.username = 'paciente.prueba'
      AND NOT EXISTS (
          SELECT 1 FROM cita
                      WHERE cita.medico_id = v_medico_id
                        AND cita.fecha_hora_cita = '2026-09-28 08:00:00+00'
      );

    INSERT INTO cita (
        usu_id, paciente_id, medico_id, fecha_hora_cita,
        cita_estado, observacion
    )
    SELECT u.usu_id, v_paciente_id, v_terapeuta_id, '2026-09-29 09:00:00+00',
           'CONFIRMADA', 'Sesión inicial de fisioterapia'
    FROM usuario u
    WHERE u.username = 'paciente.prueba'
      AND NOT EXISTS (
          SELECT 1 FROM cita
                      WHERE cita.medico_id = v_terapeuta_id
                        AND cita.fecha_hora_cita = '2026-09-29 09:00:00+00'
      );
END $$;

COMMIT;