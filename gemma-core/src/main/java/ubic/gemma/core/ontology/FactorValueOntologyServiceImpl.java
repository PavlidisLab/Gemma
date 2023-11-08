package ubic.gemma.core.ontology;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            return createIndividualFromAnnotation( uri, annotation );
        } else {
            FactorValue fv = factorValueService.loadWithExperimentalFactor( fvId );
            if ( fv == null ) {
                return null;
            }
            OntologyTermSimple instanceOf;
            if ( fv.getExperimentalFactor() != null && fv.getExperimentalFactor().getCategory() != null ) {
                String categoryUri = fv.getExperimentalFactor().getCategory().getCategoryUri();
                String category = fv.getExperimentalFactor().getCategory().getCategory();
                instanceOf = new OntologyTermSimple( categoryUri, category );
            } else {
                instanceOf = null;
            }
            return new OntologyIndividualSimple( uri, FactorValueUtils.getSummaryString( fv ), instanceOf );
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
            individuals.add( createIndividualFromAnnotation( e.getKey(), e.getValue() ) );
        }
        return individuals;
    }

    @Override
    public Set<OntologyStatement> getRelatedStatements( String uri ) {
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
        Map<String, Annotation> individuals = getAnnotationsById( fv );
        Set<StatementValueObject> statements = fv.getCharacteristics().stream().map( StatementValueObject::new ).collect( Collectors.toSet() );
        Set<OntologyStatement> ontStatements = new HashSet<>();
        FactorValueOntologyUtils.visitStatements( fvId, statements, ( svo, ids ) -> {
            Annotation subjectId = individuals.get( ids.getSubjectId() );
            OntologyIndividual subject = createIndividualFromAnnotation( ids.getSubjectId(), subjectId );
            if ( ids.getObjectId() != null ) {
                Annotation objectId = individuals.get( ids.getObjectId() );
                OntologyIndividual object = createIndividualFromAnnotation( ids.getObjectId(), objectId );
                ontStatements.add( new OntologyStatement( subject, new OntologyPropertySimple( svo.getPredicateUri(), svo.getPredicate() ), object ) );
            }
            if ( ids.getSecondObjectId() != null ) {
                Annotation secondObjectId = individuals.get( ids.getSecondObjectId() );
                OntologyIndividual object = createIndividualFromAnnotation( ids.getSecondObjectId(), secondObjectId );
                ontStatements.add( new OntologyStatement( subject, new OntologyPropertySimple( svo.getPredicateUri(), svo.getPredicate() ), object ) );
            }
        } );
        return ontStatements;
    }

    private OntologyIndividual createIndividualFromAnnotation( String uri, Annotation annotation ) {
        return new OntologyIndividualSimple( uri, annotation.getLabel(), new OntologyTermSimple( annotation.getUri(), annotation.getLabel() ) );
    }
}
