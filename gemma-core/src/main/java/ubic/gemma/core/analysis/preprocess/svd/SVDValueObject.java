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

package ubic.gemma.core.analysis.preprocess.svd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.io.Serializable;
import java.util.*;

/**
 * Store information about SVD of expression data and comparisons to factors/batch information.
 *
 * @author paul
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class SVDValueObject implements Serializable {

    private static final Log log = LogFactory.getLog( SVDValueObject.class.getName() );
    private static final long serialVersionUID = 1L;

    /**
     * In order like the rows of the v matrix.
     */
    private Long[] bioMaterialIds;
    private Map<Integer, Double> dateCorrelations = new HashMap<>();
    private Map<Integer, Double> datePVals = new HashMap<>();
    private List<Date> dates = new ArrayList<>();
    private Map<Integer, Map<Long, Double>> factorCorrelations = new HashMap<>();

    /**
     * Need to store the correlations of eigengenes with dates of assays, and also with factors. Statistics are
     * rank-based correlations
     */
    private Map<Integer, Map<Long, Double>> factorPvals = new HashMap<>();
    /**
     * Map of factors to the double-ized representations of them.
     */
    private Map<Long, List<Double>> factors = new HashMap<>();
    /**
     * ID of the experiment this is for
     */
    private Long id;
    /**
     * An array of values representing the fraction of the variance each component accounts for
     */
    private double[] variances;
    private DoubleMatrix<Long, Integer> vMatrix;

    public SVDValueObject() {

    }

    public SVDValueObject( Long id, List<Long> bioMaterialIds, double[] variances,
            DoubleMatrix<Long, Integer> vMatrix ) {
        super();
        this.id = id;
        this.variances = variances;
        this.vMatrix = vMatrix;
        this.bioMaterialIds = new Long[bioMaterialIds.size()];
        bioMaterialIds.toArray( this.bioMaterialIds );
    }

    public SVDValueObject( PrincipalComponentAnalysis pca ) {
        this.id = pca.getExperimentAnalyzed().getId();

        this.variances = pca.getVarianceFractions();
        List<Double[]> eigenvectorArrays;

        eigenvectorArrays = pca.getEigenvectorArrays();

        List<Long> bmids = new ArrayList<>();
        for ( BioAssay ba : pca.getBioAssayDimension().getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            bmids.add( bm.getId() );
        }

        this.bioMaterialIds = bmids.toArray( new Long[] {} );

        this.vMatrix = new DenseDoubleMatrix<>( eigenvectorArrays.get( 0 ).length, eigenvectorArrays.size() );

        if ( this.bioMaterialIds.length != eigenvectorArrays.get( 0 ).length ) {
            SVDValueObject.log.warn( "EE id = " + pca.getExperimentAnalyzed().getId()
                    + ": Biomaterials and eigenvectors are of different length: " + this.bioMaterialIds.length
                    + " != eigenvector len = " + eigenvectorArrays.get( 0 ).length );
        } else {
            this.vMatrix.setRowNames( Arrays.asList( this.getBioMaterialIds() ) );
        }

        int j = 0;
        List<Integer> columNames = new ArrayList<>();
        for ( Double[] vec : eigenvectorArrays ) {
            for ( int i = 0; i < vec.length; i++ ) {
                vMatrix.set( i, j, vec[i] ); // fill columns
            }
            columNames.add( j );
            j++;
        }
        vMatrix.setColumnNames( columNames );

    }

    public Long[] getBioMaterialIds() {
        return bioMaterialIds;
    }

    public void setBioMaterialIds( Long[] bioMaterialIds ) {
        this.bioMaterialIds = bioMaterialIds;
    }

    /**
     * @return Map of component to correlation that component with "batch/scan date" (the dates associated with
     * BioAssays)
     */
    public Map<Integer, Double> getDateCorrelations() {
        return dateCorrelations;
    }

    public void setDateCorrelations( Map<Integer, Double> dateCorrelations ) {
        this.dateCorrelations = dateCorrelations;
    }

    public Map<Integer, Double> getDatePvals() {
        return datePVals;
    }

    public List<Date> getDates() {
        if ( this.dates == null )
            this.dates = new ArrayList<>();
        return dates;
    }

    /**
     * @return map of component to a map of ExperimentalFactor IDs to correlations of that factor with the component.
     */
    public Map<Integer, Map<Long, Double>> getFactorCorrelations() {
        return factorCorrelations;
    }

    public void setFactorCorrelations( Map<Integer, Map<Long, Double>> factorCorrelations ) {
        this.factorCorrelations = factorCorrelations;
    }

    /**
     * @return map of component to map of ExperimentalFactor IDs to pvalues for the association of that factor with the
     * component.
     */
    public Map<Integer, Map<Long, Double>> getFactorPvals() {
        return factorPvals;
    }

    public Map<Long, List<Double>> getFactors() {
        if ( this.factors == null )
            this.factors = new HashMap<>();
        return factors;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return An array of values representing the fraction of the variance each component accounts for
     */
    public double[] getVariances() {
        return variances;
    }

    public void setVariances( double[] variances ) {
        this.variances = variances;
    }

    /**
     * @return Row names: biomaterial ids; column names: eigengene number (from 0)
     */
    public DoubleMatrix<Long, Integer> getvMatrix() {
        return vMatrix;
    }

    public void setvMatrix( DoubleMatrix<Long, Integer> vMatrix ) {
        this.vMatrix = vMatrix;
    }

    public void setPCDateCorrelation( int componentNumber, double dateCorrelation ) {
        this.dateCorrelations.put( componentNumber, dateCorrelation );
    }

    public void setPCDateCorrelationPval( int componentNumber, double spearmanPvalue ) {
        this.datePVals.put( componentNumber, spearmanPvalue );

    }

    public void setPCFactorCorrelation( int componentNumber, ExperimentalFactor ef, double factorCorrelation ) {
        if ( !this.factorCorrelations.containsKey( componentNumber ) ) {
            this.factorCorrelations.put( componentNumber, new HashMap<Long, Double>() );
        }
        this.factorCorrelations.get( componentNumber ).put( ef.getId(), factorCorrelation );
    }

    public void setPCFactorCorrelationPval( int componentNumber, ExperimentalFactor ef, double pvalue ) {
        if ( !this.factorPvals.containsKey( componentNumber ) ) {
            this.factorPvals.put( componentNumber, new HashMap<Long, Double>() );
        }
        this.factorPvals.get( componentNumber ).put( ef.getId(), pvalue );
    }

}
