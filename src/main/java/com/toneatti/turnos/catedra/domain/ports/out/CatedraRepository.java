package com.toneatti.turnos.catedra.domain.ports.out;

import com.toneatti.turnos.catedra.domain.exception.CatedraRequestRejectedException;
import com.toneatti.turnos.catedra.domain.exception.CatedraUnavailableException;
import com.toneatti.turnos.catedra.domain.model.Occupancy;

import java.time.LocalDate;
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

}
