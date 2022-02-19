package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * String argument type for Gene API. Represents the official symbol, which is non-taxon specific, so the argument
 * effectively represents all homologues that match the symbol.
 *
 * @author tesarst
 */
@Schema(type = "string")
public class GeneSymbolArg extends GeneArg<String> {

    GeneSymbolArg( String s ) {
        super( s );
    }

    @Override
    public Gene getEntity( GeneService service ) {
        String value = getValue();
        Gene gene;
        Collection<Gene> genes =
                value == null || value.isEmpty() ? null : service.findByOfficialSymbol( value );
        if ( genes == null || genes.isEmpty() ) {
            gene = null;
        } else {
            gene = genes.iterator().next();
        }
        return checkEntity( gene );
    }

    @Override
    public String getPropertyName() {
        return "officialSymbol";
    }

    @Override
    public List<GeneValueObject> getValueObjects( GeneService service ) {
        Collection<Gene> genes = service.findByOfficialSymbol( this.getValue() );
        checkEntity( genes == null || genes.size() < 1 ? null : genes.iterator().next() );
        return service.loadValueObjects( genes );
    }

    @Override
    public List<PhysicalLocationValueObject> getGeneLocation( GeneService geneService ) {
        Collection<Gene> genes = geneService.findByOfficialSymbol( this.getValue() );
        List<PhysicalLocationValueObject> gVos = new ArrayList<>( genes.size() );
        for ( Gene gene : genes ) {
            gVos.addAll( geneService.getPhysicalLocationsValueObjects( gene ) );
        }
        return gVos;
    }

    @Override
    public List<PhysicalLocationValueObject> getGeneLocation( GeneService geneService, Taxon taxon ) {
        Gene gene = geneService.findByOfficialSymbol( this.getValue(), taxon );
        if ( gene == null )
            return null;
        if ( !gene.getTaxon().equals( taxon ) ) {
            throw new IllegalArgumentException( "Taxon does not match gene's taxon." );
        }
        return geneService.getPhysicalLocationsValueObjects( gene );
    }

}
