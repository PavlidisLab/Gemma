package ubic.gemma.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;

/**
 * Base class for GeneArg representing any of the identifiers of a Gene.
 *
 * @param <T> the type of the Gene Identifier.
 * @author tesarst
 */
public abstract class GeneAnyIdArg<T> extends GeneArg<T> {

    protected GeneAnyIdArg( String propertyName, Class<T> propertyType, T value ) {
        super( propertyName, propertyType, value );
    }

    @Nullable
    @Override
    Gene getEntityWithTaxon( GeneService geneService, Taxon taxon ) {
        // gene retrieved by ID are unambiguous
        Gene gene = getEntity( geneService );
        if ( gene != null && !gene.getTaxon().equals( taxon ) ) {
            throw new BadRequestException( String.format( "The gene %s does not belong to taxon %s.", gene, taxon ) );
        }
        return gene;
    }
}
