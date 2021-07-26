package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * String argument type for taxon API, referencing the Taxon scientific name or common name. Can also be
 * null.
 *
 * @author tesarst
 */
@Schema(implementation = String.class)
public class TaxonStringArg extends TaxonArg<String> {

    TaxonStringArg( String s ) {
        super( s );
    }

    @Override
    public Taxon getEntity( TaxonService service ) {
        return checkEntity( this.getValue() == null ? null : this.tryAllNameProperties( service ) );
    }

    @Override
    public String getPropertyName() {
        return "commonName or scientificName";
    }

    /**
     * Tries to retrieve a Taxon based on its names.
     *
     * @param  service the TaxonService that handles the search.
     * @return Taxon or null if no taxon with any property matching this#value was found.
     */
    private Taxon tryAllNameProperties( TaxonService service ) {
        // Most commonly used
        Taxon taxon = service.findByCommonName( this.getValue() );

        if ( taxon == null ) {
            taxon = service.findByScientificName( this.getValue() );
        }

        return taxon;
    }
}
