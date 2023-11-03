package ubic.gemma.core.ontology;

import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import javax.annotation.Nullable;
import java.util.*;

@Service
public class FactorValueOntologyServiceImpl implements FactorValueOntologyService {

    @Autowired
    private FactorValueService factorValueService;

    @Override
    @Nullable
    public OntologyIndividual getIndividual( String uri ) {
        if ( !uri.startsWith( URI_PREFIX ) ) {
            return null;
        }
        String t = uri.replaceFirst( URI_PREFIX, "" );
        String[] pieces = t.split( "/", 2 );
        try {
            final FactorValue fv = factorValueService.loadWithExperimentalFactor( Long.parseLong( pieces[0] ) );
            if ( fv == null ) {
                return null;
            }
            if ( pieces.length == 2 ) {
                long id = Long.parseLong( pieces[1] );
                return getAnnotationById( fv, Long.parseLong( pieces[1] ) )
                        .map( a -> new FactorValueAnnotationOntologyIndividual( fv, id, a.getUri(), a.getLabel() ) )
                        .orElse( null );
            } else {
                return new FactorValueOntologyIndividual( fv );
            }
        } catch ( NumberFormatException e ) {
            return null;
        }
    }

    @Override
    public Set<OntologyIndividual> getRelatedIndividuals( String uri ) {
        if ( !uri.startsWith( URI_PREFIX ) ) {
            return null;
        }
        String t = uri.replaceFirst( URI_PREFIX, "" );
        String[] pieces = t.split( "/", 2 );
        final FactorValue fv = factorValueService.loadWithExperimentalFactor( Long.parseLong( pieces[0] ) );
        if ( fv == null ) {
            return null;
        }
        HashSet<OntologyIndividual> individuals = new HashSet<>();
        for ( Map.Entry<Long, Annotation> e : getAnnotationsById( fv ).entrySet() ) {
            individuals.add( new FactorValueAnnotationOntologyIndividual( fv, e.getKey(), e.getValue().getUri(), e.getValue().getLabel() ) );
        }
        return individuals;
    }

    @Value
    private static class Annotation {
        String label;
        @Nullable
        String uri;
    }

    private Map<Long, Annotation> getAnnotationsById( FactorValue fv ) {
        Map<Long, Annotation> result = new HashMap<>();
        long nextAvailableId = 1L;
        for ( Statement s : new TreeSet<>( fv.getCharacteristics() ) ) {
            result.put( nextAvailableId++, new Annotation( s.getSubject(), s.getSubjectUri() ) );
            if ( s.getObject() != null ) {
                result.put( nextAvailableId++, new Annotation( s.getObject(), s.getObjectUri() ) );
            }
            if ( s.getSecondObject() != null ) {
                result.put( nextAvailableId++, new Annotation( s.getSecondObject(), s.getSecondObjectUri() ) );
            }
        }
        return result;
    }

    private Optional<Annotation> getAnnotationById( FactorValue fv, long annotationId ) {
        long nextAvailableId = 1L;
        for ( Statement s : new TreeSet<>( fv.getCharacteristics() ) ) {
            if ( annotationId == nextAvailableId++ ) {
                return Optional.of( new Annotation( s.getSubject(), s.getSubjectUri() ) );
            }
            if ( s.getObject() != null ) {
                if ( annotationId == nextAvailableId++ ) {
                    return Optional.of( new Annotation( s.getObject(), s.getObjectUri() ) );
                }
            }
            if ( s.getSecondObject() != null ) {
                if ( annotationId == nextAvailableId++ ) {
                    return Optional.of( new Annotation( s.getSecondObject(), s.getSecondObjectUri() ) );
                }
            }
        }
        return Optional.empty();
    }
}
