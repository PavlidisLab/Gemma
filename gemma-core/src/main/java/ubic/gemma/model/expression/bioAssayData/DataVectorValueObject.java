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

import lombok.Data;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Base class for value objects of {@link BulkExpressionDataVector}.
 * @author paul
 */
@Data
public abstract class DataVectorValueObject extends IdentifiableValueObject<DataVector> {

    @Nullable
    private BioAssaySetValueObject expressionExperiment;
    private CompositeSequenceValueObject designElement;
    private QuantitationTypeValueObject quantitationType;
    /**
     * Represents the order of the bioassays for this. It might not be a real (persistent) BioAssayDimension: it might
     * be a subset, or a "padded" one.
     */
    private BioAssayDimensionValueObject bioAssayDimension;
    @Nullable
    private Collection<Long> genes;

    protected DataVectorValueObject() {
    }

    protected DataVectorValueObject( Long id ) {
        super( id );
    }

    protected DataVectorValueObject( BulkExpressionDataVector dedv, ExpressionExperimentValueObject eevo, QuantitationTypeValueObject qtVo, BioAssayDimensionValueObject badvo, ArrayDesignValueObject advo, @Nullable Collection<Long> genes ) {
        super( dedv );
        this.bioAssayDimension = badvo;
        this.quantitationType = qtVo;
        this.designElement = new CompositeSequenceValueObject( dedv.getDesignElement(), advo );
        this.expressionExperiment = eevo;
        this.genes = genes;
    }

    /**
     * Copy constructor
     */
    public DataVectorValueObject( DoubleVectorValueObject dvvo ) {
        super( dvvo );
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

    public List<BioAssayValueObject> getBioAssays() {
        assert bioAssayDimension != null;
        return bioAssayDimension.getBioAssays();
    }
}
