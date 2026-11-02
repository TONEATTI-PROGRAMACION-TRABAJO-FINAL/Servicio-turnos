package com.toneatti.turnos.catedra.domain.exception;

/**
 * The cátedra rejected the request with a 4xx and a functional {@code code} (section 13 of the integration contract).
 * Callers decide by {@link #getStatus()} and {@link #getCode()}, never by the message.
 */
public class CatedraRequestRejectedException extends RuntimeException {

    public static final String STUDENT_INTEGRATION_NOT_FOUND = "STUDENT_INTEGRATION_NOT_FOUND";
    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    public static final String PROFESSIONAL_DISABLED = "PROFESSIONAL_DISABLED";
    public static final String INVALID_SLOT = "INVALID_SLOT";
    public static final String SLOT_ALREADY_RESERVED = "SLOT_ALREADY_RESERVED";
    public static final String SLOT_ALREADY_HELD = "SLOT_ALREADY_HELD";
    public static final String HOLD_NOT_FOUND = "HOLD_NOT_FOUND";
    public static final String HOLD_EXPIRED = "HOLD_EXPIRED";
    public static final String RESERVATION_PROCESS_ALREADY_CONFIRMED = "RESERVATION_PROCESS_ALREADY_CONFIRMED";
    public static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    public static final String RESERVATION_OWNER_MISMATCH = "RESERVATION_OWNER_MISMATCH";
    public static final String RESERVATION_NOT_CANCELLABLE = "RESERVATION_NOT_CANCELLABLE";

    private final int status;
    private final String code;

    public CatedraRequestRejectedException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
