/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class will handle population of ExpressionExperimentSetValueObjects. Services need to be accessed in order to
 * fill size, experiment ids, and publik/private fields.
 *
 * @author tvrossum
 *
 */
@Component
@CommonsLog
public class ExpressionExperimentSetValueObjectHelperImpl implements ExpressionExperimentSetValueObjectHelper {

    private final ExpressionExperimentSetService expressionExperimentSetService;
    private final ExpressionExperimentService expressionExperimentService;
    private final TaxonService taxonService;
    private final SecurityService securityService;

    @Autowired
    public ExpressionExperimentSetValueObjectHelperImpl( ExpressionExperimentSetService expressionExperimentSetService, ExpressionExperimentService expressionExperimentService, TaxonService taxonService, SecurityService securityService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
        this.expressionExperimentService = expressionExperimentService;
        this.taxonService = taxonService;
        this.securityService = securityService;
    }


    @Override
    @Transactional
    public ExpressionExperimentSet create( ExpressionExperimentSetValueObject eesvo ) {

        /*
         * Sanity check.
         */
        Collection<ExpressionExperimentSet> dups = expressionExperimentSetService.findByName( eesvo.getName() );
        if ( dups == null || !dups.isEmpty() ) {
            throw new IllegalArgumentException(
                    "Sorry, there is already a set with that name (" + eesvo.getName() + ")" );
        }

        ExpressionExperimentSet newSet = ExpressionExperimentSet.Factory.newInstance();
        newSet.setName( eesvo.getName() );
        newSet.setDescription( eesvo.getDescription() );

        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.load(
                eesvo.getExpressionExperimentIds() );

        newSet.getExperiments().addAll( datasetsAnalyzed );

        if ( eesvo.getTaxonId() != null )
            newSet.setTaxon( taxonService.load( eesvo.getTaxonId() ) );
        else {
            /*
             * Figure out the taxon from the experiments. mustn't be heterogeneous.
             */
            Taxon taxon = null;
            for ( BioAssaySet bioAssaySet : newSet.getExperiments() ) {
                Taxon eeTaxon = expressionExperimentService.getTaxon( bioAssaySet );
                /*
                 * this can be null.
                 */

                if ( taxon == null ) {
                    taxon = eeTaxon;
                } else {
                    assert eeTaxon != null;
                    if ( !eeTaxon.equals( taxon ) ) {
                        throw new UnsupportedOperationException( "EESets with mixed taxa are not supported" );
                    }
                }
            }

            if ( taxon == null ) {
                throw new IllegalStateException( "Could not determine taxon for new EEset" );
            }
            newSet.setTaxon( taxon );

        }

        if ( newSet.getTaxon() == null ) {
            throw new IllegalArgumentException( "Unable to determine the taxon for the EESet" );
        }

        ExpressionExperimentSet newEESet = expressionExperimentSetService.create( newSet );

        // make groups private by default
        if ( eesvo.getIsPublic() ) {
            securityService.makePublic( newEESet );
        } else {
            securityService.makePrivate( newEESet );
        }

        return newEESet;

    }

    @Override
    @Transactional
    public void update( ExpressionExperimentSetValueObject eesvo ) {
        Assert.notNull( eesvo, "Cannot update null set" );
        Assert.notNull( eesvo.getId(), "Experiment set VO must have a non-null ID." );
        ExpressionExperimentSet eeset = convertToEntity( eesvo );
        expressionExperimentSetService.update( eeset );
    }

    @Override
    @Transactional
    public ExpressionExperimentSetValueObject updateNameAndDescription( ExpressionExperimentSetValueObject eeSetVO,
            boolean loadEEIds ) {

        Long groupId = eeSetVO.getId();
        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( groupId );
        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded" );
        }

        eeSet.setDescription( eeSetVO.getDescription() );
        if ( eeSetVO.getName() != null && !eeSetVO.getName().isEmpty() )
            eeSet.setName( eeSetVO.getName() );
        expressionExperimentSetService.update( eeSet );

        return expressionExperimentSetService.loadValueObjectById( eeSet.getId(), loadEEIds );
    }

    @Override
    @Transactional
    public void updateMembers( Long groupId, Collection<Long> eeIds ) {

        if ( eeIds.isEmpty() ) {
            throw new IllegalArgumentException( "No expression experiment ids provided. Cannot save an empty set." );

        }
        ExpressionExperimentSet eeSet = expressionExperimentSetService.load( groupId );

        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded. "
                    + "Either it does not exist or you do not have permission to view it." );
        }

        // check that new member ids are valid
        Collection<ExpressionExperiment> newExperiments = expressionExperimentService.load( eeIds );

        if ( newExperiments.isEmpty() ) {
            throw new IllegalArgumentException(
                    "None of the experiment ids were valid (out of " + eeIds.size() + " provided)" );
        }
        if ( newExperiments.size() < eeIds.size() ) {
            throw new IllegalArgumentException(
                    "Some of the experiment ids were invalid: only found " + newExperiments.size() + " out of "
                            + eeIds.size() + " provided)" );
        }

        assert newExperiments.size() == eeIds.size();
        Collection<BioAssaySet> basColl = new HashSet<>();
        for ( ExpressionExperiment experiment : newExperiments ) {
            Taxon eeTaxon = expressionExperimentService.getTaxon( experiment );

            // make sure experiments being added are from the right taxon
            if ( eeTaxon == null || !eeTaxon.equals( eeSet.getTaxon() ) ) {
                throw new IllegalArgumentException(
                        experiment + " is of the wrong taxon to add to eeset. EESet taxon is " + eeSet.getTaxon() );
            }

            basColl.add( experiment );

        }

        eeSet.getExperiments().clear();
        eeSet.getExperiments().addAll( basColl );

        expressionExperimentSetService.update( eeSet );
    }

    @Override
    @Transactional
    public void delete( ExpressionExperimentSetValueObject eesvo ) {
        expressionExperimentSetService.remove( expressionExperimentSetService.loadOrFail( eesvo.getId() ) );
    }

    /**
     * Tries to load an existing experiment set with the param's id, if no experiment can be loaded, create a new one
     * with id = null. Sets all fields of the new entity with values from the valueObject param.
     *
     * @param setVO if null, returns null
     * @return ee set
     */
    public ExpressionExperimentSet convertToEntity( ExpressionExperimentSetValueObject setVO ) {
        if ( setVO == null ) {
            return null;
        }
        ExpressionExperimentSet entity;
        if ( setVO.getId() == null || setVO.getId() < 0 ) {
            entity = ExpressionExperimentSet.Factory.newInstance();
            entity.setId( null );
        } else {
            entity = expressionExperimentSetService.loadOrFail( setVO.getId() );
        }

        entity.setDescription( setVO.getDescription() );
        Collection<ExpressionExperiment> experiments = expressionExperimentService
                .load( setVO.getExpressionExperimentIds() );

        if ( experiments.isEmpty() ) {
            throw new IllegalArgumentException(
                    "The value object must have some experiments associated before it can be converted and persisted" );
        }

        Set<BioAssaySet> bas = new HashSet<BioAssaySet>( experiments );
        entity.setExperiments( bas );
        entity.setName( setVO.getName() );

        if ( setVO.getTaxonId() != null && setVO.getTaxonId() >= 0 ) {
            Taxon tax = taxonService.load( setVO.getTaxonId() );
            entity.setTaxon( tax );
        } else {
            log.debug( "Trying to convert DatabaseBackedExpressionExperimentSetValueObject with id =" + setVO.getId()
                    + " to ExpressionExperimentSet entity. Unmatched ValueObject.getTaxonId() was :" + setVO
                    .getTaxonId() );
        }

        return entity;
    }
}
