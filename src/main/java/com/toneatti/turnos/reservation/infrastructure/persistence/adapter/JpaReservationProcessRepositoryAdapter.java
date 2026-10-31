package com.toneatti.turnos.reservation.infrastructure.persistence.adapter;

import com.toneatti.turnos.reservation.application.exception.ConcurrentReservationUpdateException;
import com.toneatti.turnos.reservation.application.exception.ReservationInProgressException;
import com.toneatti.turnos.reservation.domain.model.ReservationProcess;
import com.toneatti.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import com.toneatti.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import com.toneatti.turnos.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import com.toneatti.turnos.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaReservationProcessRepositoryAdapter implements ReservationProcessRepository {

    private final JpaReservationProcessRepository jpaReservationProcessRepository;
    private final ReservationProcessMapper reservationProcessMapper;

    @Override
    public ReservationProcess create(ReservationProcess process) {
        ReservationProcessEntity entity = reservationProcessMapper.toEntity(process);
        try {
            return saveAndMap(entity);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests for the same slot can both find no active process; the unique constraint decides.
            if (findActiveByUserAndSlot(process.getUserExternalId(), process.getProfessionalId(),
                    process.getDate(), process.getStartTime()).isPresent()) {
                throw new ReservationInProgressException(process.getUserExternalId(), e);
            }
            throw e;
        }
    }

    // The version read with the process travels in the entity, so saving over a newer one fails instead of
    // overwriting it: the calls to the cátedra between reading and saving can take seconds.
    @Override
    public Optional<ReservationProcess> update(Long id, ReservationProcess process) {
        // The mapper turns a null process into a null entity, which would fail further down without saying why.
        Objects.requireNonNull(process, "process");
        if (!jpaReservationProcessRepository.existsById(id)) {
            return Optional.empty();
        }
        ReservationProcessEntity entity = reservationProcessMapper.toEntity(process);
        entity.setId(id);
        try {
            return Optional.of(saveAndMap(entity));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentReservationUpdateException(id, e);
        }
    }

    // Spring Data never returns null from saveAndFlush. Stating it also tells static analysis that the mapper, which
    // only returns null for a null entity, cannot return null here.
    private ReservationProcess saveAndMap(ReservationProcessEntity entity) {
        ReservationProcessEntity savedEntity = Objects.requireNonNull(
                jpaReservationProcessRepository.saveAndFlush(entity), "saveAndFlush returned null");
        return reservationProcessMapper.toDomainModel(savedEntity);
    }

    @Override
    public Optional<ReservationProcess> findByIdAndUserExternalId(Long id, String userExternalId) {
        return jpaReservationProcessRepository.findByIdAndUserExternalId(id, userExternalId)
                .map(reservationProcessMapper::toDomainModel);
    }

    @Override
    public Optional<ReservationProcess> findByReservationProcessId(String reservationProcessId) {
        return jpaReservationProcessRepository.findByReservationProcessId(reservationProcessId)
                .map(reservationProcessMapper::toDomainModel);
    }

    @Override
    public Optional<ReservationProcess> findActiveByUserAndSlot(String userExternalId, Long professionalId,
                                                                LocalDate date, LocalTime startTime) {
        return jpaReservationProcessRepository
                .findByUserExternalIdAndProfessionalIdAndDateAndStartTimeAndActiveTrue(
                        userExternalId, professionalId, date, startTime)
                .map(reservationProcessMapper::toDomainModel);
    }
}
