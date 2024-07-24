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

import java.util.Objects;

/**
 * Data for one design element, across one or more bioassays, for a single quantitation type. For example, the
 * "expression profile" for a probe (gene) across a set of samples
 */
public class RawExpressionDataVector extends DesignElementDataVector {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7410374297463625206L;

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( getExpressionExperiment(), getQuantitationType(), getDesignElement(), getBioAssayDimension() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof RawExpressionDataVector ) ) {
            return false;
        }
        final RawExpressionDataVector that = ( RawExpressionDataVector ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return Objects.equals( getExpressionExperiment(), that.getExpressionExperiment() )
                && Objects.equals( getDesignElement(), that.getDesignElement() )
                && Objects.equals( getQuantitationType(), that.getQuantitationType() )
                && Objects.equals( getBioAssayDimension(), that.getBioAssayDimension() );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ( this.getId() == null ? "" : " Id=" + this.getId() )
                + ( this.getDesignElement() == null ? "" : " DE=" + this.getDesignElement().getName() )
                + ( this.getQuantitationType() == null ? "" : " QT=" + this.getQuantitationType().getName() )
                + ( this.getExpressionExperiment() == null ? ""
                : " EE=" + this.getExpressionExperiment().getName() + ", " + this.getData().length + " bytes" );

    }

    public static final class Factory {

        public static RawExpressionDataVector newInstance() {
            return new RawExpressionDataVector();
        }
    }
}