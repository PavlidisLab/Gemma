/*
 * The baseCode project
 *
 * Copyright (c) 2010 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util.math.linearmodels;

import java.util.Collection;

public interface GenericAnovaResult extends AnovaResult {

    Collection<String> getMainEffectFactorNames();

    double getMainEffectDof( String factorName );

    double getMainEffectFStat( String factorName );

    double getMainEffectPValue( String factorName );

    boolean hasInteractions();

    /**
     * Obtain the degrees of freedom for the interaction effects, if any.
     * @throws IllegalStateException if there is more than one interaction effect
     */
    double getInteractionEffectDof();

    /**
     * Obtain the F statistic for the interaction effects, if any.
     * @throws IllegalStateException if there is more than one interaction effect
     */
    double getInteractionEffectFStat();

    /**
     * Obtain the p-value for the interaction effects, if any.
     * @throws IllegalStateException if there is more than one interaction effect
     */
    double getInteractionEffectPValue();

    /**
     * Obtain all the combinations of factor names that have interaction effects.
     */
    Collection<String[]> getInteractionEffectFactorNames();

    double getInteractionEffectDof( String... factorNames );

    double getInteractionEffectFStat( String... factorNames );

    double getInteractionEffectPValue( String... factorNames );
}
