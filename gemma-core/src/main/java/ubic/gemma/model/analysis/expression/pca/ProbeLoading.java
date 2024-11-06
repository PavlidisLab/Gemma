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
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Objects;

/**
 * Only stored for some of the probes (e.g. the top ones)
 */
public class ProbeLoading extends AbstractIdentifiable {

    private CompositeSequence probe;
    private Integer componentNumber;
    private Double loading;
    private Integer loadingRank;

    public CompositeSequence getProbe() {
        return this.probe;
    }

    public void setProbe( CompositeSequence probe ) {
        this.probe = probe;
    }

    /**
     * @return Which component this loading is for (the first component is number 1)
     */
    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    /**
     * @return The raw loading value from the SVD. This corresponds to the values in the left singular vector.
     */
    public Double getLoading() {
        return this.loading;
    }

    public void setLoading( Double loading ) {
        this.loading = loading;
    }

    /**
     * @return The rank of this loading among the ones which were stored for the component
     */
    public Integer getLoadingRank() {
        return this.loadingRank;
    }

    public void setLoadingRank( Integer loadingRank ) {
        this.loadingRank = loadingRank;
    }

    /**
     * @return Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( probe, componentNumber );
    }

    /**
     * @return <code>true</code> if the argument is an ProbeLoading instance and all identifiers for this entity equal
     * the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof ProbeLoading ) ) {
            return false;
        }
        final ProbeLoading that = ( ProbeLoading ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            // rank is not relevant for equality
            return Objects.equals( probe, that.probe )
                    && Objects.equals( componentNumber, that.componentNumber )
                    && Objects.equals( loading, that.loading );
        }
    }

    public static final class Factory {

        public static ProbeLoading newInstance( Integer componentNumber,
                Double loading, Integer loadingRank,
                CompositeSequence probe ) {
            final ProbeLoading entity = new ProbeLoading();
            entity.setComponentNumber( componentNumber );
            entity.setLoading( loading );
            entity.setLoadingRank( loadingRank );
            entity.setProbe( probe );
            return entity;
        }
    }

}