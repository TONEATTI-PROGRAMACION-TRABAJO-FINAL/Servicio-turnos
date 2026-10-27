package com.toneatti.turnos.catedra.domain.ports.out;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.domain.model.Occupancy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Access to the cátedra REST API with the technical account of the project.
 * Every method throws {@link CatedraRequestRejectedException} when the cátedra rejects the request with a
 * functional code, and {@link CatedraUnavailableException} when it cannot answer.
 */
public interface CatedraRepository {

    /**
     * Confirmed reservations and active holds of the professional between both dates, inclusive;
     * empty when every slot is free.
     */
    List<Occupancy> findOccupancies(Long professionalId, LocalDate from, LocalDate to);

    /**
     * Holds the slot of the professional for the patient. A {@link CatedraUnavailableException} does not prove that
     * the hold was not created (section 18.4).
     */
    Hold createHold(Long professionalId, LocalDate date, LocalTime startTime, String externalPatientId);

    /**
     * Confirms the hold for the same patient that created it. A {@link CatedraUnavailableException} does not prove
     * that the hold was not confirmed (section 18.4).
     */
    HoldConfirmation confirmHold(String holdId, String externalPatientId,
                                 String patientFirstName, String patientLastName);

}
