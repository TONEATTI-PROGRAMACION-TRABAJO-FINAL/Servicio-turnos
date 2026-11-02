package com.toneatti.turnos.catedra.domain.ports.out;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentCancellation;
import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
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

    /**
     * Reservations of the patient with a date between both dates, inclusive, in any status; empty when there are
     * none. Used to find out whether a confirmation whose answer was lost reached the cátedra.
     */
    List<Appointment> findAppointments(String externalPatientId, LocalDate from, LocalDate to);

    /**
     * Page of the reservations of the patient (section 11), most recent slot first, optionally filtered by
     * {@code status} and/or a date range; {@code status}, {@code from} and {@code to} null mean no filter on them.
     * {@code page} is 0-based; the total count is independent of the page.
     */
    AppointmentPage findAppointmentsPage(String externalPatientId, LocalDate from, LocalDate to,
                                         AppointmentStatus status, int page, int size);

    /**
     * Cancels the CONFIRMED reservation of the process (section 12); {@code reason} may be null. Cancelling a
     * reservation already CANCELLED is idempotent and answers its current state. A
     * {@link CatedraUnavailableException} does not prove that the reservation was not cancelled (section 18.4).
     */
    AppointmentCancellation cancelAppointment(String reservationProcessId, String reason);

}
