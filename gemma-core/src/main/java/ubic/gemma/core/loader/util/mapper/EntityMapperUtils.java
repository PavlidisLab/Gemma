package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EntityMapperUtils {

    /**
     * Render a breakdown of possible {@code BioAssay -> sample identifier} associations.
     * <p>
     * TODO: make this generic for all possible entities.
     */
    public static String getPossibleIdentifiers( Collection<BioAssay> bioAssays, EntityMapper<BioAssay> mapper ) {
        return bioAssays.stream()
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .map( ba2 -> ba2 + ":\t" + String.join( ", ", getPossibleIdentifiers( ba2, mapper ) ) )
                .collect( Collectors.joining( "\n\t" ) );
    }

    private static List<String> getPossibleIdentifiers( BioAssay ba, EntityMapper<BioAssay> entityMapper ) {
        if ( entityMapper instanceof HintingEntityMapper ) {
            return ( ( HintingEntityMapper<BioAssay> ) entityMapper ).getCandidateIdentifiers( ba );
        } else {
            return Collections.singletonList( ba.getName() );
        }
    }
}
