package com.toneatti.turnos.reservation.application.exception;

/**
 * The cátedra rejected the reservation for a reason the user can act on; {@code code} is the one of the cátedra
 * (section 13.1 of the integration contract) and the process is FAILED.
 */
public class ReservationRejectedException extends RuntimeException {

    private final String code;

    public ReservationRejectedException(String code, Throwable cause) {
        super("Catedra rejected the reservation with " + code, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
