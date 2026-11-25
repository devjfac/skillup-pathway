package co.edu.sena.skillup.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Reloj de pruebas que permite avanzar el tiempo manualmente.
 */
class RelojAjustable extends Clock {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    private Instant instanteActual;

    RelojAjustable(Instant instanteInicial) {
        this.instanteActual = instanteInicial;
    }

    void avanzar(Duration duracion) {
        instanteActual = instanteActual.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZONA_BOGOTA;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.fixed(instanteActual, zona);
    }

    @Override
    public Instant instant() {
        return instanteActual;
    }
}
