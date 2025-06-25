/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author paul
 */
@CommonsLog
@ParametersAreNonnullByDefault
public class ExperimentalDesignUtils {

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

    public static final String BIO_MATERIAL_RNAME_PREFIX = "biomat_";
    public static final String FACTOR_RNAME_PREFIX = "fact.";
    public static final String FACTOR_VALUE_RNAME_PREFIX = "fv_";
    public static final String FACTOR_VALUE_BASELINE_SUFFIX = "_base";

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
        return ExperimentalDesignUtils.BATCH_FACTOR_NAME.equalsIgnoreCase( ef.getName() );
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
        return ExperimentalDesignUtils.BATCH_FACTOR_NAME.equalsIgnoreCase( ef.getName() );
    }

    /**
     * Sort factors in a consistent way.
     * <p>
     * For this to work, the factors must be persistent as the order will be based on the numerical ID.
     */
    public static List<ExperimentalFactor> getOrderedFactors( Collection<ExperimentalFactor> factors ) {
        return factors.stream()
                .sorted( Comparator.comparing( ExperimentalFactor::getId ) )
                .collect( Collectors.toList() );
    }
}
