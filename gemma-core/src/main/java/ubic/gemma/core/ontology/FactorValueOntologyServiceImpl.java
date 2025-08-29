package ubic.gemma.core.ontology;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyStatement;
import ubic.basecode.ontology.simple.OntologyIndividualSimple;
import ubic.basecode.ontology.simple.OntologyPropertySimple;
import ubic.basecode.ontology.simple.OntologyStatementSimple;
import ubic.basecode.ontology.simple.OntologyTermSimple;
import ubic.gemma.core.ontology.jena.TGFVO;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.persistence.util.EntityUrlBuilder;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.QueryUtils;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.ontology.FactorValueOntologyUtils.*;
import static ubic.gemma.model.expression.experiment.FactorValueUtils.getSummaryString;

@Service
@CommonsLog
@ParametersAreNonnullByDefault
public class FactorValueOntologyServiceImpl implements FactorValueOntologyService {

    private final FactorValueService factorValueService;
    private final EntityUrlBuilder entityUrlBuilder;

    @Autowired
    public FactorValueOntologyServiceImpl( FactorValueService factorValueService, EntityUrlBuilder entityUrlBuilder ) {
        this.factorValueService = factorValueService;
        this.entityUrlBuilder = entityUrlBuilder;
    }

    @Override
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
            return createIndividualFromAnnotation( uri, annotation, annotation );
        } else {
            FactorValue fv = factorValueService.loadWithExperimentalFactor( fvId );
            if ( fv == null ) {
                return null;
            }
            return createIndividualFromFactorValue( fv, fv.getExperimentalFactor().getCategory() );
        }
    }

    @Override
    public Slice<OntologyIndividual> getFactorValues( int offset, int limit ) {
        Slice<FactorValue> factors = factorValueService.loadAll( offset, limit );
        Map<FactorValue, Characteristic> f2c = factorValueService.getExperimentalFactorCategoriesIgnoreAcls( factors );
        return factors.map( fv -> createIndividualFromFactorValue( fv, f2c.get( fv ) ) );
    }

    @Override
    public Collection<String> getFactorValueUris() {
        Collection<Long> factorIds = factorValueService.loadAllIds();
        return factorIds.stream()
                .map( FactorValueOntologyUtils::getUri )
                .collect( Collectors.toList() );
    }

    @Override
    public Slice<String> getFactorValueUris( int offset, int limit ) {
        return factorValueService.loadAllIds( offset, limit ).map( FactorValueOntologyUtils::getUri );
    }

    @Override
    public Set<OntologyIndividual> getFactorValueAnnotations( String uri ) {
        Assert.isTrue( isFactorValueUri( uri ), "URI must be a factor value URI." );
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
            individuals.add( createIndividualFromAnnotation( e.getKey(), e.getValue(), e.getValue() + " of " + fv ) );
        }
        return individuals;
    }

    @Override
    public Set<OntologyStatement> getFactorValueStatements( String uri ) {
        Assert.isTrue( isFactorValueUri( uri ), "URI must be a factor value URI." );
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
            OntologyIndividual subject = createIndividualFromAnnotation( ids.getSubjectId(), subjectId, "subject of " + svo + " of " + fv );
            if ( ids.getObjectId() != null ) {
                Annotation objectId = individuals.get( ids.getObjectId() );
                OntologyIndividual object = createIndividualFromAnnotation( ids.getObjectId(), objectId, "first object of " + svo + " of " + fv );
                ontStatements.add( new OntologyStatementSimple( subject, new OntologyPropertySimple( svo.getPredicateUri(), svo.getPredicate() ), object ) );
            }
            if ( ids.getSecondObjectId() != null ) {
                Annotation secondObjectId = individuals.get( ids.getSecondObjectId() );
                OntologyIndividual object = createIndividualFromAnnotation( ids.getSecondObjectId(), secondObjectId, "second object of " + svo + " of " + fv );
                ontStatements.add( new OntologyStatementSimple( subject, new OntologyPropertySimple( svo.getPredicateUri(), svo.getPredicate() ), object ) );
            }
        } );
        return ontStatements;
    }

    private OntologyIndividual createIndividualFromFactorValue( FactorValue fv, @Nullable Characteristic category ) {
        String uri = getUri( fv );
        OntologyTermSimple instanceOf;
        if ( category != null && category.getCategoryUri() != null ) {
            instanceOf = new OntologyTermSimple( category.getCategoryUri(), category.getCategory() );
        } else {
            instanceOf = null;
        }
        return new OntologyIndividualSimple( uri, getSummaryString( fv, category, ", " ), instanceOf );
    }

    private OntologyIndividual createIndividualFromAnnotation( @Nullable String uri, Annotation annotation, @Nullable Object source ) {
        checkUri( uri, source );
        OntologyTermSimple instanceOf = annotation.getUri() != null ?
                new OntologyTermSimple( annotation.getUri(), annotation.getLabel() ) : null;
        return new OntologyIndividualSimple( uri, annotation.getLabel(), instanceOf );
    }

    @Override
    public void writeToRdf( Collection<String> uri, Writer writer ) {
        writeToRdf( uri, writer, false );
    }

    @Override
    public void writeToRdfIgnoreAcls( Collection<String> uri, Writer writer ) {
        writeToRdf( uri, writer, true );
    }

    private void writeToRdf( Collection<String> uris, Writer writer, boolean ignoreAcls ) {
        OntModel ontModel = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );

        // add some useful namespaces to shorten URLs
        ontModel.setNsPrefix( "efo", "http://www.ebi.ac.uk/efo/" );
        // FIXME: there are a bunch of ncbi_gene namespaces in the output that shouldn't be there, not sure this is
        //        doing anything
        ontModel.setNsPrefix( "ncbi_gene", "http://purl.org/commons/record/ncbi_gene/" );
        ontModel.setNsPrefix( "obo", "http://purl.obolibrary.org/obo/" );
        ontModel.setNsPrefix( "tgemo", "http://gemma.msl.ubc.ca/ont/" );
        ontModel.setNsPrefix( "tgfvo", TGFVO.NS );

        // create basic definitions
        createObjectProperty( ontModel, TGFVO.belongsTo.getURI(), "belongs to", null );
        ObjectProperty hasAnnotation = createObjectProperty( ontModel, TGFVO.hasAnnotation.getURI(), "has annotation", null );
        ObjectProperty annotationOf = createObjectProperty( ontModel, TGFVO.annotationOf.getURI(), "annotation of", null );
        annotationOf.setInverseOf( hasAnnotation );
        hasAnnotation.setInverseOf( annotationOf );
        ObjectProperty hasMeasurement = createObjectProperty( ontModel, TGFVO.hasMeasurement.getURI(), "has measurement", null );
        ObjectProperty measurementOf = createObjectProperty( ontModel, TGFVO.measurementOf.getURI(), "measurement of", null );
        hasMeasurement.addSuperProperty( hasAnnotation );
        hasMeasurement.setInverseOf( measurementOf );
        measurementOf.addSuperProperty( annotationOf );
        measurementOf.setInverseOf( hasMeasurement );

        int processed = 0;
        StopWatch timer = StopWatch.createStarted();
        for ( List<String> batch : QueryUtils.batchParameterList( uris, QueryUtils.MAX_PARAMETER_LIST_SIZE ) ) {
            createFactorValueOrAnnotationIndividuals( ontModel, batch, ignoreAcls );
            processed += batch.size();
            log.info( String.format( "Processed %d/%d factor value URIs %.2f FV/s.", processed, uris.size(), 1000.0 * processed / timer.getTime() ) );
        }
        ontModel.write( writer, "RDF/XML-ABBREV" );
    }

    private void createFactorValueOrAnnotationIndividuals( OntModel ontModel, Collection<String> uris, boolean ignoreAcls ) {
        Set<Long> fvIds = uris.stream().map( FactorValueOntologyUtils::parseUri ).filter( Objects::nonNull ).collect( Collectors.toSet() );
        Collection<FactorValue> factorValues;
        if ( ignoreAcls ) {
            factorValues = factorValueService.loadIgnoreAcls( fvIds );
        } else {
            factorValues = factorValueService.load( fvIds );
        }
        Map<Long, FactorValue> factorValuesById = IdentifiableUtils.getIdMap( factorValues );
        // we can safely ignore ACLs here since the FVs were already filtered by ACLs
        Map<FactorValue, Characteristic> fv2c = factorValueService.getExperimentalFactorCategoriesIgnoreAcls( factorValues );
        Map<FactorValue, ExpressionExperiment> fv2ee = factorValueService.getExpressionExperimentsIgnoreAcls( factorValues );
        for ( String uri : uris ) {
            try {
                createFactorValueOrAnnotationIndividual( ontModel, uri, factorValuesById, fv2c, fv2ee );
            } catch ( IllegalArgumentException e ) {
                log.warn( "Failed to create ontology term for " + uri + ".", e );
            }
        }
    }

    private void createFactorValueOrAnnotationIndividual( OntModel ontModel, String uri, Map<Long, FactorValue> factorValuesById, Map<FactorValue, Characteristic> fv2c, Map<FactorValue, ExpressionExperiment> fv2ee ) {
        Long fvId = parseUri( uri );
        if ( fvId == null ) {
            return;
        }

        FactorValue fv = factorValuesById.get( fvId );
        if ( fv == null ) {
            return;
        }

        Map<String, Annotation> annotationsById = getAnnotationsById( fv );

        // this is a specific annotation ID
        if ( isAnnotationUri( uri ) ) {
            Annotation annotation = annotationsById.get( uri );
            createIndividual( ontModel, uri, annotation.getUri(), annotation.getLabel(), annotation );
            return;
        }

        ExpressionExperiment ee = fv2ee.get( fv );

        Characteristic category = fv2c.get( fv );
        Individual fvI;
        if ( category != null && category.getCategoryUri() != null ) {
            fvI = createIndividual( ontModel, uri, category.getCategoryUri(), category.getCategory(), category + " of " + fv + " of " + ee );
        } else {
            fvI = createIndividual( ontModel, uri, null, null, category + " of " + fv + " of " + ee );
        }

        if ( ee != null ) {
            Individual dataset = createIndividual( ontModel, entityUrlBuilder.fromHostUrl().entity( ee ).web().toUriString(), TGFVO.Dataset.getURI(), ee.getShortName(), null );
            dataset.addComment( ee.getName(), null );
            fvI.addProperty( TGFVO.belongsTo, dataset );
        } else {
            log.warn( fv + " does not have an associated ExpressionExperiment, skipping the dataset link." );
        }

        if ( fv.getMeasurement() == null && annotationsById.isEmpty() ) {
            return;
        }

        if ( fv.getMeasurement() != null ) {
            Individual measurement = ontModel.createIndividual( null, TGFVO.Measurement );
            String label;
            if ( fv.getMeasurement().getValue() != null ) {
                label = fv.getMeasurement().getValue();
                if ( fv.getMeasurement().getUnit() != null ) {
                    label += " " + fv.getMeasurement().getUnit().getUnitNameCV();
                }
            } else {
                label = "N/A";
            }
            measurement.setLabel( label, null );
            measurement.addProperty( TGFVO.hasRepresentation, fv.getMeasurement().getRepresentation().name() );
            if ( fv.getMeasurement().getUnit() != null ) {
                measurement.addProperty( TGFVO.hasUnit, fv.getMeasurement().getUnit().getUnitNameCV() );
            }
            if ( fv.getMeasurement().getValue() != null ) {
                measurement.addProperty( TGFVO.hasValue, fv.getMeasurement().getValue() );
            }
            ontModel.add( fvI, TGFVO.hasMeasurement, measurement );
        }

        for ( Map.Entry<String, Annotation> e : annotationsById.entrySet() ) {
            Individual annot = createIndividual( ontModel, e.getKey(), e.getValue().getUri(), e.getValue().getLabel(), e.getValue() + " of " + fv + " of " + ee );
            ontModel.add( fvI, TGFVO.hasAnnotation, annot );
        }

        List<StatementValueObject> statements = fv.getCharacteristics().stream().map( StatementValueObject::new ).collect( Collectors.toList() );
        FactorValueOntologyUtils.visitStatements( fv.getId(), statements, ( svo, annotationIds ) -> {
            Individual subject = createIndividual( ontModel, annotationIds.getSubjectId(), svo.getCategoryUri(), svo.getCategory(), "subject of " + svo + " of " + fv + " of " + ee );
            // RDF/XML does not allow "free text" predicates, so we have to skip them
            if ( annotationIds.getObjectId() != null ) {
                if ( svo.getPredicateUri() != null ) {
                    ObjectProperty predicate = createObjectProperty( ontModel, svo.getPredicateUri(), svo.getPredicate(), "first predicate of " + svo + " of " + fv + " of " + ee );
                    if ( svo.getObjectUri() != null ) {
                        Individual object = createIndividual( ontModel, annotationIds.getObjectId(), svo.getObjectUri(), svo.getObject(), "first object of " + svo + " of " + fv + " of " + ee );
                        ontModel.add( subject, predicate, object );
                    } else {
                        subject.addProperty( predicate, svo.getObject() );
                    }
                } else {
                    log.warn( "The first predicate of " + svo + " of " + fv + " is free-text, skipping it." );
                }
            }
            if ( annotationIds.getSecondObjectId() != null ) {
                if ( svo.getSecondPredicateUri() != null ) {
                    ObjectProperty predicate = createObjectProperty( ontModel, svo.getSecondPredicateUri(), svo.getSecondPredicate(), "second predicate of " + svo + " of " + fv + " of " + ee );
                    if ( svo.getSecondObjectUri() != null ) {
                        Individual object = createIndividual( ontModel, annotationIds.getSecondObjectId(), svo.getSecondObjectUri(), svo.getSecondObject(), "second object of " + svo + " of " + fv + " of " + ee );
                        ontModel.add( subject, predicate, object );
                    } else {
                        subject.addProperty( predicate, svo.getSecondObject() );
                    }
                } else {
                    log.warn( "The second predicate of " + svo + " of " + fv + " is free-text, skipping it." );
                }
            }
        } );
    }

    private Individual createIndividual( OntModel ontModel, String uri, @Nullable String classUri, @Nullable String label, @Nullable Object source ) {
        checkUri( uri, source );
        OntClass ontClass;
        if ( classUri != null ) {
            ontClass = createClass( ontModel, classUri, label, source );
        } else {
            ontClass = null;
        }
        Individual indI = ontModel.createIndividual( uri, ontClass );
        if ( label != null ) {
            indI.setLabel( label, null );
        }
        return indI;
    }

    private OntClass createClass( OntModel ontModel, String uri, @Nullable String label, @Nullable Object source ) {
        checkUri( uri, source );
        OntClass ontClass = ontModel.createClass( uri );
        if ( label != null ) {
            ontClass.setLabel( label, null );
        }
        return ontClass;
    }

    private ObjectProperty createObjectProperty( OntModel ontModel, String uri, @Nullable String label, @Nullable Object source ) {
        checkUri( uri, source );
        ObjectProperty predicate = ontModel.createObjectProperty( uri );
        if ( predicate != null ) {
            predicate.setLabel( label, null );
        }
        return predicate;
    }

    private void checkUri( @Nullable String s, @Nullable Object source ) {
        if ( s != null && !s.startsWith( "http://" ) && !s.startsWith( "https://" ) ) {
            throw new IllegalArgumentException( "Invalid URI: '" + s + "'" + ( source != null ? " for " + source : "" ) + "." );
        }
    }
}
