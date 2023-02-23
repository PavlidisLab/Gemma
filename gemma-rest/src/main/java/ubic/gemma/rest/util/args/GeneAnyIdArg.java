package ubic.gemma.rest.util.args;

import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import javax.ws.rs.BadRequestException;
import java.util.Collections;
import java.util.List;

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
    public List<GeneValueObject> getValueObjects( GeneService service ) {
        GeneValueObject vo = service.loadValueObject( this.getEntity( service ) );
        if ( vo != null ) {
            return Collections.singletonList( vo );
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<PhysicalLocationValueObject> getGeneLocation( GeneService geneService ) {
        Gene gene = this.getEntity( geneService );
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

    @Override
    public List<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon ) {
        Gene gene = this.getEntity( geneService );
        if ( !gene.getTaxon().equals( taxon ) ) {
            throw new BadRequestException( "Taxon does not match the gene's taxon." );
        }
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

}
