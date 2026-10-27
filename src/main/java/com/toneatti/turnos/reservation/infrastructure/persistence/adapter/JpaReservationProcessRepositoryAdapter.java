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
            ReservationProcessEntity savedEntity = jpaReservationProcessRepository.saveAndFlush(entity);
            return reservationProcessMapper.toDomainModel(savedEntity);
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
        if (!jpaReservationProcessRepository.existsById(id)) {
            return Optional.empty();
        }
        ReservationProcessEntity entity = reservationProcessMapper.toEntity(process);
        entity.setId(id);
        try {
            ReservationProcessEntity savedEntity = jpaReservationProcessRepository.saveAndFlush(entity);
            return Optional.of(reservationProcessMapper.toDomainModel(savedEntity));
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentReservationUpdateException(id, e);
        }
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
