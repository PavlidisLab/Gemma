package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import java.util.Map;
import java.util.Set;

/**
 * Match design elements using the NCBI ID.
 * @see Gene#getNcbiGeneId()
 * @author poirigui
 */
public class NcbiIdDesignElementMapper extends AbstractGeneIdentifierBasedDesignElementMapper<Integer> {

    public NcbiIdDesignElementMapper( Map<CompositeSequence, Set<Gene>> cs2g ) {
        super( cs2g );
    }

    @Override
    public String getName() {
        return "NCBI ID";
    }

    @Override
    protected Integer getIdentifier( Gene gene ) {
        return gene.getNcbiGeneId();
    }

    @Override
    protected Integer processIdentifier( String identifier ) {
        try {
            return Integer.valueOf( StringUtils.strip( identifier ) );
        } catch ( NumberFormatException e ) {
            return null;
        }
    }
}
