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

import ubic.gemma.model.common.AbstractIdentifiable;

import java.util.Objects;

public class Eigenvalue extends AbstractIdentifiable {

    private Integer componentNumber;
    private Double value;
    private Double varianceFraction;

    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    public Double getValue() {
        return this.value;
    }

    public void setValue( Double value ) {
        this.value = value;
    }

    public Double getVarianceFraction() {
        return this.varianceFraction;
    }

    public void setVarianceFraction( Double varianceFraction ) {
        this.varianceFraction = varianceFraction;
    }

    @Override
    public int hashCode() {
        return Objects.hash( componentNumber, value, varianceFraction );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Eigenvalue ) ) {
            return false;
        }
        final Eigenvalue that = ( Eigenvalue ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return Objects.equals( componentNumber, that.componentNumber )
                    && Objects.equals( value, that.value )
                    && Objects.equals( varianceFraction, that.varianceFraction );
        }
    }

    public static final class Factory {

        public static ubic.gemma.model.analysis.expression.pca.Eigenvalue newInstance() {
            return new ubic.gemma.model.analysis.expression.pca.Eigenvalue();
        }

    }

}