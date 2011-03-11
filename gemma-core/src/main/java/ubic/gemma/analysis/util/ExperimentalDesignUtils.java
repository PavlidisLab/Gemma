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
package ubic.gemma.analysis.util;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author paul
 * @version $Id$
 */
public class ExperimentalDesignUtils {

    /**
     * @param experimentalFactor
     * @return true if the factor is continuous; false if it looks to be categorical.
     */
    public static boolean isContinuous( ExperimentalFactor ef ) {
        if ( ef.getType() != null ) {
            return ef.getType().equals( FactorType.CONTINUOUS );
        }
        for ( FactorValue fv : ef.getFactorValues() ) {
            if ( fv.getMeasurement() != null ) {
                try {
                    Double.parseDouble( fv.getMeasurement().getValue() );
                    return true;
                } catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }
        return false;
    }

}
