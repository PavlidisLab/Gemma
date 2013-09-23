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
package ubic.gemma.model.analysis.expression.pca;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;

/**
 * 
 */
public abstract class PrincipalComponentAnalysis extends SingleExperimentAnalysis {

    /**
     * Constructs new instances of {@link PrincipalComponentAnalysis}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link PrincipalComponentAnalysis}.
         */
        public static PrincipalComponentAnalysis newInstance() {
            return new PrincipalComponentAnalysisImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7305397084531510807L;
    private Integer numComponentsStored;

    private Integer maxNumProbesPerComponent;

    private ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension;

    private Collection<ProbeLoading> probeLoadings = new HashSet<ProbeLoading>();

    private Collection<Eigenvalue> eigenValues = new HashSet<Eigenvalue>();

    private Collection<Eigenvector> eigenVectors = new HashSet<Eigenvector>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public PrincipalComponentAnalysis() {
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.bioAssayData.BioAssayDimension getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    /**
     * 
     */
    public Collection<Eigenvalue> getEigenValues() {
        return this.eigenValues;
    }

    /**
     * Convenience method to access the eigenvectors, as a List of Double[].
     */
    public abstract List<Double[]> getEigenvectorArrays();

    /**
     * 
     */
    public Collection<Eigenvector> getEigenVectors() {
        return this.eigenVectors;
    }

    /**
     * <p>
     * How many probe loadings were stored per component (max).
     * </p>
     */
    public Integer getMaxNumProbesPerComponent() {
        return this.maxNumProbesPerComponent;
    }

    /**
     * <p>
     * How many components results are stored for (e.g. 3)
     * </p>
     */
    public Integer getNumComponentsStored() {
        return this.numComponentsStored;
    }

    /**
     * 
     */
    public Collection<ProbeLoading> getProbeLoadings() {
        return this.probeLoadings;
    }

    /**
     * <p>
     * An array of values representing the fraction of the variance each component accounts for. Convenience method to
     * access the Eigenvalue data.
     * </p>
     */
    public abstract Double[] getVarianceFractions();

    public void setBioAssayDimension( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public void setEigenValues( Collection<Eigenvalue> eigenValues ) {
        this.eigenValues = eigenValues;
    }

    public void setEigenVectors( Collection<Eigenvector> eigenVectors ) {
        this.eigenVectors = eigenVectors;
    }

    public void setMaxNumProbesPerComponent( Integer maxNumProbesPerComponent ) {
        this.maxNumProbesPerComponent = maxNumProbesPerComponent;
    }

    public void setNumComponentsStored( Integer numComponentsStored ) {
        this.numComponentsStored = numComponentsStored;
    }

    public void setProbeLoadings( Collection<ProbeLoading> probeLoadings ) {
        this.probeLoadings = probeLoadings;
    }

}