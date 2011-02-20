/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.analysis.preprocess.svd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Store information about SVD of expression data and comparisons to factors/batch information.
 * 
 * @author paul
 * @version $Id$
 */
public class SVDValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID of the experiment this is for
     */
    private Long id;

    private Double[] variances = null;

    private DoubleMatrix<Integer, Integer> vMatrix = null;

    /*
     * Need to store the correlations of eigengenes with dates of assays, and also with factors. Statistics are
     * rank-based correlations EXCEPT for categorical factors with more than two groups in which case we are using the
     * KW test.
     */

    /**
     * In order like the rows of the v matrix.
     */
    private Long[] bioMaterialIds;

    Map<Integer, Double> dateCorrelations = new HashMap<Integer, Double>();

    private Map<Integer, Map<Long, Double>> factorCorrelations = new HashMap<Integer, Map<Long, Double>>();

    private Map<Integer, Map<Long, Double>> factorPvalues = new HashMap<Integer, Map<Long, Double>>();

    /**
     * @param id
     * @param bioMaterialIds
     * @param singularValues
     * @param vMatrix
     */
    public SVDValueObject( Long id, List<Long> bioMaterialIds, Double[] variances,
            DoubleMatrix<Integer, Integer> vMatrix ) {
        super();
        this.id = id;
        this.variances = variances;
        this.vMatrix = vMatrix;
        this.bioMaterialIds = new Long[bioMaterialIds.size()];
        bioMaterialIds.toArray( this.bioMaterialIds );
    }

    public Long[] getBioMaterialIds() {
        return bioMaterialIds;
    }

    /**
     * @return Map of component to correlation that component with "batch/scan date" (the dates associated with
     *         BioAssays)
     */
    public Map<Integer, Double> getDateCorrelations() {
        return dateCorrelations;
    }

    /**
     * @return map of component to a map of ExperimentalFactors to correlations of that factor with the component. Only
     *         used for factors which are continuous or which have only two categorical levels.
     */
    public Map<Integer, Map<Long, Double>> getFactorCorrelations() {
        return factorCorrelations;
    }

    /**
     * @return map of component to a map of ExperimentalFactors to pvalues for association of that factor with the
     *         component. This is only used if the ExperimentalFactor has more than two levels so we used the
     *         Kruskal-Wallis test. FIXME not used now.
     */
    public Map<Integer, Map<Long, Double>> getFactorPvalues() {
        return factorPvalues;
    }

    public Long getId() {
        return id;
    }

    public Double[] getVariances() {
        return variances;
    }

    public DoubleMatrix<Integer, Integer> getvMatrix() {
        return vMatrix;
    }

    public void setBioMaterialIds( Long[] bioMaterialIds ) {
        this.bioMaterialIds = bioMaterialIds;
    }

    public void setDateCorrelations( Map<Integer, Double> dateCorrelations ) {
        this.dateCorrelations = dateCorrelations;
    }

    public void setFactorCorrelations( Map<Integer, Map<Long, Double>> factorCorrelations ) {
        this.factorCorrelations = factorCorrelations;
    }

    public void setFactorPvalues( Map<Integer, Map<Long, Double>> factorPvalues ) {
        this.factorPvalues = factorPvalues;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setPCDateCorrelation( int componentNumber, double dateCorrelation ) {
        this.dateCorrelations.put( componentNumber, dateCorrelation );
    }

    public void setPCFactorCorrelation( int componentNumber, ExperimentalFactor ef, double factorCorrelation ) {
        if ( !this.factorCorrelations.containsKey( componentNumber ) ) {
            this.factorCorrelations.put( componentNumber, new HashMap<Long, Double>() );
        }
        this.factorCorrelations.get( componentNumber ).put( ef.getId(), factorCorrelation );
    }

    /*
     * 
     */
    public void setPCFactorPvalue( int componentNumber, ExperimentalFactor ef, double pval ) {
        if ( !this.factorPvalues.containsKey( componentNumber ) ) {
            this.factorPvalues.put( componentNumber, new HashMap<Long, Double>() );
        }
        this.factorPvalues.get( componentNumber ).put( ef.getId(), pval );
    }

    public void setVariances( Double[] variances ) {
        this.variances = variances;
    }

    public void setvMatrix( DoubleMatrix<Integer, Integer> vMatrix ) {
        this.vMatrix = vMatrix;
    }

}
