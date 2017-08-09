package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.WebService;
import ubic.gemma.web.services.rest.util.WellComposedErrorBody;
import ubic.gemma.web.util.EntityNotFoundException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * Created by tesarst on 16/05/17.
 * Mutable argument type base class for Gene API
 */
public abstract class GeneArg<T> extends MutableArg<T, Gene, GeneService, GeneValueObject> {

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Gene argument
     * @return instance of appropriate implementation of GeneArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static GeneArg valueOf( final String s ) {
        try {
            return new GeneNcbiIdArg( Integer.parseInt( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new GeneSymbolArg( s );
        }
    }

    /**
     * @param service the service used to load the Gene value objects.
     * @return all genes that match the value of the GeneArg.
     */
    public abstract Collection<GeneValueObject> getValueObjects( GeneService service );

    /**
     * @param service service that will be used to retrieve the persistent Gene object.
     * @return a collection of Gene value objects..
     */
    public GeneValueObject getGeneOnTaxon( GeneService service, TaxonService taxonService, TaxonArg taxonArg ) {
        //noinspection unchecked
        Taxon taxon = ( Taxon ) taxonArg.getPersistentObject( taxonService );
        if ( taxon == null ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.NOT_FOUND,
                    WebService.ERROR_MSG_ENTITY_NOT_FOUND );
            WellComposedErrorBody.addExceptionFields( error, new EntityNotFoundException( taxonArg.getNullCause() ) );
            throw new GemmaApiException( error );
        }

        Gene gene = this.getPersistentObject( service );
        if ( gene == null )
            return null;
        gene = service.findByOfficialSymbol( gene.getOfficialSymbol(), taxon );
        if ( gene == null )
            return null;
        return service.loadValueObject( gene );
    }

    public Object getGeneEvidence( GeneService geneService,
            PhenotypeAssociationManagerService phenotypeAssociationManagerService, Taxon taxon,
            HttpServletResponse sr ) {
        Gene gene = this.getPersistentObject( geneService );
        if ( gene == null ) {
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.NOT_FOUND, this.getNullCause() );
            return Responder.code( error.getStatus(), error, sr );
        }
        return phenotypeAssociationManagerService
                .findGenesWithEvidence( gene.getOfficialSymbol(), taxon == null ? null : taxon.getId() );
    }
}
