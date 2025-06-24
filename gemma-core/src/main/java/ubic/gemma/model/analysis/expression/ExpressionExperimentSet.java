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

package ubic.gemma.model.analysis.expression;

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * A grouping of expression studies.
 *
 * @author Paul
 */
@Indexed
public class ExpressionExperimentSet extends AbstractAuditable implements Securable {

    @Nullable
    private DatabaseEntry accession;
    private Taxon taxon;
    private Set<ExpressionExperiment> experiments = new HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExpressionExperimentSet() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExpressionExperimentSet ) )
            return false;
        ExpressionExperimentSet that = ( ExpressionExperimentSet ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else if ( getName() != null && that.getName() != null ) {
            return getName().equals( that.getName() );
        } else {
            return false;
        }
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    @Nullable
    @IndexedEmbedded
    public DatabaseEntry getAccession() {
        return accession;
    }

    public void setAccession( @Nullable DatabaseEntry accession ) {
        this.accession = accession;
    }

    public Set<ExpressionExperiment> getExperiments() {
        return this.experiments;
    }

    public void setExperiments( Set<ExpressionExperiment> experiments ) {
        this.experiments = experiments;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public static final class Factory {
        public static ExpressionExperimentSet newInstance() {
            return new ExpressionExperimentSet();
        }
    }

}