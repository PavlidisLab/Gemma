package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaxonArgService extends AbstractEntityArgService<Taxon, TaxonService> {
    @Autowired
    public TaxonArgService( TaxonService service ) {
        super( service );
    }

    @Override
    public <A> Filters getFilters( AbstractEntityArg<A, Taxon, TaxonService> entityArg ) throws BadRequestException {
        if ( entityArg instanceof TaxonNameArg ) {
            return Filters.by(
                    service.getFilter( "commonName", String.class, Filter.Operator.eq, ( String ) entityArg.getValue() ),
                    service.getFilter( "scientificName", String.class, Filter.Operator.eq, ( String ) entityArg.getValue() ) );
        } else {
            return super.getFilters( entityArg );
        }
    }

    @Override
    protected Map<String, List<String>> getArgsByPropertyName( AbstractEntityArrayArg<Taxon, TaxonService> entitiesArg ) {
        Map<String, List<String>> argsByPropertyName = new HashMap<>();
        for ( String v : entitiesArg.getValue() ) {
            AbstractEntityArg<?, Taxon, TaxonService> arg = entityArgValueOf( entitiesArg.getEntityArgClass(), v );
            if ( arg instanceof TaxonNameArg ) {
                argsByPropertyName.computeIfAbsent( "commonName", ( k ) -> new ArrayList<>() ).add( v );
                argsByPropertyName.computeIfAbsent( "scientificName", ( k ) -> new ArrayList<>() ).add( v );
            } else {
                argsByPropertyName.computeIfAbsent( arg.getPropertyName(), ( k ) -> new ArrayList<>() ).add( v );
            }
        }
        return argsByPropertyName;
    }
}
