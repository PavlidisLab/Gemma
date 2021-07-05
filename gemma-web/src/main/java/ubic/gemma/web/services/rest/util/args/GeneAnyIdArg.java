package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;
import java.util.Collections;

/**
 * Base class for GeneArg representing any of the identifiers of a Gene.
 *
 * @param <T> the type of the Gene Identifier.
 * @author tesarst
 */
public abstract class GeneAnyIdArg<T> extends GeneArg<T> {

    GeneAnyIdArg( T value ) {
        super( value );
    }

    @Override
    public Collection<GeneValueObject> getValueObjects( GeneService service ) {
        return Collections.singleton( service.loadValueObject( this.getEntity( service ) ) );
    }

    @Override
    public Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService ) {
        Gene gene = this.getEntity( geneService );
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

    @Override
    public Collection<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon ) {
        Gene gene = this.getEntity( geneService );
        if ( !gene.getTaxon().equals( taxon ) ) {
            throw new IllegalArgumentException( "Taxon does not match the gene's taxon." );
        }
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

}
