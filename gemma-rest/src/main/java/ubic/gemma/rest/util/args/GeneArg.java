package ubic.gemma.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.rest.util.MalformedArgException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mutable argument type base class for Gene API.
 *
 * @author tesarst
 */
@Schema(oneOf = { GeneEnsemblIdArg.class, GeneNcbiIdArg.class, GeneSymbolArg.class })
public abstract class GeneArg<T> extends AbstractEntityArg<T, Gene, GeneService> {

    private static final String ENSEMBL_ID_REGEX = "(ENSTBE|MGP_BALBcJ_|MGP_PWKPhJ_|ENSMUS|MGP_129S1SvImJ_|"
            + "ENSSHA|ENSPFO|ENSRNO|FB|MGP_NODShiLtJ_|ENSLAF|ENSOAN|MGP_FVBNJ_|ENSDAR|ENSSSC|ENSGGO|ENSAMX|"
            + "ENSXMA|ENSCHO|ENSGAC|ENSDOR|MGP_CASTEiJ_|ENSGMO|ENSTSY|ENSAME|ENSLOC|MGP_LPJ_|ENSCPO|ENSPAN|"
            + "ENSTRU|ENSNLE|ENSPCA|ENSXET|ENSDNO|MGP_AJ_|MGP_DBA2J_|ENSMPU|ENSMOD|ENSVPA|ENS|ENSMMU|ENSOCU|"
            + "MGP_CBAJ_|MGP_NZOHlLtJ_|ENSSCE|ENSOPR|ENSACA|ENSCSA|ENSORL|ENSCSAV|ENSTNI|ENSECA|MGP_C3HHeJ_|"
            + "ENSCEL|ENSFAL|ENSPSI|ENSAPL|ENSCAF|MGP_SPRETEiJ_|ENSLAC|MGP_C57BL6NJ_|ENSSAR|ENSBTA|ENSMIC|"
            + "ENSEEU|ENSTTR|ENSOGA|ENSMLU|ENSSTO|ENSCIN|MGP_WSBEiJ_|ENSMEU|ENSPVA|ENSPMA|ENSPTR|ENSFCA|"
            + "ENSPPY|ENSMGA|ENSOAR|ENSCJA|ENSETE|ENSTGU|MGP_AKRJ_|ENSONI|ENSGAL).*";

    protected GeneArg( String propertyName, Class<T> propertyType, T value ) {
        super( propertyName, propertyType, value );
    }


    List<Gene> getEntitiesWithTaxon( GeneService service, Taxon taxon ) {
        return getEntities( service ).stream()
                .filter( gene -> gene.getTaxon().equals( taxon ) )
                .collect( Collectors.toList() );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request Gene argument
     * @return instance of appropriate implementation of GeneArg based on the actual property the argument represents.
     */
    @SuppressWarnings("unused")
    public static GeneArg<?> valueOf( final String s ) {
        if ( StringUtils.isBlank( s ) ) {
            throw new MalformedArgException( "Gene identifier cannot be null or empty." );
        }
        try {
            return new GeneNcbiIdArg( Integer.parseInt( s.trim() ) );
        } catch ( NumberFormatException e ) {
            if ( s.matches( GeneArg.ENSEMBL_ID_REGEX ) ) {
                return new GeneEnsemblIdArg( s );
            } else {
                return new GeneSymbolArg( s );
            }
        }
    }
}
