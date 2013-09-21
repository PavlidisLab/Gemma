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

/**
 * Only stored for some of the probes (e.g. the top ones)
 */
public abstract class ProbeLoading implements java.io.Serializable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.analysis.expression.pca.ProbeLoading}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.pca.ProbeLoading}.
         */
        public static ubic.gemma.model.analysis.expression.pca.ProbeLoading newInstance() {
            return new ubic.gemma.model.analysis.expression.pca.ProbeLoadingImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.analysis.expression.pca.ProbeLoading}, taking all
         * possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.analysis.expression.pca.ProbeLoading newInstance( Integer componentNumber,
                Double loading, Integer loadingRank, ubic.gemma.model.expression.designElement.CompositeSequence probe ) {
            final ubic.gemma.model.analysis.expression.pca.ProbeLoading entity = new ubic.gemma.model.analysis.expression.pca.ProbeLoadingImpl();
            entity.setComponentNumber( componentNumber );
            entity.setLoading( loading );
            entity.setLoadingRank( loadingRank );
            entity.setProbe( probe );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -2328381846024383305L;
    private Integer componentNumber;

    private Double loading;

    private Integer loadingRank;

    private Long id;

    private ubic.gemma.model.expression.designElement.CompositeSequence probe;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ProbeLoading() {
    }

    /**
     * Returns <code>true</code> if the argument is an ProbeLoading instance and all identifiers for this entity equal
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
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * Which component this loading is for (the first component is number 1)
     */
    public Integer getComponentNumber() {
        return this.componentNumber;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * The raw loading value from the SVD. This corresponds to the values in the left singular vector.
     */
    public Double getLoading() {
        return this.loading;
    }

    /**
     * The rank of this loading among the ones which were stored for the component
     */
    public Integer getLoadingRank() {
        return this.loadingRank;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence getProbe() {
        return this.probe;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setComponentNumber( Integer componentNumber ) {
        this.componentNumber = componentNumber;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setLoading( Double loading ) {
        this.loading = loading;
    }

    public void setLoadingRank( Integer loadingRank ) {
        this.loadingRank = loadingRank;
    }

    public void setProbe( ubic.gemma.model.expression.designElement.CompositeSequence probe ) {
        this.probe = probe;
    }

}