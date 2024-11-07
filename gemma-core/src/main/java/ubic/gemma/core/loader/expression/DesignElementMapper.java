package ubic.gemma.core.loader.expression;

import lombok.Value;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * Maps gene identifiers to {@link CompositeSequence}.
 * @author poirigui
 */
public interface DesignElementMapper {

    String getName();

    boolean contains( String geneIdentifier );

    boolean containsAny( Collection<String> genesSet );

    @Nullable
    CompositeSequence get( String geneIdentifier );

    /**
     * Calculate mapping statistics for a set of gene identifiers.
     */
    MappingStatistics getMappingStatistics( Collection<String> geneIdentifiers );

    @Value
    class MappingStatistics {
        /**
         * Proportion of the gene identifiers that were mapped to a {@link CompositeSequence}.
         */
        double overlap;
        /**
         * Proportion of the composite sequences that were mapped by a gene identifier.
         */
        double coverage;
    }
}
