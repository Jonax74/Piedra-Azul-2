package com.piedrazul.citas.domain.model;

import com.piedrazul.citas.domain.valueobjects.MedicoId;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class DisponibilidadSnapshot {
    private final MedicoId medicoId;
    private final Map<DayOfWeek, List<TimeRange>> horariosSemanales;
    private final Set<LocalDateTime> bloqueosEspecificos;
    private final LocalDateTime actualizadoEn;
    private Integer intervaloMinutos;

    public DisponibilidadSnapshot(MedicoId medicoId, Integer intervaloMinutos) {
        this.medicoId = medicoId;
        this.intervaloMinutos = intervaloMinutos;
        this.horariosSemanales = new HashMap<>();
        this.bloqueosEspecificos = new HashSet<>();
        this.actualizadoEn = LocalDateTime.now();
    }

    public Integer getIntervaloMinutos() {
        return intervaloMinutos;
    }

    public void setIntervaloMinutos(Integer intervaloMinutos) {
        this.intervaloMinutos = intervaloMinutos;
    }

    public void agregarHorarioSemanal(DayOfWeek dia, TimeRange rango) {
        horariosSemanales.computeIfAbsent(dia, k -> new ArrayList<>()).add(rango);
    }

    public void agregarBloqueo(LocalDateTime fechaHora) {
        bloqueosEspecificos.add(fechaHora);
    }

    public void removerBloqueo(LocalDateTime fechaHora) {
        bloqueosEspecificos.remove(fechaHora);
    }

    public boolean estaDisponible(MedicoId medicoId, LocalDateTime fechaHora) {

        if (!this.medicoId.equals(medicoId)) {
            return false;
        }

        // normalizar segundos y nanos
        fechaHora = fechaHora.withSecond(0).withNano(0);

        // bloqueo específico
        LocalDateTime finalFechaHora = fechaHora;
        if (bloqueosEspecificos.stream()
                .map(b -> b.withSecond(0).withNano(0))
                .anyMatch(b -> b.equals(finalFechaHora))) {
            return false;
        }

        DayOfWeek dia = fechaHora.getDayOfWeek();

        List<TimeRange> rangos = horariosSemanales.get(dia);

        if (rangos == null || rangos.isEmpty()) {
            return false;
        }

        LocalTime hora = fechaHora.toLocalTime();

        for (TimeRange rango : rangos) {

            LocalTime actual = rango.getStart();

            while (actual.isBefore(rango.getEnd())) {

                if (actual.equals(hora)) {
                    return true;
                }

                actual = actual.plusMinutes(intervaloMinutos);
            }
        }

        return false;
    }

    public void reemplazarHorariosDelDia(DayOfWeek dia, List<TimeRange> nuevosRangos) {
        horariosSemanales.put(dia, new ArrayList<>(nuevosRangos));
    }

    public void agregarHorarioSemanalSinDuplicados(DayOfWeek dia, TimeRange nuevo) {
        List<TimeRange> existentes = horariosSemanales
                .computeIfAbsent(dia, k -> new ArrayList<>());

        boolean yaExiste = existentes.stream()
                .anyMatch(r -> r.getStart().equals(nuevo.getStart())
                        && r.getEnd().equals(nuevo.getEnd()));

        if (!yaExiste) {
            existentes.add(nuevo);
        }
    }

    public void removerHorarioSemanal(DayOfWeek dia, TimeRange rango) {
        List<TimeRange> existentes = horariosSemanales.get(dia);
        if (existentes == null) {
            return;
        }

        existentes.removeIf(r -> r.getStart().equals(rango.getStart())
                && r.getEnd().equals(rango.getEnd()));

        if (existentes.isEmpty()) {
            horariosSemanales.remove(dia);
        }
    }

    public void reemplazarHorarioSemanal(DayOfWeek dia, TimeRange anterior, TimeRange nuevo) {
        removerHorarioSemanal(dia, anterior);
        agregarHorarioSemanalSinDuplicados(dia, nuevo);
    }

    public boolean esSlotValido(LocalDateTime fechaHora) {
        if (!estaDisponible(this.medicoId, fechaHora)) {
            return false;
        }

        int intervalo = this.intervaloMinutos;

        return fechaHora.getMinute() % intervalo == 0;
    }

    // Getters
    public MedicoId getMedicoId() { return medicoId; }
    public Map<DayOfWeek, List<TimeRange>> getHorariosSemanales() { return horariosSemanales; }
    public Set<LocalDateTime> getBloqueosEspecificos() { return bloqueosEspecificos; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}