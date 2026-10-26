package com.toneatti.turnos.availability.domain.ports.in;

import com.toneatti.turnos.availability.domain.model.AvailableSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GetAvailableSlotsUseCase {
    /**
     * Free slots of the professional between both dates, inclusive, ordered by date and start time;
     * empty when the professional does not exist.
     */
    Optional<List<AvailableSlot>> getAvailableSlots(Long professionalId, LocalDate from, LocalDate to);
}
