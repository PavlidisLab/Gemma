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

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Represents one row of an ANOVA table
 *
 * @author paul
 *
 */
public class AnovaEffect implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double dof;
    private final String effectName;
    private final double fStat;
    private final boolean isInteraction;
    private final boolean isResiduals;
    private final double meanSq;
    private final double pValue;
    private final double ssQ;

    public AnovaEffect( String effectName, double pValue, double fStat, double dof, double ssQ, boolean isInteraction, boolean isResiduals ) {
        if ( isResiduals && isInteraction ) {
            throw new IllegalArgumentException( "An ANOVA effect cannot be both a residual and an interaction." );
        }
        this.effectName = effectName;
        this.pValue = pValue;
        this.fStat = fStat;
        this.dof = dof;
        this.ssQ = ssQ;
        this.meanSq = ssQ / dof;
        this.isInteraction = isInteraction;
        this.isResiduals = isResiduals;
    }

    /**
     * @return the degreesOfFreedom
     */
    public double getDof() {
        return dof;
    }

    /**
     * @return the effectName
     */
    public String getEffectName() {
        return effectName;
    }

    /**
     * @return the fStatistic
     */
    public double getFStat() {
        return fStat;
    }

    /**
     * @return the meanSq
     */
    public double getMeanSq() {
        return meanSq;
    }

    /**
     * @return the pValue
     */
    public double getPValue() {
        return pValue;
    }

    /**
     * @return the ssQ
     */
    public double getSsQ() {
        return ssQ;
    }

    /**
     * @return the isInteraction
     */
    public boolean isInteraction() {
        return isInteraction;
    }

    public boolean isResiduals() {
        return isResiduals;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( StringUtils.rightPad( StringUtils.abbreviate( getEffectName(), 10 ), 10 ) ).append( "\t" );
        buf.append( String.format( "%.2f", getDof() ) ).append( "\t" );
        buf.append( String.format( "%.4f", getSsQ() ) ).append( "\t" );
        buf.append( String.format( "%.4f", getMeanSq() ) ).append( "\t" );
        if ( !Double.isNaN( fStat ) ) {
            buf.append( StringUtils.rightPad( String.format( "%.3f", getFStat() ), 6 ) ).append( "\t" );
            buf.append( String.format( "%.3g", getPValue() ) );
        }
        return buf.toString();
    }
}
