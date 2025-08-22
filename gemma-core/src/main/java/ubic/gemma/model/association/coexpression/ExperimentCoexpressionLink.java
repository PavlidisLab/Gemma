/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Represents coexpression at the level of experiment, referinng to links stored as Gene2GeneCoexpression.
 *
 * @author Paul
 *         FIXME the overrides are uselsess, no specific functionality, they can be represented an enumeration field.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class ExperimentCoexpressionLink {

    private ExpressionExperiment experiment;

    /**
     * we need to have the genes here as a denormalization. We store these links both ways for efficiency.
     */
    private Long firstGene;

    // This ID is actually pretty useless; the experiment+linkId is a unique key
    private Long id;

    /**
     * Note that we do not make this a proper link to a Gene2GeneCoexpression entity, for simplicity, so we cant do some
     * hql stuff.
     */
    private Long linkId;

    private Long secondGene;

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public ExperimentCoexpressionLink( ExpressionExperiment experiment, Long linkId, Long firstGene, Long secondGene ) {
        this.firstGene = firstGene;
        this.secondGene = secondGene;
        this.linkId = linkId;
        this.experiment = experiment;
    }

    public ExpressionExperiment getExperiment() {
        return experiment;
    }

    public void setExperiment( ExpressionExperiment experiment ) {
        this.experiment = experiment;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId( Long linkId ) {
        this.linkId = linkId;
    }

    @Override
    public int hashCode() {
        if ( id != null )
            return id.hashCode();
        final int prime = 31;
        int result = 1;

        result = prime * result + ( ( experiment == null ) ? 0 : experiment.hashCode() );
        result = prime * result + ( ( linkId == null ) ? 0 : linkId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        ExperimentCoexpressionLink other = ( ExperimentCoexpressionLink ) obj;

        if ( id != null )
            return this.getId().equals( other.getId() );

        if ( experiment == null ) {
            if ( other.experiment != null )
                return false;
        } else if ( !experiment.equals( other.experiment ) )
            return false;
        if ( linkId == null ) {
            return other.linkId == null;
        }
        return linkId.equals( other.linkId );
    }

    public Long getFirstGene() {
        return firstGene;
    }

    public void setFirstGene( Long firstGene ) {
        this.firstGene = firstGene;
    }

    public Long getSecondGene() {
        return secondGene;
    }

    public void setSecondGene( Long secondGene ) {
        this.secondGene = secondGene;
    }

}
