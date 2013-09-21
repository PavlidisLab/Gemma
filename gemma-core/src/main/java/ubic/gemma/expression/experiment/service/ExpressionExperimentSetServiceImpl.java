/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.expression.experiment.service;

import gemma.gsec.SecurityService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;

/**
 * @version $Id$
 * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService
 */
@Service
public class ExpressionExperimentSetServiceImpl extends ExpressionExperimentSetServiceBase {

    private static Logger log = LoggerFactory.getLogger( ExpressionExperimentSetServiceImpl.class );

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentSetValueObjectHelper expressionExperimentValueObjectHelper;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#createDatabaseEntity(ubic.gemma.model
     * .expression.experiment.ExpressionExperimentSetValueObject)
     */
    @Override
    @Transactional
    public ExpressionExperimentSet createFromValueObject( ExpressionExperimentSetValueObject eesvo ) {

        /*
         * Sanity check.
         */
        Collection<ExpressionExperimentSet> dups = findByName( eesvo.getName() );
        if ( dups == null || !dups.isEmpty() ) {
            throw new IllegalArgumentException( "Sorry, there is already a set with that name (" + eesvo.getName()
                    + ")" );
        }

        ExpressionExperimentSet newSet = ExpressionExperimentSet.Factory.newInstance();
        newSet.setName( eesvo.getName() );
        newSet.setDescription( eesvo.getDescription() );

        Collection<? extends BioAssaySet> datasetsAnalyzed = expressionExperimentService.loadMultiple( eesvo
                .getExpressionExperimentIds() );

        newSet.getExperiments().addAll( datasetsAnalyzed );

        if ( eesvo.getTaxonId() != null )
            newSet.setTaxon( taxonService.load( eesvo.getTaxonId() ) );
        else {
            /*
             * Figure out the taxon from the experiments. mustn't be heterogeneous.
             */
            Taxon taxon = null;
            for ( BioAssaySet bioAssaySet : newSet.getExperiments() ) {
                Taxon eeTaxon = getTaxonForSet( bioAssaySet );
                /*
                 * this can be null.
                 */

                if ( taxon == null ) {
                    taxon = eeTaxon;
                } else if ( !eeTaxon.equals( taxon ) ) {
                    throw new UnsupportedOperationException( "EESets with mixed taxa are not supported" );
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

        ExpressionExperimentSet newEESet = create( newSet );

        // make groups private by default
        if ( eesvo.getIsPublic() ) {
            securityService.makePublic( newEESet );
        } else {
            securityService.makePrivate( newEESet );
        }

        return newEESet;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#deleteDatabaseEntity(ubic.gemma.expression
     * .experiment.DatabaseBackedExpressionExperimentSetValueObject)
     */
    @Override
    @Transactional
    public void deleteDatabaseEntity( ExpressionExperimentSetValueObject eesvo ) {
        try {
            delete( load( eesvo.getId() ) );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getExpressionExperimentSetDao().find( bioAssaySet );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> findIds( BioAssaySet bioAssaySet ) {
        Collection<Long> ids = new ArrayList<Long>();
        Collection<ExpressionExperimentSet> eesets = this.getExpressionExperimentSetDao().find( bioAssaySet );
        for ( ExpressionExperimentSet eeset : eesets ) {
            ids.add( eeset.getId() );
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.getExpressionExperimentSetDao().getExperimentsInSet( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id ) {

        return this.getExpressionExperimentSetDao().getExperimentValueObjectsInSet( id );

    }

    /**
     * Instantiate non-persistent experiment set with description = "Automatically generated for ## EEs.". Mostly for
     * use in Gene2GenePopulationServiceImpl.intializeNewAnalysis(Collection<BioAssaySet>, Taxon, Collection<Gene>,
     * String, int). By convention, these sets should not be modifiable.
     * 
     * @see ubic.gemma.analysis.expression.coexpression.Gene2GenePopulationServiceImpl.intializeNewAnalysis(Collection<
     *      BioAssaySet>, Taxon, Collection<Gene>, String, int)
     * @param expressionExperiments
     * @param taxon
     * @param setName
     * @return
     */
    @Override
    public ExpressionExperimentSet initAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon ) {
        ExpressionExperimentSet eeSet;
        eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setTaxon( taxon );
        eeSet.setName( getMasterSetName( taxon ) );
        eeSet.setDescription( String.format(
                ExpressionExperimentSetService.AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION,
                String.valueOf( expressionExperiments.size() ) ) );
        eeSet.getExperiments().addAll( expressionExperiments );
        return eeSet;
    }

    /**
     * Determines if set was automatically generated by matching the description to that used in
     * ubic.gemma.analysis.expression
     * .coexpression.ExpressionExperimentSetService.AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION
     * 
     * @return true if the set was automatically generated, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAutomaticallyGenerated( String experimentSetDescription ) {
        String regexDesc = String.format(
                ExpressionExperimentSetService.AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION, ".*" );
        return experimentSetDescription.matches( regexDesc );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#load(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> load( Collection<Long> ids ) {
        return ( Collection<ExpressionExperimentSet> ) this.getExpressionExperimentSetDao().load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjects() {
        return this.getExpressionExperimentSetDao().loadAllValueObjects();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadMySets()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> loadMySets() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    /*
     * (non-Javadoc) Same as loadAllValueObjects but security filtering is differenet.
     * 
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadMySetValueObjects()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSetValueObject> loadMySetValueObjects() {
        return this.getExpressionExperimentSetDao().loadAllValueObjects();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> loadMySharedSets() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSetValueObject loadValueObject( Long id ) {
        return this.getExpressionExperimentSetDao().loadValueObject( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSetValueObject> loadValueObjects( Collection<Long> eeSetIds ) {
        return this.getExpressionExperimentSetDao().loadValueObjects( eeSetIds );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#thaw(ubic.gemma.model.analysis.expression
     * .ExpressionExperimentSet)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( ExpressionExperimentSet expressionExperimentSet ) {
        this.getExpressionExperimentSetDao().thaw( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    @Transactional
    public void update( final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException( "Cannot update null set" );
        }
        if ( expressionExperimentSet.getId() == null || expressionExperimentSet.getId() < 0 ) {
            throw new IllegalArgumentException( "Can only update an existing eeset (passed id="
                    + expressionExperimentSet.getId() + ")" );
        }

        if ( StringUtils.isBlank( expressionExperimentSet.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        // make sure potentially new experiment members are of the right taxon
        Taxon groupTaxon = expressionExperimentSet.getTaxon();
        Taxon eeTaxon = null;
        for ( BioAssaySet ee : expressionExperimentSet.getExperiments() ) {
            eeTaxon = getTaxonForSet( ee );

            if ( !eeTaxon.equals( groupTaxon ) ) {
                throw new IllegalArgumentException( "Failed to add experiments of wrong taxa (" + ee + ") to eeset. "
                        + "EESet taxon is " + groupTaxon + ", experiment was " + eeTaxon );
            }
        }

        this.handleUpdate( expressionExperimentSet );

    }

    /**
     * Check for an old expression experiment set that has the same name as the one we want to use. If it exists, check
     * if it contains exactly the experiments we want to use. If it does, return it. Otherwise rename the old one and
     * return null.
     * <p>
     * FIXME this needs to be refactored so we aren't passing the experiment in. I also don't like identifying it by
     * name.
     * 
     * @param analysisName
     * @param expressionExperiments
     * @return expressionExperimentSet if a usable old one exists.
     */
    @Override
    @Transactional
    public ExpressionExperimentSet updateAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon ) {
        Collection<ExpressionExperimentSet> oldEESets = findByName( getMasterSetName( taxon ) );
        ExpressionExperimentSet existing = null;

        if ( oldEESets.size() > 0 ) {

            for ( ExpressionExperimentSet oldSet : oldEESets ) {
                thaw( oldSet );

                if ( !oldSet.getTaxon().equals( taxon ) ) {
                    log.warn( "There is a EEset the name '" + "Master set for " + taxon.getCommonName()
                            + "' but taxon is " + oldSet.getTaxon() + ", not expected " + taxon );
                    // skip it. Though this could be a data error
                    continue;
                } else if ( oldSet.getExperiments().containsAll( expressionExperiments )
                        && oldSet.getExperiments().size() == expressionExperiments.size() ) {
                    log.info( "Reusing an old EE set that has the same experiments as those requested" );
                    existing = oldSet;
                    break;
                } else {
                    /*
                     * Really we should delete it.
                     */
                }

                // This means the old set is to be retired. TODO: delete older old sets so we don't have
                if ( !oldSet.getName().contains( "(old" ) ) {
                    log.info( "Flagging old EEset '" + oldSet.getName() + "'as 'old'" );
                    oldSet.setName( oldSet.getName() + " (old)" );
                    update( oldSet );
                }
            }
        }

        if ( existing != null ) return existing;

        // If we get here, it means there isn't one for this taxon, already.
        return initAutomaticallyGeneratedExperimentSet( expressionExperiments, taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#updateDatabaseEntity(ubic.gemma.expression
     * .experiment.DatabaseBackedExpressionExperimentSetValueObject)
     */
    @Override
    @Transactional
    public void updateDatabaseEntity( ExpressionExperimentSetValueObject eesvo ) {
        try {
            ExpressionExperimentSet eeset = expressionExperimentValueObjectHelper.convertToEntity( eesvo );
            if ( eeset == null ) {
                throw new IllegalArgumentException( "Cannot update null set" );
            }
            update( eeset );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * update the members of the experiment set with the given ids
     * 
     * @param groupId set to update
     * @param eeIds new set member ids
     * @return error message or null if no errors
     */
    @Override
    @Transactional
    public String updateDatabaseEntityMembers( Long groupId, Collection<Long> eeIds ) {

        String msg = null;
        if ( eeIds.isEmpty() ) {
            throw new IllegalArgumentException( "No expression experiment ids provided. Cannot save an empty set." );

        }
        ExpressionExperimentSet eeSet = this.load( groupId );

        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded. "
                    + "Either it does not exist or you do not have permission to view it." );
        }

        // check that new member ids are valid
        Collection<ExpressionExperiment> newExperiments = expressionExperimentService.loadMultiple( eeIds );

        if ( newExperiments.isEmpty() ) {
            throw new IllegalArgumentException( "None of the experiment ids were valid (out of " + eeIds.size()
                    + " provided)" );
        }
        if ( newExperiments.size() < eeIds.size() ) {
            throw new IllegalArgumentException( "Some of the experiment ids were invalid: only found "
                    + newExperiments.size() + " out of " + eeIds.size() + " provided)" );
        }

        assert newExperiments.size() == eeIds.size();
        Collection<BioAssaySet> basColl = new HashSet<BioAssaySet>();
        for ( ExpressionExperiment experiment : newExperiments ) {
            Taxon eeTaxon = getTaxonForSet( experiment );

            // make sure experiments being added are from the right taxon
            if ( eeTaxon == null || !eeTaxon.equals( eeSet.getTaxon() ) ) {
                throw new IllegalArgumentException( experiment
                        + " is of the wrong taxon to add to eeset. EESet taxon is " + eeSet.getTaxon() );
            }

            basColl.add( experiment );

        }

        eeSet.getExperiments().clear();
        eeSet.getExperiments().addAll( basColl );

        this.update( eeSet );

        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#updateDatabaseEntityNameDesc(ubic.gemma
     * .expression.experiment.DatabaseBackedExpressionExperimentSetValueObject)
     */
    @Override
    @Transactional
    public ExpressionExperimentSetValueObject updateDatabaseEntityNameDesc( ExpressionExperimentSetValueObject eeSetVO ) {

        Long groupId = eeSetVO.getId();
        ExpressionExperimentSet eeSet = this.load( groupId );
        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded" );
        }

        eeSet.setDescription( eeSetVO.getDescription() );
        if ( eeSetVO.getName() != null && eeSetVO.getName().length() > 0 ) eeSet.setName( eeSetVO.getName() );
        this.update( eeSet );

        return this.loadValueObject( eeSet.getId() );

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleCreate(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        return this.getExpressionExperimentSetDao().create( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        this.getExpressionExperimentSetDao().remove( expressionExperimentSet );
    }

    @Override
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) {
        return this.getExpressionExperimentSetDao().findByName( name );
    }

    @Override
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet ) {
        return this.getExpressionExperimentSetDao().getAnalyses( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#load(java.lang.Long)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleLoad( java.lang.Long id ) {
        return this.getExpressionExperimentSetDao().load( id );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadAll()
     */
    @Override
    protected java.util.Collection<ExpressionExperimentSet> handleLoadAll() {
        return ( Collection<ExpressionExperimentSet> ) this.getExpressionExperimentSetDao().loadAll();

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected java.util.Collection<ExpressionExperimentSet> handleLoadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user ) {
        // @todo implement protected java.util.Collection
        // handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user)
        throw new java.lang.UnsupportedOperationException(
                "ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user) Not implemented!" );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {

        if ( StringUtils.isBlank( expressionExperimentSet.getName() ) ) {
            throw new IllegalArgumentException( "Attempt to update an ExpressionExperimentSet so it has no name" );
        }

        this.getExpressionExperimentSetDao().update( expressionExperimentSet );
    }

    /**
     * @param taxon
     * @return
     */
    private String getMasterSetName( Taxon taxon ) {
        return "Master set for " + taxon.getCommonName();
    }

    /**
     * @param experiment
     * @return
     */
    private Taxon getTaxonForSet( BioAssaySet experiment ) {
        Taxon eeTaxon = expressionExperimentService.getTaxon( experiment );

        if ( eeTaxon == null ) {
            // can happen if the experiment has no samples.
            return null;
        }

        // get top level parent taxon
        while ( eeTaxon.getParentTaxon() != null ) {
            eeTaxon = eeTaxon.getParentTaxon();
        }

        assert eeTaxon != null;
        return eeTaxon;
    }
}