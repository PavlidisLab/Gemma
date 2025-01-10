package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import java.util.Map;
import java.util.Set;

/**
 * Match design elements using Ensembl IDs.
 * @see Gene#getEnsemblId()
 * @author poirigui
 */
public class EnsemblIdDesignElementMapper extends AbstractGeneIdentifierBasedDesignElementMapper<String> {

    public EnsemblIdDesignElementMapper( Map<CompositeSequence, Set<Gene>> cs2g ) {
        super( cs2g );
    }

    @Override
    public String getName() {
        return "Ensembl ID";
    }

    @Override
    protected String getIdentifier( Gene gene ) {
        return gene.getEnsemblId() != null ? stripEnsemblIdVersion( gene.getEnsemblId() ) : null;
    }

    @Override
    protected String processIdentifier( String identifier ) {
        return stripEnsemblIdVersion( StringUtils.strip( identifier ) );
    }

    private String stripEnsemblIdVersion( String ensemblId ) {
        int i;
        if ( ( i = ensemblId.lastIndexOf( '.' ) ) != -1 ) {
            return ensemblId.substring( 0, i );
        } else {
            return ensemblId;
        }
    }
}
