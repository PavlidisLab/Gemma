package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.common.Identifiable;

/**
 * A minimalistic projection of an {@link ExpressionExperiment}.
 * <p>
 * Implemented as a Java record (formerly a Lombok {@code @Value} class). {@link #getId()} is
 * provided explicitly to satisfy the {@link Identifiable} contract, alongside the record's
 * canonical {@code id()} accessor. {@link #getShortName()} is provided as a {@code get}-prefixed
 * alias to preserve the JavaBean accessor convention used by existing callers.
 *
 * @author poirigui
 */
public record ExpressionExperimentIdAndShortName(Long id, String shortName) implements Identifiable {

    @Override
    public Long getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }
}
