/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.util.iterator.WrappedIterator;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.jena.reasoner.ReasonerRegistry.makeDirect;

class JenaUtils {

    protected static final Logger log = LoggerFactory.getLogger( JenaUtils.class );

    /**
     * Safely convert a {@link RDFNode} to a target class.
     */
    public static <T extends RDFNode> Optional<T> as( RDFNode resource, Class<T> clazz ) {
        if ( !resource.canAs( clazz ) ) {
            return Optional.empty();
        }
        try {
            return Optional.of( resource.as( clazz ) );
        } catch ( ConversionException e ) {
            log.warn( "Conversion of {} to {} failed.", resource, clazz.getName() );
            return Optional.empty();
        }
    }

    public static Collection<OntClass> getParents( OntModel model, Collection<OntClass> ontClasses, boolean direct, @Nullable Set<Restriction> additionalRestrictions ) {
        Collection<OntClass> parents = getParentsInternal( model, ontClasses, direct, additionalRestrictions );
        if ( shouldRevisit( parents, direct, model, additionalRestrictions ) ) {
            // if there are some missing direct parents, revisit the hierarchy
            Set<OntClass> parentsToRevisit = new HashSet<>( parents );
            while ( !parentsToRevisit.isEmpty() ) {
                log.debug( "Revisiting the direct parents of {} terms...", parentsToRevisit.size() );
                parentsToRevisit = new HashSet<>( getParentsInternal( model, parentsToRevisit, true, additionalRestrictions ) );
                parentsToRevisit.removeAll( parents );
                log.debug( "Found {} missed parents.", parentsToRevisit.size() );
                parents.addAll( parentsToRevisit );
            }
        }
        return parents;
    }

    private static Collection<OntClass> getParentsInternal( OntModel model, Collection<OntClass> ontClasses, boolean direct, @Nullable Set<Restriction> additionalRestrictions ) {
        ontClasses = ontClasses.stream()
            .map( t -> t.inModel( model ) )
            .filter( t -> t.canAs( OntClass.class ) )
            .map( t -> as( t, OntClass.class ) )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .collect( Collectors.toSet() );
        if ( ontClasses.isEmpty() ) {
            return Collections.emptySet();
        }
        Iterator<OntClass> it = ontClasses.iterator();
        ExtendedIterator<OntClass> iterator = it.next().listSuperClasses( direct );
        while ( it.hasNext() ) {
            iterator = iterator.andThen( it.next().listSuperClasses( direct ) );
        }

        Collection<OntClass> result = new HashSet<>();
        while ( iterator.hasNext() ) {
            OntClass c = iterator.next();

            // handles part of some {parent container} or part of all {parent container}
            if ( additionalRestrictions != null && !additionalRestrictions.isEmpty() && c.isRestriction() ) {
                Restriction r = c.asRestriction();
                if ( additionalRestrictions.contains( r ) ) {
                    Resource value = getRestrictionValue( r );
                    if ( value instanceof OntClass ) {
                        c = ( OntClass ) value;
                    } else {
                        continue;
                    }
                }
            }

            // bnode
            if ( c.getURI() == null )
                continue;

            // owl:Thing
            if ( c.equals( model.getProfile().THING() ) )
                continue;

            result.add( c );
        }

        return result;
    }

    public static Collection<OntClass> getChildren( OntModel model, Collection<OntClass> terms, boolean direct, @Nullable Set<Restriction> additionalRestrictions ) {
        Collection<OntClass> children = getChildrenInternal( model, terms, direct, additionalRestrictions );
        if ( shouldRevisit( children, direct, model, additionalRestrictions ) ) {
            // if there are some missing direct children, revisit the hierarchy
            Set<OntClass> childrenToRevisit = new HashSet<>( children );
            while ( !childrenToRevisit.isEmpty() ) {
                log.debug( "Revisiting the direct parents of {} terms...", childrenToRevisit.size() );
                childrenToRevisit = new HashSet<>( JenaUtils.getChildrenInternal( model, childrenToRevisit, true, additionalRestrictions ) );
                childrenToRevisit.removeAll( children );
                log.debug( "Found {} missed children.", childrenToRevisit.size() );
                children.addAll( childrenToRevisit );
            }
        }
        return children;
    }

    public static Collection<OntClass> getChildrenInternal( OntModel model, Collection<OntClass> terms, boolean direct, @Nullable Set<Restriction> additionalRestrictions ) {
        Set<OntClass> termsSet = terms.stream()
            .map( t -> t.inModel( model ) )
            .filter( t -> t.canAs( OntClass.class ) )
            .map( t -> as( t, OntClass.class ) )
            .filter( Optional::isPresent )
            .map( Optional::get )
            .collect( Collectors.toSet() );
        if ( termsSet.isEmpty() ) {
            return Collections.emptySet();
        }
        StopWatch timer = StopWatch.createStarted();
        Iterator<OntClass> it = termsSet.iterator();
        ExtendedIterator<OntClass> iterator = it.next().listSubClasses( direct );
        while ( it.hasNext() ) {
            iterator = iterator.andThen( it.next().listSubClasses( direct ) );
        }
        Set<OntClass> result = iterator
            .filterDrop( new BnodeFilter<>() )
            .filterDrop( new PredicateFilter<>( o -> o.equals( model.getProfile().NOTHING() ) ) )
            .toSet();
        if ( additionalRestrictions != null && !additionalRestrictions.isEmpty() ) {
            timer.reset();
            timer.start();
            Property subClassOf = model.getProfile().SUB_CLASS_OF();
            if ( direct ) {
                subClassOf = ResourceFactory.createProperty( makeDirect( subClassOf.getURI() ) );
            }
            // Phase 3 Jena 4 port: UniqueExtendedIterator was removed; use WrappedIterator + UniqueFilter.
            Set<Restriction> restrictions = WrappedIterator.create( additionalRestrictions.iterator() )
                .filterKeep( new UniqueFilter<>() )
                .filterKeep( new RestrictionWithValuesFromFilter( termsSet ) )
                .toSet();
            for ( Restriction r : restrictions ) {
                result.addAll( model.listResourcesWithProperty( subClassOf, r )
                    .filterDrop( new BnodeFilter<>() )
                    .mapWith( r2 -> as( r2, OntClass.class ) )
                    .filterKeep( new PredicateFilter<Optional<OntClass>>( Optional::isPresent ) )
                    .mapWith( Optional::get )
                    .toSet() );
            }
        }
        return result;
    }

    /**
     * Check if a set of terms should be revisited to find missing parents or children.
     * <p>
     * To be considered, the model must have a reasoner that lacks one of {@code rdfs:subClassOf}, {@code owl:subValuesFrom}
     * or {@code owl:allValuesFrom} inference capabilities. If a model has no reasoner, revisiting is not desirable and
     * thus this will return false.
     * <p>
     * If direct is false or terms is empty, it's not worth revisiting.
     */
    private static boolean shouldRevisit( Collection<OntClass> terms, boolean direct, OntModel model, @Nullable Set<Restriction> additionalRestrictions ) {
        return !direct
            && !terms.isEmpty()
            && additionalRestrictions != null
            && !additionalRestrictions.isEmpty()
            && model.getReasoner() != null
            && ( !supportsSubClassInference( model ) || !supportsAdditionalRestrictionsInference( model ) );
    }

    /**
     * Check if an ontology model supports inference of {@code rdfs:subClassOf}.
     */
    public static boolean supportsSubClassInference( OntModel model ) {
        return model.getReasoner() != null
            && model.getReasoner().supportsProperty( model.getProfile().SUB_CLASS_OF() );
    }

    /**
     * Checks if an ontology model supports inference of with additional restrictions.
     * <p>
     * This covers {@code owl:subValuesFrom} and {@code owl:allValuesFrom} restrictions.
     */
    public static boolean supportsAdditionalRestrictionsInference( OntModel model ) {
        return model.getReasoner() != null
            && model.getReasoner().supportsProperty( model.getProfile().SOME_VALUES_FROM() )
            && model.getReasoner().supportsProperty( model.getProfile().ALL_VALUES_FROM() );
    }

    public static Resource getRestrictionValue( Restriction r ) {
        if ( r.isSomeValuesFromRestriction() ) {
            return r.asSomeValuesFromRestriction().getSomeValuesFrom();
        } else if ( r.isAllValuesFromRestriction() ) {
            return r.asAllValuesFromRestriction().getAllValuesFrom();
        } else {
            return null;
        }
    }

    /**
     * List all restrictions in the given model on any of the given properties.
     */
    public static ExtendedIterator<Restriction> listRestrictionsOnProperties( OntModel model, Set<? extends Property> props, boolean includeSubProperties ) {
        if ( includeSubProperties ) {
            Set<Property> allProps = new HashSet<>( props );
            for ( Property p : props ) {
                Property property = p.inModel( model );
                // include sub-properties for inference
                as( property, OntProperty.class ).ifPresent( op -> {
                    ExtendedIterator<? extends OntProperty> it = op.listSubProperties( false );
                    while ( it.hasNext() ) {
                        OntProperty sp = it.next();
                        allProps.add( sp );
                    }
                } );
            }
            props = allProps;
        }
        return model.listRestrictions().filterKeep( new RestrictionWithOnPropertyFilter( props ) );
    }
}
