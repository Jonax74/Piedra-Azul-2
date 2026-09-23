package com.piedrazul.citas.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de TimeRange")
class TimeRangeTest {

    @Test
    @DisplayName("Debe crear un rango de tiempo válido")
    void testCrearValido() {
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(17, 0);
        TimeRange timeRange = new TimeRange(start, end);

        assertNotNull(timeRange);
        assertEquals(start, timeRange.getStart());
        assertEquals(end, timeRange.getEnd());
    }

    @Test
    @DisplayName("Debe lanzar excepción si start es después de end")
    void testCrearConStartDespuesDeEnd() {
        LocalTime start = LocalTime.of(17, 0);
        LocalTime end = LocalTime.of(8, 0);

        assertThrows(IllegalArgumentException.class, () -> {
            new TimeRange(start, end);
        });
    }

    @Test
    @DisplayName("contiene() debe devolver true si la hora está dentro del rango")
    void testContieneHoraDentro() {
        TimeRange timeRange = new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertTrue(timeRange.contiene(LocalTime.of(9, 0)));
        assertTrue(timeRange.contiene(LocalTime.of(12, 0)));
        assertTrue(timeRange.contiene(LocalTime.of(16, 30)));
    }

    @Test
    @DisplayName("contiene() debe devolver false si la hora está fuera del rango")
    void testContieneHoraFuera() {
        TimeRange timeRange = new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertFalse(timeRange.contiene(LocalTime.of(7, 59)));
        assertFalse(timeRange.contiene(LocalTime.of(17, 1)));
        assertFalse(timeRange.contiene(LocalTime.of(20, 0)));
    }

    @Test
    @DisplayName("contiene() debe devolver true en los límites")
    void testContieneLimites() {
        TimeRange timeRange = new TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertTrue(timeRange.contiene(LocalTime.of(8, 0)));
        assertTrue(timeRange.contiene(LocalTime.of(17, 0)));
    }
}