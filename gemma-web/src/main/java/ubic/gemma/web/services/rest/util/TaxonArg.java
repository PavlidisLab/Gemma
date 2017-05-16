package ubic.gemma.web.services.rest.util;

import ubic.gemma.core.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Taxon;

/**
 * Created by tesarst on 16/05/17.
 * Mutable argument type base class for Taxon API
 */
public abstract class TaxonArg<T> extends MutableType<T> {

    /**
     * Used by RS to parse value of request parameters.
     * @param s the request taxon argument
     * @return instance of appropriate implementation of TaxonArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static TaxonArg valueOf( final String s ) {
        try {
            return new TaxonIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new TaxonStringArg( s );
        }
    }

    public static Taxon getTaxon( final TaxonArg taxonArg, TaxonService service ) {
        try {
            return taxonArg.getTaxon( service );
        }catch(NullPointerException e){
            return null;
        }
    }

    /**
     * Calls appropriate backend logic to retrieve a Taxon.
     *
     * @return a Taxon object matching the value of the original argument this object was created with.
     * @see TaxonIdArg#getTaxon(TaxonService)
     * @see TaxonStringArg#getTaxon(TaxonService)
     */
    protected abstract Taxon getTaxon( TaxonService service );

}
