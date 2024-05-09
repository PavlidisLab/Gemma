package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * String argument type for Gene API. Represents the official symbol, which is non-taxon specific, so the argument
 * effectively represents all homologues that match the symbol.
 *
 * @author tesarst
 */
@Schema(type = "string", description = "An official gene symbol approved by HGNC.",
        externalDocs = @ExternalDocumentation(url = "https://www.genenames.org/"))
public class GeneSymbolArg extends GeneArg<String> {

    GeneSymbolArg( String s ) {
        super( "officialSymbol", String.class, s );
    }

    @Override
    Gene getEntity( GeneService service ) {
        String value = getValue();
        Gene gene;
        Collection<Gene> genes = value.isEmpty() ? null : service.findByOfficialSymbol( value );
        if ( genes == null || genes.isEmpty() ) {
            gene = null;
        } else {
            gene = genes.iterator().next();
        }
        return gene;
    }

    @Override
    List<Gene> getEntities( GeneService service ) throws BadRequestException {
        return new ArrayList<>( service.findByOfficialSymbol( getValue() ) );
    }

    @Override
    List<Gene> getEntitiesWithTaxon( GeneService service, Taxon taxon ) {
        return Collections.singletonList( service.findByOfficialSymbol( getValue(), taxon ) );
    }
}
