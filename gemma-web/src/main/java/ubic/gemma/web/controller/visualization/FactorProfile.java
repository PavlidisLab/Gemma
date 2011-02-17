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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ubic.basecode.dataStructure.DoublePoint;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;

/**
 * Represents data for displaying a factor (+ factor values) in a chart.
 * 
 * @author paul
 * @version $Id$
 */
public class FactorProfile {

    /**
     * In which case the Y values will not be constant and we provide but a single vector.
     */
    Boolean isContinuous = false;

    List<List<DoublePoint>> plots;

    public FactorProfile() {
    }

    /**
     * @param ef Factor to work on.
     * @param layouts The double values are either just dummy values to tell us the extent of each factor value; or for
     *        continuous measurements it is the actual measurement.
     */
    public FactorProfile( ExperimentalFactor ef, LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> layouts ) {
        super();
        checkIfFactorIsContinuous( ef );
        List<Double> values = extractFactorPlotValues( ef, layouts );
        addValues( values );
    }

    public FactorProfile( List<Double> values, boolean isContinuous ) {
        super();
        this.isContinuous = isContinuous;
        addValues( values );
    }

    /**
     * @return the isContinuous
     */
    public Boolean getIsContinuous() {
        return isContinuous;
    }

    /**
     * @return the plots
     */
    public List<List<DoublePoint>> getPlots() {
        return plots;
    }

    /**
     * @param isContinuous the isContinuous to set
     */
    public void setIsContinuous( Boolean isContinuous ) {
        this.isContinuous = isContinuous;
    }

    /**
     * @param plots the plots to set
     */
    public void setPlots( List<List<DoublePoint>> plots ) {
        this.plots = plots;
    }

    /**
     * Y values are the continuous measures; otherwise we just use zero as the y axis value. The x axis is simply an
     * index.
     * 
     * @param values A list of values which indicate how we should draw the plots. Each different value in this list
     *        indicates a new list. For example, 1 1 1 2 2 2 would lead to two sublists, 1 1 1 2 2 2 3 3 3 would lead to
     *        three, etc. 1 1 2 2 1 1 2 2 would lead to four plots.
     */
    private void addValues( List<Double> values ) {

        this.plots = new ArrayList<List<DoublePoint>>();
        List<DoublePoint> currentList = new ArrayList<DoublePoint>();
        int i = 0;
        double lastValue = 0.0;
        boolean first = true;

        for ( Double d : values ) {
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
                if ( currentList.size() > 0 ) {
                    this.plots.add( currentList );
                }

                // start a new list, don't increment the X axis.
                currentList = new ArrayList<DoublePoint>();
                currentList.add( new DoublePoint( i, 0 ) );
            } else {
                i++;
            }
            lastValue = d;
        }

        if ( currentList.size() > 0 ) {
            this.plots.add( currentList );
        }

    }

    private void checkIfFactorIsContinuous( ExperimentalFactor ef ) {
        this.isContinuous = ef.getType().equals(FactorType.CONTINUOUS);
//        for ( FactorValue fv : ef.getFactorValues() ) {
//            if ( fv.getMeasurement() != null ) {
//                try {
//                    Double.parseDouble( fv.getMeasurement().getValue() );
//                    this.isContinuous = true;
//                } catch ( NumberFormatException e ) {
//                    this.isContinuous = false;
//                    break;
//                }
//            }
//        }
    }

    private List<Double> extractFactorPlotValues( ExperimentalFactor ef,
            LinkedHashMap<BioAssay, Map<ExperimentalFactor, Double>> profiles ) {
        List<Double> values = new ArrayList<Double>();
        for ( BioAssay ba : profiles.keySet() ) {
            for ( ExperimentalFactor bef : profiles.get( ba ).keySet() ) {
                if ( bef.equals( ef ) ) {
                    values.add( profiles.get( ba ).get( ef ) );
                }
            }
        }
        return values;
    }

}
