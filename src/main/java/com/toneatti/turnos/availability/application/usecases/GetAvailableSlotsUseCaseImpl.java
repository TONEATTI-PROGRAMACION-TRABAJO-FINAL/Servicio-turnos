package com.toneatti.turnos.availability.application.usecases;

import com.toneatti.turnos.availability.application.exception.AvailabilityUnavailableException;
import com.toneatti.turnos.availability.application.exception.DateRangeTooLongException;
import com.toneatti.turnos.availability.application.exception.InvalidDateRangeException;
import com.toneatti.turnos.availability.application.exception.ProfessionalDisabledException;
import com.toneatti.turnos.availability.domain.model.AvailableSlot;
import com.toneatti.turnos.availability.domain.ports.in.GetAvailableSlotsUseCase;
import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.WeeklySchedule;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Slots generated from the weekly schedules of the professional, minus the occupancies of the cátedra
 * (confirmed reservations and active holds) and the slots that already started.
 */
@Component
@RequiredArgsConstructor
public class GetAvailableSlotsUseCaseImpl implements GetAvailableSlotsUseCase {

    static final int MAX_RANGE_DAYS = 31;

    private final CatalogoRepository catalogoRepository;
    private final CatedraRepository catedraRepository;
    private final Clock clock;

    @Override
    public Optional<List<AvailableSlot>> getAvailableSlots(Long professionalId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        try {
            Optional<Professional> professional = catalogoRepository.findProfessionalById(professionalId);
            if (professional.isEmpty()) {
                return Optional.empty();
            }
            if (!professional.get().enabled()) {
                throw new ProfessionalDisabledException(professionalId);
            }
            List<AvailableSlot> candidates = generateSlots(
                    catalogoRepository.findWeeklySchedulesByProfessionalId(professionalId), from, to);
            // Nothing to subtract from: the cátedra is not called.
            if (candidates.isEmpty()) {
                return Optional.of(candidates);
            }
            return Optional.of(withoutOccupancies(candidates, professionalId, from, to));
        } catch (CatalogoUnavailableException | CatedraUnavailableException e) {
            throw new AvailabilityUnavailableException("Could not calculate the availability", e);
        }
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException(from, to);
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new DateRangeTooLongException(from, to, MAX_RANGE_DAYS);
        }
    }

    private List<AvailableSlot> generateSlots(List<WeeklySchedule> schedules, LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<AvailableSlot> slots = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            // Keyed by start time: overlapping schedules of the same day do not repeat a slot, and the day stays sorted.
            Map<LocalTime, AvailableSlot> slotsOfDay = new TreeMap<>();
            for (WeeklySchedule schedule : schedules) {
                if (schedule.dayOfWeek() == date.getDayOfWeek()) {
                    addSlots(schedule, date, slotsOfDay);
                }
            }
            for (AvailableSlot slot : slotsOfDay.values()) {
                if (!LocalDateTime.of(slot.date(), slot.startTime()).isBefore(now)) {
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    // Seconds of the day instead of LocalTime arithmetic, which wraps around midnight. A last slot that does not
    // fit completely before the end time is left out; a schedule that ends before it starts has no slots.
    private static void addSlots(WeeklySchedule schedule, LocalDate date, Map<LocalTime, AvailableSlot> slotsOfDay) {
        if (schedule.slotDurationMinutes() <= 0) {
            return;
        }
        long duration = schedule.slotDurationMinutes() * 60L;
        long end = schedule.endTime().toSecondOfDay();
        for (long start = schedule.startTime().toSecondOfDay(); start + duration <= end; start += duration) {
            LocalTime startTime = LocalTime.ofSecondOfDay(start);
            slotsOfDay.putIfAbsent(startTime,
                    new AvailableSlot(date, startTime, LocalTime.ofSecondOfDay(start + duration)));
        }
    }

    // Every occupancy is CONFIRMED or HELD (section 8): both make the slot unavailable. Compared as LocalDateTime,
    // so 09:30 and 09:30:00 are the same slot.
    private List<AvailableSlot> withoutOccupancies(List<AvailableSlot> candidates, Long professionalId,
                                                   LocalDate from, LocalDate to) {
        Set<LocalDateTime> occupied;
        try {
            occupied = catedraRepository.findOccupancies(professionalId, from, to).stream()
                    .map(occupancy -> LocalDateTime.of(occupancy.date(), occupancy.startTime()))
                    .collect(Collectors.toSet());
        } catch (CatedraRequestRejectedException e) {
            if (CatedraRequestRejectedException.INVALID_DATE_RANGE.equals(e.getCode())) {
                throw new InvalidDateRangeException(from, to);
            }
            throw new AvailabilityUnavailableException("Catedra rejected the occupancies with " + e.getCode(), e);
        }
        return candidates.stream()
                .filter(slot -> !occupied.contains(LocalDateTime.of(slot.date(), slot.startTime())))
                .toList();
    }
}
