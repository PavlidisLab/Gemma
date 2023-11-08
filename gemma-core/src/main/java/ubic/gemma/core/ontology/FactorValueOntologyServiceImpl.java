package ubic.gemma.core.ontology;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.gemma.core.ontology.jena.TGFVO;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import javax.annotation.Nullable;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.ontology.FactorValueOntologyUtils.*;

@Service
public class FactorValueOntologyServiceImpl implements FactorValueOntologyService {

    private final FactorValueService factorValueService;

    @Autowired
    public FactorValueOntologyServiceImpl( FactorValueService factorValueService ) {
        this.factorValueService = factorValueService;
    }

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
    public Set<OntologyIndividual> getFactorValueAnnotations( String uri ) {
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
        if ( fv.getMeasurement() != null ) {
            OntologyTermSimple measurementClass = new OntologyTermSimple( TGFVO.Measurement.getURI(), "measurement" );
            individuals.add( new OntologyIndividualSimple( null, fv.getMeasurement().getValue(), measurementClass ) );
        }
        for ( Map.Entry<String, FactorValueOntologyUtils.Annotation> e : getAnnotationsById( fv ).entrySet() ) {
            individuals.add( createIndividualFromAnnotation( e.getKey(), e.getValue() ) );
        }
        return individuals;
    }

    @Override
    public Set<OntologyStatement> getFactorValueStatements( String uri ) {
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

    /**
     * Write a small RDF model for a given factor value or annotation.
     */
    @Override
    public void writeToRdf( String uri, Writer writer ) {
        OntModel ontModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        ontModel.setNsPrefix( "obo", "http://purl.obolibrary.org/obo/" );
        ontModel.setNsPrefix( "efo", "http://www.ebi.ac.uk/efo/" );
        ontModel.setNsPrefix( "tgemo", "http://gemma.msl.ubc.ca/ont/" );
        ontModel.setNsPrefix( "tgfvo", TGFVO.NS );
        createFactorValueOrAnnotationIndividual( ontModel, uri );
        ontModel.write( writer, "RDF/XML-ABBREV" );
    }

    private void createFactorValueOrAnnotationIndividual( OntModel ontModel, String uri ) {
        Long fvId = parseUri( uri );
        if ( fvId == null ) {
            return;
        }

        FactorValue fv = factorValueService.loadWithExperimentalFactor( fvId );
        if ( fv == null ) {
            return;
        }

        Map<String, Annotation> annotationsById = getAnnotationsById( fv );

        // this is a specific annotation ID
        if ( isAnnotationUri( uri ) ) {
            Annotation annotation = annotationsById.get( uri );
            createIndividual( ontModel, uri, annotation.getUri(), annotation.getLabel() );
            return;
        }

        OntClass fvClass;
        if ( fv.getExperimentalFactor().getCategory() != null ) {
            fvClass = ontModel.createClass( fv.getExperimentalFactor().getCategory().getCategoryUri() );
            fvClass.setLabel( fv.getExperimentalFactor().getCategory().getCategory(), null );
        } else {
            fvClass = null;
        }
        Individual fvI = ontModel.createIndividual( uri, fvClass );

        if ( fv.getMeasurement() == null && annotationsById.isEmpty() ) {
            return;
        }

        // create basic definitions
        ObjectProperty hasAnnotation = ontModel.createObjectProperty( TGFVO.hasAnnotation.getURI() );
        hasAnnotation.setLabel( "has annotation", null );
        ObjectProperty annotationOf = ontModel.createObjectProperty( TGFVO.annotationOf.getURI() );
        annotationOf.setLabel( "annotation of", null );
        annotationOf.setInverseOf( hasAnnotation );
        hasAnnotation.setInverseOf( annotationOf );

        // extra definitions needed
        if ( fv.getMeasurement() != null ) {
            ObjectProperty hasMeasurement = ontModel.createObjectProperty( TGFVO.hasMeasurement.getURI() );
            hasMeasurement.setLabel( "has measurement", null );
            ObjectProperty measurementOf = ontModel.createObjectProperty( TGFVO.measurementOf.getURI() );
            measurementOf.setLabel( "measurement of", null );
            hasMeasurement.addSuperProperty( hasAnnotation );
            hasMeasurement.setInverseOf( measurementOf );
            measurementOf.addSuperProperty( annotationOf );
            measurementOf.setInverseOf( hasMeasurement );
            Individual measurement = ontModel.createIndividual( null, TGFVO.Measurement );
            String label = fv.getMeasurement().getValue();
            if ( fv.getMeasurement().getUnit() != null ) {
                label += " " + fv.getMeasurement().getUnit().getUnitNameCV();
            }
            measurement.setLabel( label, null );
            measurement.addProperty( TGFVO.hasRepresentation, fv.getMeasurement().getRepresentation().name() );
            if ( fv.getMeasurement().getUnit() != null ) {
                measurement.addProperty( TGFVO.hasUnit, fv.getMeasurement().getUnit().getUnitNameCV() );
            }
            measurement.addProperty( TGFVO.hasValue, fv.getMeasurement().getValue() );
            ontModel.add( fvI, hasMeasurement, measurement );
        }

        for ( Map.Entry<String, Annotation> e : annotationsById.entrySet() ) {
            Individual annot = createIndividual( ontModel, e.getKey(), e.getValue().getUri(), e.getValue().getLabel() );
            ontModel.add( fvI, hasAnnotation, annot );
        }

        List<StatementValueObject> statements = fv.getCharacteristics().stream().map( StatementValueObject::new ).collect( Collectors.toList() );
        FactorValueOntologyUtils.visitStatements( fv.getId(), statements, ( svo, annotationIds ) -> {
            Individual subject = createIndividual( ontModel, annotationIds.getSubjectId(), svo.getCategoryUri(), svo.getCategory() );
            if ( annotationIds.getObjectId() != null ) {
                Individual object = createIndividual( ontModel, annotationIds.getObjectId(), svo.getObjectUri(), svo.getObject() );
                ObjectProperty predicate = ontModel.createObjectProperty( svo.getPredicateUri() );
                predicate.setLabel( svo.getPredicate(), null );
                ontModel.add( subject, predicate, object );
            }
            if ( annotationIds.getSecondObjectId() != null ) {
                Individual object = createIndividual( ontModel, annotationIds.getSecondObjectId(), svo.getSecondObjectUri(), svo.getSecondObject() );
                ObjectProperty predicate = ontModel.createObjectProperty( svo.getSecondPredicateUri() );
                predicate.setLabel( svo.getSecondPredicate(), null );
                ontModel.add( subject, predicate, object );
            }
        } );
    }

    private Individual createIndividual( OntModel ontModel, String uri, String classUri, String classLabel ) {
        OntClass ontClass;
        if ( classUri != null ) {
            ontClass = ontModel.createClass( classUri );
            ontClass.setLabel( classLabel, null );
        } else {
            ontClass = null;
        }
        Individual indI = ontModel.createIndividual( uri, ontClass );
        indI.setLabel( classLabel, null );
        return indI;
    }
}
