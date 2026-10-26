package com.toneatti.turnos.availability.application.usecases;

import com.toneatti.turnos.availability.application.exception.AvailabilityUnavailableException;
import com.toneatti.turnos.availability.application.exception.DateRangeTooLongException;
import com.toneatti.turnos.availability.application.exception.InvalidDateRangeException;
import com.toneatti.turnos.availability.application.exception.ProfessionalDisabledException;
import com.toneatti.turnos.availability.domain.model.AvailableSlot;
import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.WeeklySchedule;
import com.toneatti.turnos.catalogo.domain.ports.out.CatalogoRepository;
import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Occupancy;
import com.toneatti.turnos.catedra.domain.model.OccupancyStatus;
import com.toneatti.turnos.catedra.domain.ports.out.CatedraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAvailableSlotsUseCaseImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Long PROFESSIONAL_ID = 15L;
    // 2026-09-14 is a Monday.
    private static final LocalDate MONDAY_14 = LocalDate.of(2026, 9, 14);
    private static final LocalDate TUESDAY_15 = MONDAY_14.plusDays(1);
    private static final LocalDateTime BEFORE_EVERY_SLOT = LocalDateTime.of(2026, 9, 1, 0, 0);

    @Mock
    private CatalogoRepository catalogoRepository;

    @Mock
    private CatedraRepository catedraRepository;

    @Test
    void generatesTheSlotsOfEachDayFromItsSchedules() {
        givenAnEnabledProfessionalWith(
                schedule(MONDAY, "09:00", "10:00", 30),
                schedule(TUESDAY, "14:00", "15:00", 60));
        givenOccupancies(MONDAY_14, TUESDAY_15);

        assertThat(availableSlots(MONDAY_14, TUESDAY_15)).containsExactly(
                slot(MONDAY_14, "09:00", "09:30"),
                slot(MONDAY_14, "09:30", "10:00"),
                slot(TUESDAY_15, "14:00", "15:00"));
    }

    @Test
    void leavesOutALastSlotThatDoesNotFitCompletely() {
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "10:00", 45));
        givenOccupancies(MONDAY_14, MONDAY_14);

        assertThat(availableSlots(MONDAY_14, MONDAY_14)).containsExactly(slot(MONDAY_14, "09:00", "09:45"));
    }

    @Test
    void generatesEveryDayOfARangeThatCrossesWeeks() {
        LocalDate saturday12 = MONDAY_14.minusDays(2);
        LocalDate monday21 = MONDAY_14.plusWeeks(1);
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "09:30", 30));
        givenOccupancies(saturday12, monday21);

        assertThat(availableSlots(saturday12, monday21)).containsExactly(
                slot(MONDAY_14, "09:00", "09:30"),
                slot(monday21, "09:00", "09:30"));
    }

    @Test
    void mergesOverlappingSchedulesOfTheSameDayInOrder() {
        givenAnEnabledProfessionalWith(
                schedule(MONDAY, "09:30", "10:30", 30),
                schedule(MONDAY, "09:00", "10:00", 30));
        givenOccupancies(MONDAY_14, MONDAY_14);

        assertThat(availableSlots(MONDAY_14, MONDAY_14)).extracting(AvailableSlot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));
    }

    @Test
    void ignoresSchedulesWithoutRoomForASlotAndDoesNotCallTheCatedra() {
        givenAnEnabledProfessionalWith(
                schedule(MONDAY, "12:00", "09:00", 30),
                schedule(MONDAY, "09:00", "12:00", 0),
                schedule(MONDAY, "09:00", "09:20", 30));

        assertThat(availableSlots(MONDAY_14, MONDAY_14)).isEmpty();
        verifyNoInteractions(catedraRepository);
    }

    @Test
    void findsNoSlotsWithoutSchedulesForTheRange() {
        givenAnEnabledProfessionalWith(schedule(TUESDAY, "09:00", "12:00", 30));

        assertThat(availableSlots(MONDAY_14, MONDAY_14)).isEmpty();
        verifyNoInteractions(catedraRepository);
    }

    @Test
    void leavesOutTheSlotsThatAlreadyStarted() {
        givenAnEnabledProfessionalWith(
                schedule(MONDAY, "09:00", "10:30", 30),
                schedule(TUESDAY, "09:00", "09:30", 30));
        LocalDate sunday13 = MONDAY_14.minusDays(1);
        givenOccupancies(sunday13, TUESDAY_15);

        List<AvailableSlot> slots = useCaseAt(LocalDateTime.of(MONDAY_14, LocalTime.of(9, 30)))
                .getAvailableSlots(PROFESSIONAL_ID, sunday13, TUESDAY_15).orElseThrow();

        assertThat(slots).containsExactly(
                slot(MONDAY_14, "09:30", "10:00"),
                slot(MONDAY_14, "10:00", "10:30"),
                slot(TUESDAY_15, "09:00", "09:30"));
    }

    @Test
    void leavesOutConfirmedAndHeldSlotsWithOrWithoutSeconds() {
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "11:00", 30));
        givenOccupancies(MONDAY_14, MONDAY_14,
                new Occupancy(MONDAY_14, LocalTime.parse("09:00:00"), OccupancyStatus.CONFIRMED, null),
                new Occupancy(MONDAY_14, LocalTime.parse("10:00"), OccupancyStatus.HELD,
                        Instant.parse("2026-09-10T14:44:50Z")));

        assertThat(availableSlots(MONDAY_14, MONDAY_14)).containsExactly(
                slot(MONDAY_14, "09:30", "10:00"),
                slot(MONDAY_14, "10:30", "11:00"));
    }

    @Test
    void ignoresOccupanciesOfOtherDays() {
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "09:30", 30));
        givenOccupancies(MONDAY_14, TUESDAY_15,
                new Occupancy(TUESDAY_15, LocalTime.of(9, 0), OccupancyStatus.CONFIRMED, null));

        assertThat(availableSlots(MONDAY_14, TUESDAY_15)).containsExactly(slot(MONDAY_14, "09:00", "09:30"));
    }

    @Test
    void findsNothingWhenTheProfessionalDoesNotExist() {
        when(catalogoRepository.findProfessionalById(PROFESSIONAL_ID)).thenReturn(Optional.empty());

        assertThat(useCaseAt(BEFORE_EVERY_SLOT).getAvailableSlots(PROFESSIONAL_ID, MONDAY_14, MONDAY_14)).isEmpty();
        verify(catalogoRepository, never()).findWeeklySchedulesByProfessionalId(any());
        verifyNoInteractions(catedraRepository);
    }

    @Test
    void failsWhenTheProfessionalIsDisabled() {
        when(catalogoRepository.findProfessionalById(PROFESSIONAL_ID))
                .thenReturn(Optional.of(new Professional(PROFESSIONAL_ID, 1L, "Ana", "Gomez", false)));

        assertThatThrownBy(() -> availableSlots(MONDAY_14, MONDAY_14))
                .isInstanceOf(ProfessionalDisabledException.class);
        verifyNoInteractions(catedraRepository);
    }

    @Test
    void failsWhenFromIsAfterTo() {
        assertThatThrownBy(() -> availableSlots(TUESDAY_15, MONDAY_14))
                .isInstanceOf(InvalidDateRangeException.class);
        verifyNoInteractions(catalogoRepository, catedraRepository);
    }

    @Test
    void acceptsARangeOfTheMaximumLength() {
        givenAnEnabledProfessionalWith();

        assertThat(availableSlots(MONDAY_14, MONDAY_14.plusDays(GetAvailableSlotsUseCaseImpl.MAX_RANGE_DAYS - 1)))
                .isEmpty();
    }

    @Test
    void failsWhenTheRangeIsLongerThanTheMaximum() {
        assertThatThrownBy(() -> availableSlots(MONDAY_14,
                MONDAY_14.plusDays(GetAvailableSlotsUseCaseImpl.MAX_RANGE_DAYS)))
                .isInstanceOf(DateRangeTooLongException.class);
        verifyNoInteractions(catalogoRepository, catedraRepository);
    }

    @Test
    void failsAsAnInvalidRangeWhenTheCatedraRejectsIt() {
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "09:30", 30));
        whenTheCatedraFailsWith(new CatedraRequestRejectedException(
                400, CatedraRequestRejectedException.INVALID_DATE_RANGE, "rejected", null));

        assertThatThrownBy(() -> availableSlots(MONDAY_14, MONDAY_14))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void failsAsUnavailableWhenTheCatedraRejectsTheRequestWithAnotherCode() {
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "09:30", 30));
        whenTheCatedraFailsWith(new CatedraRequestRejectedException(
                403, "STUDENT_INTEGRATION_NOT_FOUND", "rejected", null));

        assertThatThrownBy(() -> availableSlots(MONDAY_14, MONDAY_14))
                .isInstanceOf(AvailabilityUnavailableException.class)
                .hasCauseInstanceOf(CatedraRequestRejectedException.class);
    }

    @Test
    void failsAsUnavailableWhenTheCatedraCannotAnswer() {
        givenAnEnabledProfessionalWith(schedule(MONDAY, "09:00", "09:30", 30));
        whenTheCatedraFailsWith(new CatedraUnavailableException("down", null));

        assertThatThrownBy(() -> availableSlots(MONDAY_14, MONDAY_14))
                .isInstanceOf(AvailabilityUnavailableException.class)
                .hasCauseInstanceOf(CatedraUnavailableException.class);
    }

    @Test
    void failsAsUnavailableWhenTheCatalogoCannotAnswer() {
        when(catalogoRepository.findProfessionalById(PROFESSIONAL_ID))
                .thenThrow(new CatalogoUnavailableException("down", null));

        assertThatThrownBy(() -> availableSlots(MONDAY_14, MONDAY_14))
                .isInstanceOf(AvailabilityUnavailableException.class)
                .hasCauseInstanceOf(CatalogoUnavailableException.class);
        verifyNoInteractions(catedraRepository);
    }

    private List<AvailableSlot> availableSlots(LocalDate from, LocalDate to) {
        return useCaseAt(BEFORE_EVERY_SLOT).getAvailableSlots(PROFESSIONAL_ID, from, to).orElseThrow();
    }

    private GetAvailableSlotsUseCaseImpl useCaseAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(ZONE).toInstant(), ZONE);
        return new GetAvailableSlotsUseCaseImpl(catalogoRepository, catedraRepository, clock);
    }

    private void givenAnEnabledProfessionalWith(WeeklySchedule... schedules) {
        when(catalogoRepository.findProfessionalById(PROFESSIONAL_ID))
                .thenReturn(Optional.of(new Professional(PROFESSIONAL_ID, 1L, "Ana", "Gomez", true)));
        when(catalogoRepository.findWeeklySchedulesByProfessionalId(PROFESSIONAL_ID)).thenReturn(List.of(schedules));
    }

    private void givenOccupancies(LocalDate from, LocalDate to, Occupancy... occupancies) {
        when(catedraRepository.findOccupancies(PROFESSIONAL_ID, from, to)).thenReturn(List.of(occupancies));
    }

    private void whenTheCatedraFailsWith(RuntimeException failure) {
        when(catedraRepository.findOccupancies(PROFESSIONAL_ID, MONDAY_14, MONDAY_14)).thenThrow(failure);
    }

    private static WeeklySchedule schedule(DayOfWeek dayOfWeek, String startTime, String endTime, int minutes) {
        return new WeeklySchedule(null, PROFESSIONAL_ID, dayOfWeek, LocalTime.parse(startTime),
                LocalTime.parse(endTime), minutes);
    }

    private static AvailableSlot slot(LocalDate date, String startTime, String endTime) {
        return new AvailableSlot(date, LocalTime.parse(startTime), LocalTime.parse(endTime));
    }
}
