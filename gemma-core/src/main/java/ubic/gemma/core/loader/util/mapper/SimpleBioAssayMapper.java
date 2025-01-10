package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;

/**
 * A simple strategy for matching BioAssay to sample name.
 * @author poirigui
 */
public class SimpleBioAssayMapper extends AbstractBioAssayMapper implements HintingEntityMapper<BioAssay> {

    @Override
    public String getName() {
        return "simple";
    }

    @Override
    public Set<BioAssay> matchAll( Collection<BioAssay> bas, String n ) {
        Set<BioAssay> results = new HashSet<>( 1 ); // ideally, should only match 1 element

        // BioAssay ID
        bas.stream().filter( ba -> matchId( ba.getId(), n ) ).forEach( results::add );

        // BioAssay name
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchName( ba.getName(), n ) ).forEach( results::add );
        }

        // BioAssay name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getName(), n ) ).forEach( results::add );
        }

        // BioMaterial name
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchName( ba.getSampleUsed().getName(), n ) ).forEach( results::add );
        }

        // BioMaterial name (case-insensitive)
        if ( results.isEmpty() ) {
            bas.stream().filter( ba -> matchNameIgnoreCase( ba.getSampleUsed().getName(), n ) ).forEach( results::add );
        }

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
