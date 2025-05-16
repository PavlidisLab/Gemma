package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * Match design elements using the official symbol of a gene.
 * @see Gene#getOfficialSymbol()
 * @author poirigui
 */
public class OfficialSymbolDesignElementMapper extends AbstractGeneIdentifierBasedDesignElementMapper<String> {

    public OfficialSymbolDesignElementMapper( Map<CompositeSequence, Set<Gene>> cs2g ) {
        super( cs2g );
    }

    @Override
    public String getName() {
        return "Official Symbol";
    }

    @Nullable
    @Override
    protected String getIdentifier( Gene gene ) {
        return gene.getOfficialSymbol();
    }

    @Override
    protected String processIdentifier( String identifier ) {
        return StringUtils.strip( identifier );
    }
}
