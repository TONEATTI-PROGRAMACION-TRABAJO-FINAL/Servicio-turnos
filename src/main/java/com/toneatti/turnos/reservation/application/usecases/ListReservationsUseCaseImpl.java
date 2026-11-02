package com.toneatti.turnos.reservation.application.usecases;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.AppointmentPage;
import com.toneatti.turnos.catedra.domain.model.AppointmentStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import com.toneatti.turnos.reservation.application.exception.InvalidDateRangeException;
import com.toneatti.turnos.reservation.application.exception.ReservationUnavailableException;
import com.toneatti.turnos.reservation.domain.ports.in.ListReservationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Reservations of the patient registered in the cátedra (section 11 of the integration contract): the filters and
 * the page pass straight through, and the patient is always the authenticated user's externalId, never a value the
 * app chooses.
 */
@Component
@RequiredArgsConstructor
public class ListReservationsUseCaseImpl implements ListReservationsUseCase {

    private final CatedraRepository catedraRepository;

    @Override
    public AppointmentPage listReservations(String userExternalId, LocalDate from, LocalDate to,
                                             AppointmentStatus status, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException(from, to);
        }
        try {
            return catedraRepository.findAppointmentsPage(userExternalId, from, to, status, page, size);
        } catch (CatedraRequestRejectedException e) {
            if (CatedraRequestRejectedException.INVALID_DATE_RANGE.equals(e.getCode())) {
                throw new InvalidDateRangeException(from, to);
            }
            // Includes STUDENT_INTEGRATION_NOT_FOUND: a configuration problem of the technical account, not the
            // user's, so it surfaces as unavailable instead of a rejection (section 13.1), the same as T6.
            throw new ReservationUnavailableException("Catedra rejected the appointments with " + e.getCode(), e);
        } catch (CatedraUnavailableException e) {
            throw new ReservationUnavailableException("Could not list the appointments", e);
        }
    }
}
