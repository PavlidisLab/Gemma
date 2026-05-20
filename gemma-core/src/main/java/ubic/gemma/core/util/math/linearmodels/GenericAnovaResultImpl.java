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

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A generic representation of an R-style ANOVA table, including interactions.
 *
 * @author paul
 */
class GenericAnovaResultImpl implements Serializable, GenericAnovaResult {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final Map<TreeSet<String>, AnovaEffect> interactionEffects = new LinkedHashMap<>();
    private final Map<String, AnovaEffect> mainEffects = new LinkedHashMap<>();
    @Nullable
    private final AnovaEffect residual;

    public GenericAnovaResultImpl( String key, Collection<AnovaEffect> effects ) {
        this.key = key;
        AnovaEffect residual = null;
        for ( AnovaEffect ae : effects ) {
            String termLabel = ae.getEffectName();
            if ( ae.isInteraction() ) { // kind of lame way to detect interaction rows.
                interactionEffects.put( new TreeSet<>( Arrays.asList( StringUtils.split( termLabel, ":" ) ) ), ae );
            } else if ( ae.isResiduals() ) {
                residual = ae;
            } else {
                mainEffects.put( termLabel, ae );
            }
        }
        this.residual = residual;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean hasResiduals() {
        return residual != null;
    }

    @Override
    public double getResidualsDof() {
        return residual != null ? residual.getDof() : Double.NaN;
    }

    @Override
    public double getResidualsFStat() {
        return residual != null ? residual.getFStat() : Double.NaN;
    }

    @Override
    public double getResidualsPValue() {
        return residual != null ? residual.getPValue() : Double.NaN;
    }

    @Override
    public double getInteractionEffectDof() {
        if ( interactionEffects.size() > 1 ) {
            throw new IllegalStateException( "Must specify which interaction" );
        }
        if ( interactionEffects.isEmpty() ) {
            return Double.NaN;
        }
        return interactionEffects.values().iterator().next().getDof();
    }

    @Override
    public double getInteractionEffectFStat() {
        if ( interactionEffects.size() > 1 ) {
            throw new IllegalStateException( "Must specify which interaction" );
        }
        if ( interactionEffects.isEmpty() ) {
            return Double.NaN;
        }
        return interactionEffects.values().iterator().next().getFStat();
    }

    @Override
    public double getInteractionEffectPValue() {
        if ( interactionEffects.size() > 1 ) {
            throw new IllegalStateException( "Must specify which interaction" );
        }
        if ( interactionEffects.isEmpty() ) {
            return Double.NaN;
        }
        return interactionEffects.values().iterator().next().getPValue();
    }

    @Override
    public Collection<String[]> getInteractionEffectFactorNames() {
        return interactionEffects.keySet().stream()
            .map( fn -> fn.toArray( new String[0] ) )
            .collect( Collectors.toList() );
    }

    @Override
    public double getInteractionEffectDof( String... factorNames ) {
        if ( interactionEffects.isEmpty() ) {
            return Double.NaN;
        }
        Set<String> interactionFactor = new TreeSet<>( Arrays.asList( factorNames ) );
        if ( !interactionEffects.containsKey( interactionFactor ) ) {
            return Double.NaN;
        }
        return interactionEffects.get( interactionFactor ).getDof();
    }

    @Override
    public double getInteractionEffectFStat( String... factorNames ) {
        if ( interactionEffects.isEmpty() ) {
            return Double.NaN;
        }
        TreeSet<String> interactionFactor = new TreeSet<>( Arrays.asList( factorNames ) );
        if ( !interactionEffects.containsKey( interactionFactor ) ) {
            return Double.NaN;
        }
        return interactionEffects.get( interactionFactor ).getFStat();
    }

    @Override
    public double getInteractionEffectPValue( String... factorNames ) {
        if ( interactionEffects.isEmpty() ) {
            return Double.NaN;
        }
        TreeSet<String> interactionFactor = new TreeSet<>( Arrays.asList( factorNames ) );
        if ( !interactionEffects.containsKey( interactionFactor ) ) {
            return Double.NaN;
        }
        return interactionEffects.get( interactionFactor ).getPValue();
    }

    @Override
    public double getMainEffectDof( String factorName ) {
        if ( mainEffects.get( factorName ) == null ) {
            return Double.NaN;
        }
        return mainEffects.get( factorName ).getDof();
    }

    @Override
    public double getMainEffectFStat( String factorName ) {
        if ( mainEffects.get( factorName ) == null ) {
            return Double.NaN;
        }
        return mainEffects.get( factorName ).getFStat();
    }

    /**
     * @return names of main effects factors like 'f', 'g'.
     */
    @Override
    public Collection<String> getMainEffectFactorNames() {
        return mainEffects.keySet();
    }

    @Override
    public double getMainEffectPValue( String factorName ) {
        if ( mainEffects.get( factorName ) == null ) {
            return Double.NaN;
        }
        return mainEffects.get( factorName ).getPValue();
    }

    @Override
    public boolean hasInteractions() {
        return !interactionEffects.isEmpty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        GenericAnovaResultImpl other = ( GenericAnovaResultImpl ) obj;
        if ( key == null ) {
            if ( other.key != null ) return false;
        } else if ( !key.equals( other.key ) ) return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( "ANOVA table " ).append( this.getKey() ).append( " \n" );

        buf.append( StringUtils.leftPad( "\t", 10 ) ).append( "Df\tSSq\tMSq\tF\tP\n" );

        for ( String me : this.getMainEffectFactorNames() ) {
            if ( me.equals( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME ) ) {
                continue;
            }
            AnovaEffect a = mainEffects.get( me );
            buf.append( a ).append( "\n" );
        }

        if ( hasInteractions() ) {
            for ( TreeSet<String> ifa : interactionEffects.keySet() ) {
                AnovaEffect a = this.interactionEffects.get( ifa );
                buf.append( a ).append( "\n" );
            }
        }

        buf.append( residual ).append( "\n" );

        return buf.toString();
    }
}