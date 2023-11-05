package ubic.gemma.core.ontology;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ubic.gemma.core.ontology.FactorValueOntologyUtils.*;

@Service
public class FactorValueOntologyServiceImpl implements FactorValueOntologyService {

    @Autowired
    private FactorValueService factorValueService;

    @Override
    @Nullable
    public OntologyIndividual getIndividual( String uri ) {
        Long fvId = parseUri( uri );
        if ( fvId == null ) {
            return null;
        }
        if ( isAnnotationUri( uri ) ) {
            FactorValue fv = factorValueService.load( fvId );
            if ( fv == null ) {
                return null;
            }
            Annotation annotation = getAnnotationsById( fv ).get( uri );
            if ( annotation == null ) {
                return null;
            }
            return new FactorValueAnnotationOntologyIndividual( uri, annotation.getUri(), annotation.getLabel() );
        } else {
            FactorValue fv = factorValueService.loadWithExperimentalFactor( fvId );
            if ( fv == null ) {
                return null;
            }
            return new FactorValueOntologyIndividual( fv, uri );
        }
    }

    @Override
    public Set<OntologyIndividual> getRelatedIndividuals( String uri ) {
        if ( isAnnotationUri( uri ) ) {
            // this is a specific annotation ID
            return Collections.emptySet();
        }
        Long fvId = parseUri( uri );
        if ( fvId == null ) {
            return Collections.emptySet();
        }
        final FactorValue fv = factorValueService.load( fvId );
        if ( fv == null ) {
            return Collections.emptySet();
        }
        HashSet<OntologyIndividual> individuals = new HashSet<>();
        for ( Map.Entry<String, FactorValueOntologyUtils.Annotation> e : getAnnotationsById( fv ).entrySet() ) {
            individuals.add( new FactorValueAnnotationOntologyIndividual( e.getKey(), e.getValue().getUri(), e.getValue().getLabel() ) );
        }
        return individuals;
    }
}
