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
import java.util.List;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;

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
     * rank-based correlations EXCEPT for categorical factors with more than two groups in which case we are using the KW test.
     */

    /**
     * In order like the rows of the v matrix.
     */
    private Long[] bioMaterialIds;

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

    public Long getId() {
        return id;
    }

    public DoubleMatrix<Integer, Integer> getvMatrix() {
        return vMatrix;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setvMatrix( DoubleMatrix<Integer, Integer> vMatrix ) {
        this.vMatrix = vMatrix;
    }

    protected Long[] getBioMaterialIds() {
        return bioMaterialIds;
    }

    protected Double[] getVariances() {
        return variances;
    }

    protected void setBioMaterialIds( Long[] bioMaterialIds ) {
        this.bioMaterialIds = bioMaterialIds;
    }

    protected void setVariances( Double[] variances ) {
        this.variances = variances;
    }

}
