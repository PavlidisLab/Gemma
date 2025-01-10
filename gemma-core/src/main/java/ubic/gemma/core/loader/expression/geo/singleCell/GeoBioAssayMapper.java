package ubic.gemma.core.loader.expression.geo.singleCell;

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
public class GeoBioAssayMapper extends AbstractBioAssayMapper implements HintingEntityMapper<BioAssay> {

    @Override
    public String getName() {
        return "geo";
    }

    @Override
    public Set<BioAssay> matchAll( Collection<BioAssay> bas, String sampleName ) {
        Set<BioAssay> results = new HashSet<>( 1 ); // ideally, should only match 1 element

        // BioAssay GEO accession (canonical)
        bas.stream().filter( ba -> matchGeoAccession( ba.getAccession(), sampleName ) ).forEach( results::add );

        // BioAssay ID
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchId( ba.getId(), sampleName ) ).forEach( results::add );
        }

        // BioAssay name
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchName( ba.getName(), sampleName ) ).forEach( results::add );
        }

        // BioAssay name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getName(), sampleName ) ).forEach( results::add );
        }

        // BioMaterial GEO accession
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchGeoAccession( ba.getSampleUsed().getExternalAccession(), sampleName ) ).forEach( results::add );
        }

        // BioMaterial name
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchName( ba.getSampleUsed().getName(), sampleName ) ).forEach( results::add );
        }

        // BioMaterial name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getSampleUsed().getName(), sampleName ) ).forEach( results::add );
        }

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
