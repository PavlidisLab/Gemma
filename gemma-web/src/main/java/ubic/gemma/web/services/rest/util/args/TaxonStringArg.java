package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * Created by tesarst on 16/05/17.
 * String argument type for taxon API, referencing the Taxon scientific name, common name or abbreviation. Can also be null.
 */
public class TaxonStringArg extends TaxonArg<String> {

    TaxonStringArg( String s ) {
        this.value = s;
    }

    /**
     * Tries to retrieve a Taxon object based on its properties. The search order is common name, scientific name, abbreviation.
     *
     * @param service the TaxonService that handles the search.
     * @return a Taxon object, if found, or null, if the original argument value was null, or if the search did not find
     * any Taxon that would match the string.
     */
    @Override
    protected Taxon getPersistentObject( TaxonService service ) {
        return this.value == null ? null : this.tryAllNameProperties( service );
    }

    /**
     * Tries to retrieve a TaxonValueObject object based on its properties.
     *
     * @see #getPersistentObject(TaxonService)
     */
    @Override
    protected TaxonValueObject getValueObject( TaxonService service ) {
        return service.loadValueObject( this.getPersistentObject( service ).getId() );
    }

    /**
     * Tries to retrieve a Taxon based on its names.
     * @param service the TaxonService that handles the search.
     * @return Taxon or null if no taxon with any property matching this#value was found.
     */
    private Taxon tryAllNameProperties(TaxonService service){
        // Most commonly used
        Taxon taxon = service.findByCommonName( this.value );

        if ( taxon == null ) {
            taxon = service.findByScientificName( this.value );
        }

        if ( taxon == null ) {
            taxon = service.findByAbbreviation( this.value );
        }

        return taxon;
    }
}
