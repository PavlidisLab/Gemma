package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class GeneNameDesignElementMapper extends AbstractGeneIdentifierBasedDesignElementMapper<String> {

    public GeneNameDesignElementMapper( Map<CompositeSequence, Set<Gene>> cs2g ) {
        super( cs2g );
    }

    @Override
    public String getName() {
        return "Gene Name";
    }

    @Nullable
    @Override
    protected String getIdentifier( Gene gene ) {
        return gene.getName();
    }

    @Override
    protected String processIdentifier( String identifier ) {
        return StringUtils.strip( identifier );
    }
}
