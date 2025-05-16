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

import lombok.Value;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Store information about SVD of expression data and comparisons to factors/batch information.
 *
 * @author paul
 */
@Value
public class SVDResult implements Serializable {

    /**
     * Experiment or subset this is for.
     */
    BioAssaySet experimentAnalyzed;

    /**
     * Assays used in the SVD analysis.
     * <p>
     * In order like the rows of the V matrix.
     */
    List<BioAssay> bioAssays;

    /**
     * Biomaterials used in the SVD analysis.
     * <p>
     * In order like the rows of the V matrix.
     */
    List<BioMaterial> bioMaterials;

    /**
     * An array of values representing the fraction of the variance each component accounts for
     */
    double[] variances;

    /**
     * Row names: biomaterials; column names: eigengene number (from 0)
     */
    DoubleMatrix<BioMaterial, Integer> vMatrix;

    /**
     * Date associated to the {@link #bioMaterials}.
     * <p>
     * Missing values are encoded as {@code null}.
     */
    List<Date> dates = new ArrayList<>();

    /**
     * Map of component to correlation that component with "batch/scan date"
     * @see #dates
     */
    Map<Integer, Double> dateCorrelations = new HashMap<>();

    /**
     * P-values associated to the "batch/scan date" component.
     * @see #dates
     */
    Map<Integer, Double> datePVals = new HashMap<>();

    /**
     * Map of factors to the double-ized representations of them.
     */
    Map<ExperimentalFactor, List<Number>> factors = new HashMap<>();

    /**
     * Map of component to a map of ExperimentalFactor IDs to correlations of that factor with the component.
     */
    Map<Integer, Map<ExperimentalFactor, Double>> factorCorrelations = new HashMap<>();

    /**
     * Map of component to map of ExperimentalFactor IDs to P-values for the association of that factor with the
     * component.
     * <p>
     * Need to store the correlations of eigengenes with dates of assays, and also with factors. Statistics are
     * rank-based correlations
     */
    Map<Integer, Map<ExperimentalFactor, Double>> factorPVals = new HashMap<>();

    public SVDResult( PrincipalComponentAnalysis pca ) {
        this.experimentAnalyzed = pca.getExperimentAnalyzed();
        this.bioAssays = assaysFromPca( pca );
        this.bioMaterials = samplesFromPca( pca );
        this.variances = pca.getVarianceFractions();
        this.vMatrix = matrixFromPca( pca, this.bioMaterials );
    }

    private List<BioAssay> assaysFromPca( PrincipalComponentAnalysis pca ) {
        return new ArrayList<>( pca.getBioAssayDimension().getBioAssays() );
    }

    private List<BioMaterial> samplesFromPca( PrincipalComponentAnalysis pca ) {
        return pca.getBioAssayDimension()
                .getBioAssays()
                .stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
    }

    private DoubleMatrix<BioMaterial, Integer> matrixFromPca( PrincipalComponentAnalysis pca, List<BioMaterial> bioMaterials ) {
        List<Double[]> eigenvectorArrays = pca.getEigenvectorArrays();
        int numCols = eigenvectorArrays.size();

        if ( numCols == 0 ) {
            // empty matrix, we cannot check if the rows match, so we must assume it's correct
            DenseDoubleMatrix<BioMaterial, Integer> result = new DenseDoubleMatrix<>( bioMaterials.size(), 0 );
            result.setRowNames( bioMaterials );
            result.setColumnNames( new ArrayList<>() );
            return result;
        }

        int numRows = eigenvectorArrays.get( 0 ).length;

        if ( bioMaterials.size() != numRows ) {
            throw new IllegalArgumentException( "The number of rows is not compatible with the number of biomaterials in " + pca + "." );
        }

        DenseDoubleMatrix<BioMaterial, Integer> result = new DenseDoubleMatrix<>( numRows, eigenvectorArrays.size() );

        result.setRowNames( bioMaterials );

        List<Integer> columNames = new ArrayList<>( numCols );
        for ( int k = 0; k < eigenvectorArrays.size(); k++ ) {
            Double[] vec = eigenvectorArrays.get( k );
            if ( vec.length != numRows ) {
                throw new IllegalStateException( "PCA vector at column " + k + " does not have the expected size of " + numRows + "." );
            }
            for ( int i = 0; i < vec.length; i++ ) {
                result.set( i, k, vec[i] ); // fill columns
            }
            columNames.add( k );
        }
        result.setColumnNames( columNames );
        return result;
    }

    void setPCDateCorrelation( int componentNumber, double dateCorrelation ) {
        this.dateCorrelations.put( componentNumber, dateCorrelation );
    }

    void setPCDateCorrelationPval( int componentNumber, double spearmanPvalue ) {
        this.datePVals.put( componentNumber, spearmanPvalue );
    }

    void setPCFactorCorrelation( int componentNumber, ExperimentalFactor ef, double factorCorrelation ) {
        if ( !this.factorCorrelations.containsKey( componentNumber ) ) {
            this.factorCorrelations.put( componentNumber, new HashMap<>() );
        }
        this.factorCorrelations.get( componentNumber ).put( ef, factorCorrelation );
    }

    void setPCFactorCorrelationPval( int componentNumber, ExperimentalFactor ef, double pvalue ) {
        if ( !this.factorPVals.containsKey( componentNumber ) ) {
            this.factorPVals.put( componentNumber, new HashMap<>() );
        }
        this.factorPVals.get( componentNumber ).put( ef, pvalue );
    }
}
