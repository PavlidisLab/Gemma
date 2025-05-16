package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.common.Identifiable;

import java.util.List;

/**
 * Interface for mappers that can provide hints of identifier candidates.
 * <p>
 * This is mainly used to provide feedback to the user.
 * @author poirigui
 */
public interface HintingEntityMapper<T extends Identifiable> extends EntityMapper<T> {

    /**
     * Obtain a list of possible identifiers for the given entity from higher to lower confidence.
     */
    List<String> getCandidateIdentifiers( T entity );
}
