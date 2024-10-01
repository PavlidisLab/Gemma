/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public abstract class DataVectorValueObject extends IdentifiableValueObject<DataVector> {

    private static final long serialVersionUID = 4291090102921066018L;

    private ExpressionExperimentValueObject expressionExperiment;
    private CompositeSequenceValueObject designElement;
    private QuantitationTypeValueObject quantitationType;
    private Collection<Long> genes;
    private BioAssayDimensionValueObject bioAssayDimension;

    protected DataVectorValueObject( Long id ) {
        super( id );
    }

    protected DataVectorValueObject( BulkExpressionDataVector dedv, BioAssayDimensionValueObject badvo ) {
        super( dedv );
        if ( badvo == null ) {
            BioAssayDimension badim = dedv.getBioAssayDimension();
            this.bioAssayDimension = new BioAssayDimensionValueObject( badim );
        } else {
            this.bioAssayDimension = badvo;
        }
        // assert !this.bioAssayDimension.getBioAssays().isEmpty();
        this.quantitationType = new QuantitationTypeValueObject( dedv.getQuantitationType(), dedv.getExpressionExperiment(), dedv.getClass() );
        this.designElement = new CompositeSequenceValueObject( dedv.getDesignElement() );
        this.expressionExperiment = new ExpressionExperimentValueObject( dedv.getExpressionExperiment() );
    }

    protected DataVectorValueObject( BulkExpressionDataVector dedv, Collection<Long> genes,
            BioAssayDimensionValueObject badvo ) {
        this( dedv, badvo );
        this.genes = genes;
    }

    /**
     * Copy constructor
     */
    public DataVectorValueObject( DoubleVectorValueObject dvvo ) {
        this.expressionExperiment = dvvo.getExpressionExperiment();
        this.designElement = dvvo.getDesignElement();
        this.quantitationType = dvvo.getQuantitationType();
        this.genes = dvvo.getGenes();
        this.bioAssayDimension = dvvo.getBioAssayDimension();
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof DataVectorValueObject ) ) {
            return false;
        }
        DataVectorValueObject that = ( DataVectorValueObject ) o;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        // genes do not really matter here
        return Objects.equals( expressionExperiment, that.expressionExperiment )
                && Objects.equals( quantitationType, that.quantitationType )
                && Objects.equals( bioAssayDimension, that.bioAssayDimension )
                && Objects.equals( designElement, that.designElement );
    }

    @Override
    public int hashCode() {
        return Objects.hash( expressionExperiment, quantitationType, bioAssayDimension, designElement );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( this.expressionExperiment != null ? " EE=" + this.expressionExperiment.getId() : "" )
                + ( this.designElement != null ? " Probe=" + this.designElement.getId() : "" );
    }

    /**
     * Represents the order of the bioassays for this. It might not be a real (persistent) BioAssayDimension: it might
     * be a subset, or a "padded" one.
     *
     * @return BA dimension VO
     */
    public BioAssayDimensionValueObject getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    public void setBioAssayDimension( BioAssayDimensionValueObject bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public List<BioAssayValueObject> getBioAssays() {
        assert bioAssayDimension != null;
        return bioAssayDimension.getBioAssays();
    }

    public CompositeSequenceValueObject getDesignElement() {
        return designElement;
    }

    public void setDesignElement( CompositeSequenceValueObject designElement ) {
        this.designElement = designElement;
    }

    public ExpressionExperimentValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperimentValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @return the genes
     */
    public Collection<Long> getGenes() {
        return genes;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Collection<Long> genes ) {
        this.genes = genes;
    }

    public QuantitationTypeValueObject getQuantitationType() {
        return quantitationType;
    }

    public void setQuantitationType( QuantitationTypeValueObject quantitationType ) {
        this.quantitationType = quantitationType;
    }

}
