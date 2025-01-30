package ubic.gemma.core.loader.expression.geo.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.util.mapper.AbstractBioAssayMapper;
import ubic.gemma.core.loader.util.mapper.HintingEntityMapper;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Strategy to match a {@link BioAssay} against a sample name provided by GEO.
 * <p>
 * Ideally, the provided sample name is a GSM accession, but in some cases it might originate from a supplementary file,
 * so this has some fallback on other metadata.
 * @author poirigui
 */
@CommonsLog
public class GeoBioAssayMapper extends AbstractBioAssayMapper implements HintingEntityMapper<BioAssay> {

    @Override
    public String getName() {
        return "geo";
    }

    @Override
    protected Set<BioAssay> matchAllInternal( Collection<BioAssay> bas, String sampleName ) {
        Set<BioAssay> results = new HashSet<>( 1 ); // ideally, should only match 1 element

        // BioAssay GEO accession (canonical)
        if ( matchWithFunction( bas, BioAssay::getAccession, this::matchGeoAccession, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by assay GEO accession" );
            return results;
        }

        // BioAssay ID
        if ( matchWithFunction( bas, BioAssay::getId, this::matchId, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by assay ID" );
            return results;
        }

        // BioAssay name
        if ( matchWithFunction( bas, BioAssay::getName, this::matchName, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by assay name" );
            return results;
        }

        // BioAssay name (case-insensitive)
        if ( matchWithFunction( bas, BioAssay::getName, this::matchNameIgnoreCase, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by assay name (case-insensitive)" );
            return results;
        }

        // BioMaterial GEO accession
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getExternalAccession(), this::matchGeoAccession, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by sample GEO accession" );
            return results;
        }

        // BioMaterial name
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getName(), this::matchName, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by sample name" );
            return results;
        }

        // BioMaterial name (case-insensitive)
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getName(), this::matchNameIgnoreCase, sampleName, results ) ) {
            log.info( "Matched '" + sampleName + "' by sample name (case-insensitive)" );
            return results;
        }

        // at this point, this is desperation

        // BioAssay description
        if ( matchWithFunction( bas, BioAssay::getDescription, this::matchDescriptionIgnoreCase, sampleName, results ) ) {
            log.warn( "Matched '" + sampleName + "' by assay description" );
            return results;
        }

        // BioMaterial description
        if ( matchWithFunction( bas, ba -> ba.getSampleUsed().getDescription(), this::matchDescriptionIgnoreCase, sampleName, results ) ) {
            log.warn( "Matched '" + sampleName + "' by sample description" );
            return results;
        }

        log.warn( "No match found for '" + sampleName + "'" );
        return results;
    }

    private boolean matchGeoAccession( @Nullable DatabaseEntry accession, String n ) {
        return accession != null && accession.getExternalDatabase().getName().equals( ExternalDatabases.GEO )
                && accession.getAccession().equals( n );
    }

    @Override
    public List<String> getCandidateIdentifiers( BioAssay entity ) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if ( entity.getAccession() != null && entity.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            candidates.add( entity.getAccession().getAccession() );
        }
        if ( entity.getId() != null ) {
            candidates.add( entity.getId().toString() );
        }
        candidates.add( entity.getName() );
        if ( entity.getSampleUsed().getExternalAccession() != null && entity.getSampleUsed().getExternalAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            candidates.add( entity.getSampleUsed().getExternalAccession().getAccession() );
        }
        candidates.add( entity.getSampleUsed().getName() );
        return new ArrayList<>( candidates );
    }
}
