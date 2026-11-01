package com.toneatti.turnos.catedra.infrastructure.rest.mapper;

import com.toneatti.turnos.catedra.domain.model.Appointment;
import com.toneatti.turnos.catedra.domain.model.AppointmentSummary;
import com.toneatti.turnos.catedra.domain.model.Hold;
import com.toneatti.turnos.catedra.domain.model.HoldConfirmation;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraAppointmentListResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraAppointmentResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldConfirmationResponse;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraHoldResponse;
import com.toneatti.turnos.catedra.infrastructure.rest.dto.CatedraOccupancyResponse;
import org.springframework.stereotype.Component;

@Component
public class CatedraRestMapper {

    public Occupancy toDomainModel(CatedraOccupancyResponse response) {
        if (response == null) {
            return null;
        }
        return new Occupancy(
                response.date(),
                response.startTime(),
                response.status(),
                response.expiresAt());
    }

    public Hold toDomainModel(CatedraHoldResponse response) {
        if (response == null) {
            return null;
        }
        return new Hold(
                response.holdId(),
                response.reservationProcessId(),
                response.expiresAt());
    }

    public HoldConfirmation toDomainModel(CatedraHoldConfirmationResponse response) {
        if (response == null) {
            return null;
        }
        return new HoldConfirmation(
                response.reservationProcessId(),
                response.expiresAt());
    }

    public Appointment toDomainModel(CatedraAppointmentResponse response) {
        if (response == null) {
            return null;
        }
        return new Appointment(
                response.reservationId(),
                response.reservationProcessId(),
                response.externalPatientId(),
                response.professionalId(),
                response.appointmentDate(),
                response.startTime(),
                response.status());
    }

    public AppointmentSummary toDomainModel(CatedraAppointmentListResponse response) {
        if (response == null) {
            return null;
        }
        return new AppointmentSummary(
                response.reservationId(),
                response.reservationProcessId(),
                response.externalPatientId(),
                response.professionalId(),
                response.professionalFirstName(),
                response.professionalLastName(),
                response.appointmentDate(),
                response.startTime(),
                response.endTime(),
                response.patientFirstName(),
                response.patientLastName(),
                response.patientPhone(),
                response.status(),
                response.createdAt(),
                response.confirmedAt(),
                response.cancelledAt(),
                response.cancellationReason());
    }
}
