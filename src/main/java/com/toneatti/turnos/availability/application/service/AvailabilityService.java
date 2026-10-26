package com.toneatti.turnos.availability.application.service;

import com.toneatti.turnos.availability.application.exception.AvailabilityException;
import com.toneatti.turnos.availability.domain.model.AvailableSlot;
import com.toneatti.turnos.availability.domain.ports.in.GetAvailableSlotsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final GetAvailableSlotsUseCase getAvailableSlotsUseCase;

    public List<AvailableSlot> getAvailableSlots(Long professionalId, LocalDate from, LocalDate to) {
        return getAvailableSlotsUseCase.getAvailableSlots(professionalId, from, to)
                .orElseThrow(() -> new AvailabilityException("Could not find professional with id: " + professionalId));
    }

}
