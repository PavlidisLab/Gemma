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
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author paul
 */
@CommonsLog
@ParametersAreNonnullByDefault
public class ExperimentalDesignUtils {

    /**
     * Create a mapping of samples to factor values for all factors in the experimental design.
     * <p>
     * The resulting map will be populated for every factor in the design and every provided sample. If a sample is
     * lacking a particular value is missing, a {@code null} will be used. If a sample has more than one value, a
     * {@link IllegalStateException} will be raised.
     * @throws IllegalStateException if a sample has more than one value any factor in the design
     */
    public static Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> getFactorValueMap( ExperimentalDesign experimentalDesign, Collection<BioMaterial> samples ) {
        return getFactorValueMap( experimentalDesign.getExperimentalFactors(), samples );
    }

    public static Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> getFactorValueMap( Collection<ExperimentalFactor> factors, Collection<BioMaterial> samples ) {
        int numSamples = samples.size();
        int numFactors = factors.size();
        Map<BioMaterial, Map<ExperimentalFactor, Set<FactorValue>>> factorValueIndex = new HashMap<>( numSamples );
        for ( BioMaterial sample : samples ) {
            for ( FactorValue fv : sample.getAllFactorValues() ) {
                factorValueIndex
                        .computeIfAbsent( sample, k -> new HashMap<>( numFactors ) )
                        .computeIfAbsent( fv.getExperimentalFactor(), k -> new HashSet<>( 1 ) )
                        .add( fv );
            }
        }
        Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> result = new HashMap<>( factors.size() );
        for ( ExperimentalFactor factor : factors ) {
            for ( BioMaterial sample : samples ) {
                Map<ExperimentalFactor, Set<FactorValue>> fvm = factorValueIndex.get( sample );
                FactorValue value;
                if ( fvm != null ) {
                    Set<FactorValue> values = fvm.get( factor );
                    if ( values == null ) {
                        log.warn( sample + " has no value for " + factor + ", using null for " + factor + "." );
                        value = null;
                    } else if ( values.size() == 1 ) {
                        value = values.iterator().next();
                    } else {
                        throw new IllegalStateException( String.format( "%s has more than one value for %s:\n\t%s",
                                sample, factor,
                                values.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
                    }
                } else {
                    log.warn( sample + " has no factor values, using null for " + factor + "." );
                    value = null;
                }
                result.computeIfAbsent( factor, k -> new HashMap<>() )
                        .put( sample, value );
            }
        }
        return result;
    }
}
