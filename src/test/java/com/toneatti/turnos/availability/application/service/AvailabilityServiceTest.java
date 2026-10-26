package com.toneatti.turnos.availability.application.service;

import com.toneatti.turnos.availability.application.exception.AvailabilityException;
import com.toneatti.turnos.availability.domain.model.AvailableSlot;
import com.toneatti.turnos.availability.domain.ports.in.GetAvailableSlotsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 14);

    @Mock
    private GetAvailableSlotsUseCase getAvailableSlotsUseCase;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void getAvailableSlotsReturnsTheSlotsOfTheUseCase() {
        List<AvailableSlot> slots = List.of(new AvailableSlot(DAY, LocalTime.of(9, 0), LocalTime.of(9, 30)));
        when(getAvailableSlotsUseCase.getAvailableSlots(15L, DAY, DAY)).thenReturn(Optional.of(slots));

        assertThat(availabilityService.getAvailableSlots(15L, DAY, DAY)).isSameAs(slots);
    }

    @Test
    void getAvailableSlotsThrowsAvailabilityExceptionWhenTheProfessionalDoesNotExist() {
        when(getAvailableSlotsUseCase.getAvailableSlots(99L, DAY, DAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getAvailableSlots(99L, DAY, DAY))
                .isInstanceOf(AvailabilityException.class)
                .hasMessageContaining("99");
    }
}
