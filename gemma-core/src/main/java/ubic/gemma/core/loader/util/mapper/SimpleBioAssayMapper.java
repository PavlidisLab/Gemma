package ubic.gemma.core.loader.util.mapper;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;

/**
 * A simple strategy for matching BioAssay to sample name.
 * @author poirigui
 */
@CommonsLog
public class SimpleBioAssayMapper extends AbstractBioAssayMapper implements HintingEntityMapper<BioAssay> {

    @Override
    public String getName() {
        return "simple";
    }

    @Override
    public Set<BioAssay> matchAllInternal( Collection<BioAssay> bas, String n ) {
        Set<BioAssay> results = new HashSet<>( 1 ); // ideally, should only match 1 element

        // BioAssay ID
        if ( matchWithFunction( bas, BioAssay::getId, this::matchId, n, results ) ) {
            log.info( "Matched '" + n + "' by assay ID" );
            return results;
        }

        // BioAssay short name
        if ( matchWithFunction( bas, BioAssay::getShortName, StringUtils::equals, n, results ) ) {
            log.info( "Matched '" + n + "' by assay short name" );
            return results;
        }

        // BioAssay short name (case insensitive)
        if ( matchWithFunction( bas, BioAssay::getShortName, StringUtils::equalsIgnoreCase, n, results ) ) {
            log.info( "Matched '" + n + "' by assay short name (case-insensitive)" );
            return results;
        }

        // BioAssay name
        if ( matchWithFunction( bas, BioAssay::getName, this::matchName, n, results ) ) {
            log.info( "Matched '" + n + "' by assay name" );
            return results;
        }

        // BioAssay name (case-insensitive)
        if ( matchWithFunction( bas, BioAssay::getName, this::matchNameIgnoreCase, n, results ) ) {
            log.info( "Matched '" + n + "' by assay name (case-insensitive)" );
            return results;
        }

        // BioMaterial name
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getName(), this::matchName, n, results ) ) {
            log.info( "Matched '" + n + "' by sample name" );
            return results;
        }

        // BioMaterial name (case-insensitive)
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getName(), this::matchNameIgnoreCase, n, results ) ) {
            log.info( "Matched '" + n + "' by sample name (case-insensitive)" );
            return results;
        }

        // at this point, this is desperation

        // BioAssay description
        if ( matchWithFunction( bas, BioAssay::getDescription, this::matchDescriptionIgnoreCase, n, results ) ) {
            log.warn( "Matched '" + n + "' by assay description" );
            return results;
        }

        // BioMaterial description
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getDescription(), this::matchDescriptionIgnoreCase, n, results ) ) {
            log.warn( "Matched '" + n + "' by sample description" );
            return results;
        }

        log.debug( "No match found for '" + n + "'" );

        return results;
    }

    @Override
    public List<String> getCandidateIdentifiers( BioAssay entity ) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if ( entity.getId() != null ) {
            candidates.add( entity.getId().toString() );
        }
        candidates.add( entity.getName() );
        candidates.add( entity.getSampleUsed().getName() );
        return new ArrayList<>( candidates );
    }
}
