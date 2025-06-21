/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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

package ubic.gemma.web.controller.visualization;

import lombok.extern.apachecommons.CommonsLog;
import ubic.basecode.dataStructure.DoublePoint;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents data for displaying a factor (+ factor values) in a chart.
 *
 * @author paul
 */
@CommonsLog
public class FactorProfile {

    /**
     * In which case the Y values will not be constant and we provide but a single vector.
     */
    private Boolean isContinuous = false;

    private List<List<DoublePoint>> plots;

    public FactorProfile() {
    }

    /**
     * @param ef      Factor to work on.
     * @param layouts The double values are either just dummy values to tell us the extent of each factor value; or for
     *                continuous measurements it is the actual measurement.
     */
    public FactorProfile( ExperimentalFactor ef,
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layouts ) {
        super();
        this.checkIfFactorIsContinuous( ef );
        List<Double> values = this.extractFactorPlotValues( ef, layouts );
        this.addValues( values );
    }

    /**
     */
    public FactorProfile( List<Double> values, boolean isContinuous ) {
        super();
        this.isContinuous = isContinuous;
        this.addValues( values );
    }

    /**
     * @return the isContinuous
     */
    public Boolean getIsContinuous() {
        return isContinuous;
    }

    /**
     * @param isContinuous the isContinuous to set
     */
    public void setIsContinuous( Boolean isContinuous ) {
        this.isContinuous = isContinuous;
    }

    /**
     * @return the plots
     */
    public List<List<DoublePoint>> getPlots() {
        return plots;
    }

    /**
     * @param plots the plots to set
     */
    public void setPlots( List<List<DoublePoint>> plots ) {
        this.plots = plots;
    }

    @Override
    public String toString() {
        final int maxLen = 5;
        return "FactorProfile [" + ( plots != null ?
                "plots=" + plots.subList( 0, Math.min( plots.size(), maxLen ) ) :
                "" ) + "]";
    }

    /**
     * Y values are the continuous measures; otherwise we just use zero as the y axis value. The x axis is simply an
     * index.
     *
     * @param values A list of values which indicate how we should draw the plots. Each different value in this list
     *               indicates a new list. For example, 1 1 1 2 2 2 would lead to two sublists, 1 1 1 2 2 2 3 3 3 would lead to
     *               three, etc. 1 1 2 2 1 1 2 2 would lead to four plots.
     */
    private void addValues( List<Double> values ) {

        this.plots = new ArrayList<>();
        List<DoublePoint> currentList = new ArrayList<>();
        int i = 0;
        double lastValue = 0.0;
        boolean first = true;
        int nullCount = 0;

        for ( Double d : values ) {
            if ( d == null ) {
                nullCount++;
            } else {
                if ( this.isContinuous ) {
                    currentList.add( new DoublePoint( i, d ) );
                    i++;
                    continue;
                }

                if ( first ) {
                    currentList.add( new DoublePoint( i, 0 ) );
                    first = false;
                } else if ( d != lastValue ) {
                    // save the list we just finished.
                    currentList.add( new DoublePoint( i, 0 ) );
                    this.plots.add( currentList );

                    // start a new list, don't increment the X axis.
                    currentList = new ArrayList<>();
                    currentList.add( new DoublePoint( i, 0 ) );
                } else {
                    i++;
                }
                lastValue = d;
            }
        }
        if ( nullCount > 0 ) {
            FactorProfile.log.warn( nullCount + " null value(s) not added to plot list of DoublePoints." );
        }

        if ( !currentList.isEmpty() ) {
            this.plots.add( currentList );
        }

    }

    /**
     * @param ef experimental factor
     */
    private void checkIfFactorIsContinuous( ExperimentalFactor ef ) {
        if ( ef.getType() != null ) {
            this.isContinuous = ef.getType().equals( FactorType.CONTINUOUS );
        } else {
            for ( FactorValue fv : ef.getFactorValues() ) {
                if ( fv.getMeasurement() != null && fv.getMeasurement().getValue() != null ) {
                    try {
                        Double.parseDouble( fv.getMeasurement().getValue() );
                        this.isContinuous = true;
                    } catch ( NumberFormatException e ) {
                        this.isContinuous = false;
                        break;
                    }
                }
            }
        }
    }

    private List<Double> extractFactorPlotValues( ExperimentalFactor ef,
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> profiles ) {
        List<Double> values = new ArrayList<>();
        for ( BioAssayValueObject ba : profiles.keySet() ) {
            for ( ExperimentalFactor bef : profiles.get( ba ).keySet() ) {
                if ( bef.equals( ef ) ) {
                    values.add( profiles.get( ba ).get( ef ) );
                }
            }
        }
        return values;
    }

}
