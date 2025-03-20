/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;

import javax.annotation.Nullable;
import javax.persistence.Transient;

/**
 * The 'analysis' in the name is a bit of a stretch here, as this object servers purely as an aggregator
 * of all the sample coexpression matrices.
 */
public class SampleCoexpressionAnalysis extends SingleExperimentAnalysis {

    private SampleCoexpressionMatrix fullCoexpressionMatrix;
    @Nullable
    private SampleCoexpressionMatrix regressedCoexpressionMatrix;

    /**
     * Note that since you get a full square matrix, all correlations are represented twice, and values on the main
     * diagonal will always be 1.
     *
     * @return a coexpression matrix with all factors (none regressed out), and including outliers.
     */
    public SampleCoexpressionMatrix getFullCoexpressionMatrix() {
        return fullCoexpressionMatrix;
    }

    public void setFullCoexpressionMatrix( SampleCoexpressionMatrix rawFullCoexpressionMatrix ) {
        this.fullCoexpressionMatrix = rawFullCoexpressionMatrix;
    }

    /**
     * Note that since you get a full square matrix, all correlations are represented twice, and values on the main
     * diagonal will always be 1.
     *
     * @return a coexpression matrix with regressed out major factors.
     */
    @Nullable
    public SampleCoexpressionMatrix getRegressedCoexpressionMatrix() {
        return regressedCoexpressionMatrix;
    }

    public void setRegressedCoexpressionMatrix( @Nullable SampleCoexpressionMatrix regressedCoexpressionMatrix ) {
        this.regressedCoexpressionMatrix = regressedCoexpressionMatrix;
    }

    @Transient
    public SampleCoexpressionMatrix getBestCoexpressionMatrix() {
        return regressedCoexpressionMatrix != null ? regressedCoexpressionMatrix : fullCoexpressionMatrix;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof SampleCoexpressionAnalysis ) )
            return false;
        SampleCoexpressionAnalysis that = ( SampleCoexpressionAnalysis ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static class Factory {

        public static SampleCoexpressionAnalysis newInstance( BioAssaySet experimentAnalyzed, SampleCoexpressionMatrix fullCoexpressionMatrix,
                @Nullable SampleCoexpressionMatrix regressedCoexpressionMatrix ) {
            SampleCoexpressionAnalysis analysis = new SampleCoexpressionAnalysis();
            analysis.setExperimentAnalyzed( experimentAnalyzed );
            analysis.setFullCoexpressionMatrix( fullCoexpressionMatrix );
            analysis.setRegressedCoexpressionMatrix( regressedCoexpressionMatrix );
            return analysis;
        }
    }
}