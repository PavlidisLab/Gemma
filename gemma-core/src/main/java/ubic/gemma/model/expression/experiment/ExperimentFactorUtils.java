package ubic.gemma.model.expression.experiment;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;

import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public class ExperimentFactorUtils {

    /**
     * A list of all categories considered to be batch.
     */
    public static final List<Category> BATCH_FACTOR_CATEGORIES = Collections.singletonList( Categories.BLOCK );

    /**
     * Name used by a batch factor.
     * <p>
     * This is used only if the factor lacks a category.
     */
    public static final String BATCH_FACTOR_NAME = "batch";

    /**
     * Check if a factor is a batch factor.
     */
    public static boolean isBatchFactor( ExperimentalFactor ef ) {
        if ( ef.getType().equals( FactorType.CONTINUOUS ) ) {
            return false;
        }
        Characteristic category = ef.getCategory();
        if ( category != null ) {
            return BATCH_FACTOR_CATEGORIES.stream()
                    .anyMatch( c -> CharacteristicUtils.hasCategory( category, c ) );
        }
        return BATCH_FACTOR_NAME.equalsIgnoreCase( ef.getName() );
    }

    /**
     * Check if a given factor VO is a batch factor.
     */
    public static boolean isBatchFactor( ExperimentalFactorValueObject ef ) {
        if ( ef.getType().equals( FactorType.CONTINUOUS.name() ) ) {
            return false;
        }
        String category = ef.getCategory();
        String categoryUri = ef.getCategoryUri();
        if ( category != null ) {
            return BATCH_FACTOR_CATEGORIES.stream()
                    .anyMatch( c -> CharacteristicUtils.equals( category, categoryUri, c.getCategory(), c.getCategoryUri() ) );
        }
        return BATCH_FACTOR_NAME.equalsIgnoreCase( ef.getName() );
    }

    /**
     * Check if two experimental factors are compatible, i.e. they have the same type and category and {@code a}
     * contains a subset of the factor values of {@code b}.
     * <p>
     * The main reason to check compatibility is to see if it is possible to keep the current factor and avoid removing a factor used in DEA avoid removing a factor that used in a DEA.
     */
    public static boolean isCompatibleWith( ExperimentalFactor a, ExperimentalFactor b ) {
        Assert.isTrue( a.getId() == null || b.getId() == null, "At least one transient factor must be supplied." );
        Assert.isTrue( a.getType() == FactorType.CATEGORICAL && b.getType() == FactorType.CATEGORICAL,
                "Only categorical factor can be tested for compatibility." );
        return Objects.equals( a.getName(), b.getName() )
                // description can differ
                && Objects.equals( a.getType(), b.getType() )
                && Objects.equals( a.getCategory(), b.getCategory() )
                && isCompatibleWith( a.getFactorValues(), b.getFactorValues() );
    }

    private static boolean isCompatibleWith( Set<FactorValue> a, Set<FactorValue> b ) {
        Set<FactorValue> seenB = new HashSet<>();
        for ( FactorValue fv : a ) {
            boolean found = false;
            for ( FactorValue fv2 : b ) {
                if ( isCompatibleWith( fv, fv2 ) ) {
                    if ( found ) {
                        log.warn( fv + " matches more than one factor value in\n\t" + b.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) );
                        return false;
                    }
                    if ( !seenB.add( fv2 ) ) {
                        log.warn( fv2 + " is matched by more than one factor value in " + a + "." );
                        return false;
                    }
                    found = true;
                }
            }
            if ( !found ) {
                log.warn( "No matching factor value found for " + fv + " in:\n\t" + b.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) )
                ;
                return false;
            }
        }
        return true;
    }

    private static boolean isCompatibleWith( FactorValue fv, FactorValue fv2 ) {
        return isCompatibleWithS( fv.getCharacteristics(), fv2.getCharacteristics() )
                && Objects.equals( fv.getValue(), fv2.getValue() );
    }

    private static boolean isCompatibleWithS( Set<Statement> characteristics, Set<Statement> characteristics1 ) {
        return populate( characteristics ).equals( populate( characteristics1 ) );
    }

    private static Set<StatementModel> populate( Set<Statement> characteristics ) {
        Set<StatementModel> models = new HashSet<>();
        for ( Statement s : characteristics ) {
            for ( int i = 0; i < s.getNumberOfStatements(); i++ ) {
                models.add( new StatementModel(
                        s.getCategory(),
                        s.getCategoryUri(),
                        s.getSubject( i ),
                        s.getSubjectUri( i ),
                        s.getPredicate( i ),
                        s.getPredicateUri( i ),
                        s.getObject( i ),
                        s.getObjectUri( i ) ) );
            }
        }
        return models;
    }

    @Value
    private static class StatementModel {
        String category;
        String categoryUri;
        String subject;
        String subjectUri;
        String predicate;
        String predicateUri;
        String object;
        String objectUri;

        @Override
        public int hashCode() {
            return Objects.hash(
                    CharacteristicUtils.hash( category, categoryUri ),
                    CharacteristicUtils.hash( subject, subjectUri ),
                    CharacteristicUtils.hash( predicate, predicateUri ),
                    CharacteristicUtils.hash( object, objectUri ) );
        }

        @Override
        public boolean equals( Object other ) {
            if ( this == other ) {
                return true;
            }
            if ( !( other instanceof StatementModel ) ) {
                return false;
            }
            StatementModel o = ( StatementModel ) other;
            return CharacteristicUtils.equals( subject, subjectUri, o.subject, o.subjectUri )
                    && CharacteristicUtils.equals( predicate, predicateUri, o.predicate, o.predicateUri )
                    && CharacteristicUtils.equals( object, objectUri, o.object, o.objectUri );
        }
    }
}
