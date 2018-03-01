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

public class Eigenvalue implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6675153115416719020L;
    private Integer componentNumber;
    private Double value;
    private Double varianceFraction;
    private Long id;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Eigenvalue() {
    }

    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
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
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
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
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    public static final class Factory {

        public static ubic.gemma.model.analysis.expression.pca.Eigenvalue newInstance() {
            return new ubic.gemma.model.analysis.expression.pca.Eigenvalue();
        }

    }

}