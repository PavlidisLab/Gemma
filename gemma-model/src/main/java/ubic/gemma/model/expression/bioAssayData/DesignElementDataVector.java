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
package ubic.gemma.model.expression.bioAssayData;

/**
 * 
 */
public abstract class DesignElementDataVector extends ubic.gemma.model.expression.bioAssayData.DataVectorImpl {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5614004098731894433L;

    private ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension;
    private ubic.gemma.model.expression.designElement.CompositeSequence designElement;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public DesignElementDataVector() {
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
    public ubic.gemma.model.expression.designElement.CompositeSequence getDesignElement() {
        return this.designElement;
    }

    /**
     * 
     */
    public abstract ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment();

    public void setBioAssayDimension( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public void setDesignElement( ubic.gemma.model.expression.designElement.CompositeSequence designElement ) {
        this.designElement = designElement;
    }

    /**
     * 
     */
    public abstract void setExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

}