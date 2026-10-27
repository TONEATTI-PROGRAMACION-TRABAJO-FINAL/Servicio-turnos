package com.toneatti.turnos.catedra.domain.exception;

/**
 * The cátedra rejected the request with a 4xx and a functional {@code code} (section 13 of the integration contract).
 * Callers decide by {@link #getStatus()} and {@link #getCode()}, never by the message.
 */
public class CatedraRequestRejectedException extends RuntimeException {

    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";
    public static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    public static final String PROFESSIONAL_DISABLED = "PROFESSIONAL_DISABLED";
    public static final String INVALID_SLOT = "INVALID_SLOT";
    public static final String SLOT_ALREADY_RESERVED = "SLOT_ALREADY_RESERVED";
    public static final String SLOT_ALREADY_HELD = "SLOT_ALREADY_HELD";

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
