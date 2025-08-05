package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;

import java.util.Collections;
import java.util.List;

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
}
