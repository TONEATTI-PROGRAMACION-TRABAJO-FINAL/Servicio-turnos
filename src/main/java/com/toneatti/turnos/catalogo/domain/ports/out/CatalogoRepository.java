package com.toneatti.turnos.catalogo.domain.ports.out;

import com.toneatti.turnos.catalogo.domain.exception.CatalogoUnavailableException;
import com.toneatti.turnos.catalogo.domain.model.Professional;
import com.toneatti.turnos.catalogo.domain.model.ProfessionalCategory;
import com.toneatti.turnos.catalogo.domain.model.WeeklySchedule;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to the local copy of the catalog kept by Servicio-catalogo.
 * Every method throws {@link CatalogoUnavailableException} when Servicio-catalogo cannot answer.
 */
public interface CatalogoRepository {

    List<ProfessionalCategory> findAllCategories();

    /**
     * Enabled professionals of the given category, or of every category when {@code categoryId} is null.
     */
    List<Professional> findProfessionalsByCategoryId(Long categoryId);

    /**
     * The professional with that id, enabled or not, so past reservations can still show the name.
     */
    Optional<Professional> findProfessionalById(Long id);

    /**
     * Enabled weekly schedules of the professional; empty when it has none or does not exist.
     */
    List<WeeklySchedule> findWeeklySchedulesByProfessionalId(Long professionalId);

}
