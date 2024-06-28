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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

import javax.persistence.Transient;
import java.util.*;

public class PrincipalComponentAnalysis extends SingleExperimentAnalysis {

    private static final long serialVersionUID = 7046708934564931841L;
    private Integer numComponentsStored;
    private Integer maxNumProbesPerComponent;
    private BioAssayDimension bioAssayDimension;
    private Set<ProbeLoading> probeLoadings = new HashSet<>();
    private Set<Eigenvalue> eigenValues = new HashSet<>();
    private Set<Eigenvector> eigenVectors = new HashSet<>();

    public ubic.gemma.model.expression.bioAssayData.BioAssayDimension getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    public void setBioAssayDimension( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public Set<Eigenvalue> getEigenValues() {
        return this.eigenValues;
    }

    public void setEigenValues( Set<Eigenvalue> eigenValues ) {
        this.eigenValues = eigenValues;
    }

    public Set<Eigenvector> getEigenVectors() {
        return this.eigenVectors;
    }

    public void setEigenVectors( Set<Eigenvector> eigenVectors ) {
        this.eigenVectors = eigenVectors;
    }

    /**
     * @return How many probe loadings were stored per component (max).
     */
    public Integer getMaxNumProbesPerComponent() {
        return this.maxNumProbesPerComponent;
    }

    public void setMaxNumProbesPerComponent( Integer maxNumProbesPerComponent ) {
        this.maxNumProbesPerComponent = maxNumProbesPerComponent;
    }

    /**
     * @return How many components results are stored for (e.g. 3)
     */
    public Integer getNumComponentsStored() {
        return this.numComponentsStored;
    }

    public void setNumComponentsStored( Integer numComponentsStored ) {
        this.numComponentsStored = numComponentsStored;
    }

    public Set<ProbeLoading> getProbeLoadings() {
        return this.probeLoadings;
    }

    public void setProbeLoadings( Set<ProbeLoading> probeLoadings ) {
        this.probeLoadings = probeLoadings;
    }

    /**
     * @return Convenience method to access the eigenvectors, as a List of Double[].
     */
    @Transient
    public List<Double[]> getEigenvectorArrays() throws IllegalArgumentException {
        ByteArrayConverter bac = new ByteArrayConverter();

        List<Double[]> result = new ArrayList<>( this.getNumComponentsStored() );

        Collection<BioAssay> bioAssays = this.getBioAssayDimension().getBioAssays();

        if ( bioAssays.size() < this.getNumComponentsStored() ) {
            /*
             * This is a sanity check. The number of components stored is fixed at some lower value
             */
            throw new IllegalArgumentException(
                    "EE id = " + this.getExperimentAnalyzed().getId() + ", PCA: Number of components stored (" + this
                            .getNumComponentsStored() + ") is less than the number of bioAssays (" + bioAssays.size()
                            + ")" );
        }

        for ( int i = 0; i < bioAssays.size(); i++ ) {
            result.add( null );
        }

        for ( Eigenvector ev : this.getEigenVectors() ) {
            int index = ev.getComponentNumber() - 1;
            if ( index >= this.getNumComponentsStored() )
                continue;
            double[] doubleArr = bac.byteArrayToDoubles( ev.getVector() );
            Double[] dA = ArrayUtils.toObject( doubleArr );
            result.set( index, dA );
        }

        CollectionUtils.filter( result, Objects::nonNull );
        return result;
    }

    /**
     * @return An array of values representing the fraction of the variance each component accounts for. Convenience method to
     * access the Eigenvalue data.
     */
    @Transient
    public double[] getVarianceFractions() {
        double[] result = new double[this.getEigenValues().size()];
        for ( Eigenvalue v : this.getEigenValues() ) {
            result[v.getComponentNumber() - 1] = v.getVarianceFraction();
        }
        return result;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof PrincipalComponentAnalysis ) )
            return false;
        PrincipalComponentAnalysis that = ( PrincipalComponentAnalysis ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {
        public static PrincipalComponentAnalysis newInstance() {
            return new PrincipalComponentAnalysis();
        }
    }

}