package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.MalformedArgException;

/**
 * Mutable argument type base class for Taxon API
 *
 * @author tesarst
 */
@Schema(oneOf = { TaxonIdArg.class, TaxonNcbiIdArg.class, TaxonNameArg.class })
public abstract class TaxonArg<T> extends AbstractEntityArg<T, Taxon, TaxonService> {

    /**
     * Minimum value to be considered an NCBI ID, lower values will be considered a regular gemma Taxon ID.
     */
    private static final Long MIN_NCBI_ID = 999L;

    protected TaxonArg( String propertyName, Class<T> propertyType, T arg ) {
        super( propertyName, propertyType, arg );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request taxon argument
     * @return instance of appropriate implementation of TaxonArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static TaxonArg<?> valueOf( final String s ) throws MalformedArgException {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Taxon identifier cannot be null or empty.", null );
        }
        try {
            long id = Long.parseLong( s.trim() );
            return id > TaxonArg.MIN_NCBI_ID ? new TaxonNcbiIdArg( ( int ) id ) : new TaxonIdArg( id );
        } catch ( NumberFormatException e ) {
            return new TaxonNameArg( s );
        }
    }
}
