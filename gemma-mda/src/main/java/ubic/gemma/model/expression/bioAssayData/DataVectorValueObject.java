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

import java.io.Serializable;
import java.util.Collection;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public abstract class DataVectorValueObject implements Serializable {

    private static final long serialVersionUID = 4291090102921066018L;

    protected static ByteArrayConverter byteArrayConverter;

    static {
        byteArrayConverter = new ByteArrayConverter();
    }

    protected Long id;

    protected CompositeSequence designElement;
    protected BioAssayDimension bioAssayDimension;
    protected QuantitationType quantitationType;
    protected ExpressionExperimentValueObject expressionExperiment;
    Collection<Gene> genes;

    public DataVectorValueObject() {
    }

    public DataVectorValueObject( DesignElementDataVector dedv ) {
        this.bioAssayDimension = dedv.getBioAssayDimension();
        this.quantitationType = dedv.getQuantitationType();
        this.designElement = dedv.getDesignElement();
        this.expressionExperiment = new ExpressionExperimentValueObject( dedv.getExpressionExperiment() );
        this.id = dedv.getId();
    }

    public DataVectorValueObject( DesignElementDataVector dedv, Collection<Gene> genes ) {
        this( dedv );
        this.genes = genes;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final DoubleVectorValueObject other = ( DoubleVectorValueObject ) obj;
        if ( id == null ) {
            return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    public BioAssayDimension getBioAssayDimension() {
        return bioAssayDimension;
    }

    public CompositeSequence getDesignElement() {
        return designElement;
    }

    public ExpressionExperimentValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @return the genes
     */
    public Collection<Gene> getGenes() {
        return genes;
    }

    public Long getId() {
        return id;
    }

    public QuantitationType getQuantitationType() {
        return quantitationType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public void setBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public void setDesignElement( CompositeSequence designElement ) {
        this.designElement = designElement;
    }

    public void setExpressionExperiment( ExpressionExperimentValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Collection<Gene> genes ) {
        this.genes = genes;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setQuantitationType( QuantitationType quantitationType ) {
        this.quantitationType = quantitationType;
    }

    @Override
    public String toString() {
        return "EE=" + this.expressionExperiment.getId() + " Probe=" + this.designElement.getId();
    }

}
