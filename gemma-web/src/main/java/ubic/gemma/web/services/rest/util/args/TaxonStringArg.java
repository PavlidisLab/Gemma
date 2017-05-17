package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.model.genome.Taxon;

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
    protected Taxon getTaxon( TaxonService service ) {
        // Case when the original API argument was null, we can skip the search.
        if ( this.value == null )
            return null;

        // Most commonly used
        Taxon taxon = service.findByCommonName( this.value );
        if ( taxon == null ) {
            taxon = service.findByScientificName( this.value );
        }
        if ( taxon == null ) {
            taxon = service.findByAbbreviation( this.value );
        }

        if ( taxon != null ) {
            System.out.println( "Found taxon id: " + taxon.getId() );
        } else {
            System.out.println( "No taxons found for String value: " + value );
        }

        return taxon;
    }
}
