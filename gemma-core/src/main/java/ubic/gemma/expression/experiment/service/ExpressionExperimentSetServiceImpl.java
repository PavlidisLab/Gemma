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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.expression.experiment.DatabaseBackedExpressionExperimentSetValueObject;
import ubic.gemma.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.security.SecurityService;

/**
 * @version $Id$
 * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService
 */
@Service
public class ExpressionExperimentSetServiceImpl extends
        ubic.gemma.expression.experiment.service.ExpressionExperimentSetServiceBase {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

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
    public DatabaseBackedExpressionExperimentSetValueObject createDatabaseEntity(
            ExpressionExperimentSetValueObject eesvo ) {

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
        if ( eesvo.isPublik() ) {
            securityService.makePublic( newEESet );
        } else {
            securityService.makePrivate( newEESet );
        }

        return expressionExperimentValueObjectHelper.convertToValueObject( newEESet );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#deleteDatabaseEntity(ubic.gemma.expression
     * .experiment.DatabaseBackedExpressionExperimentSetValueObject)
     */
    @Override
    public void deleteDatabaseEntity( DatabaseBackedExpressionExperimentSetValueObject eesvo ) {
        try {
            delete( load( eesvo.getId() ) );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getExpressionExperimentSetDao().find( bioAssaySet );
    }

    @Override
    public Collection<Long> findIds( BioAssaySet bioAssaySet ) {
        Collection<Long> ids = new ArrayList<Long>();
        Collection<ExpressionExperimentSet> eesets = this.getExpressionExperimentSetDao().find( bioAssaySet );
        for ( ExpressionExperimentSet eeset : eesets ) {
            ids.add( eeset.getId() );
        }

        return ids;
    }

    @Override
    public Collection<Long> getExperimentIdsInSet( Long id ) {
        ExpressionExperimentSet eeSet = load( id ); // secure
        Collection<BioAssaySet> datasets = eeSet.getExperiments(); // Not secure.
        Collection<Long> eeids = new HashSet<Long>();
        for ( BioAssaySet ee : datasets ) {
            eeids.add( ee.getId() );
        }
        return eeids;
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.getExpressionExperimentSetDao().getExperimentsInSet( id );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id ) {

        Collection<Long> eeids = getExperimentIdsInSet( id );
        Collection<ExpressionExperimentValueObject> result = expressionExperimentService
                .loadValueObjects( eeids, false );
        expressionExperimentReportService.getReportInformation( result );
        return result;
    }

    @Override
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> getLightValueObjectsFromIds(
            Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return new ArrayList<DatabaseBackedExpressionExperimentSetValueObject>();
        }
        Collection<ExpressionExperimentSet> eeSets = this.load( ids );
        return expressionExperimentValueObjectHelper.convertToLightValueObjects( eeSets );
    }

    @Override
    public DatabaseBackedExpressionExperimentSetValueObject getValueObject( Long id ) {
        ExpressionExperimentSet eeSet = this.load( id );
        return expressionExperimentValueObjectHelper.convertToValueObject( eeSet );
    }

    @Override
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> getValueObjectsFromIds( Collection<Long> ids ) {
        Collection<ExpressionExperimentSet> eeSets = this.load( ids );
        return expressionExperimentValueObjectHelper.convertToValueObjects( eeSets );
    }

    @Override
    public boolean isValidForFrontEnd( ExpressionExperimentSet eeSet ) {
        return ( eeSet.getTaxon() != null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#load(java.util.Collection)
     */
    @Override
    public Collection<ExpressionExperimentSet> load( Collection<Long> ids ) {
        return ( Collection<ExpressionExperimentSet> ) this.getExpressionExperimentSetDao().load( ids );
    }

    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    @Override
    public Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjectsWithTaxon() {
        Collection<ExpressionExperimentSet> sets = this.loadAllExperimentSetsWithTaxon();
        // filtered by security.
        List<ExpressionExperimentSetValueObject> results = new ArrayList<ExpressionExperimentSetValueObject>();

        // should be a small number of items.
        for ( ExpressionExperimentSet set : sets ) {
            ExpressionExperimentSetValueObject vo = expressionExperimentValueObjectHelper.convertToValueObject( set );
            results.add( vo );
        }

        Collections.sort( results );

        return results;
    }

    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadMySets()
     */
    @Override
    public Collection<ExpressionExperimentSet> loadMySets() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    @Override
    public Collection<DatabaseBackedExpressionExperimentSetValueObject> loadMySetValueObjects() {
        return expressionExperimentValueObjectHelper.convertToValueObjects( this.getExpressionExperimentSetDao()
                .loadAllExperimentSetsWithTaxon() );
    }

    @Override
    public Collection<ExpressionExperimentSet> loadMySharedSets() {
        return this.getExpressionExperimentSetDao().loadAllExperimentSetsWithTaxon();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#thaw(ubic.gemma.model.analysis.expression
     * .ExpressionExperimentSet)
     */
    @Override
    public void thaw( ExpressionExperimentSet expressionExperimentSet ) {
        this.getExpressionExperimentSetDao().thaw( expressionExperimentSet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#updateDatabaseEntity(ubic.gemma.expression
     * .experiment.DatabaseBackedExpressionExperimentSetValueObject)
     */
    @Override
    public void updateDatabaseEntity( DatabaseBackedExpressionExperimentSetValueObject eesvo ) {
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
        Collection<BioAssaySet> basColl = new LinkedList<BioAssaySet>();
        for ( ExpressionExperiment experiment : newExperiments ) {
            Taxon eeTaxon = getTaxonForSet( experiment );

            // make sure experiments being added are from the right taxon
            if ( eeTaxon == null || !eeTaxon.equals( eeSet.getTaxon() ) ) {
                throw new IllegalArgumentException( experiment
                        + " is of the wrong taxon to add to eeset. EESet taxon is " + eeSet.getTaxon() );
            }

            basColl.add( experiment );

        }

        eeSet.setExperiments( basColl );

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
    public DatabaseBackedExpressionExperimentSetValueObject updateDatabaseEntityNameDesc(
            DatabaseBackedExpressionExperimentSetValueObject eeSetVO ) {

        Long groupId = eeSetVO.getId();
        ExpressionExperimentSet eeSet = this.load( groupId );
        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded" );
        }

        eeSet.setDescription( eeSetVO.getDescription() );
        if ( eeSetVO.getName() != null && eeSetVO.getName().length() > 0 ) eeSet.setName( eeSetVO.getName() );
        this.update( eeSet );

        return expressionExperimentValueObjectHelper.convertToValueObject( eeSet );

    }

    @Override
    public Collection<ExpressionExperimentSet> validateForFrontEnd( Collection<ExpressionExperimentSet> eeSets ) {
        Collection<ExpressionExperimentSet> valid = new ArrayList<ExpressionExperimentSet>();
        for ( ExpressionExperimentSet eeSet : eeSets ) {
            if ( isValidForFrontEnd( eeSet ) ) {
                valid.add( eeSet );
            }
        }
        return valid;
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleCreate(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        return this.getExpressionExperimentSetDao().create( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        this.getExpressionExperimentSetDao().remove( expressionExperimentSet );
    }

    @Override
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getExpressionExperimentSetDao().findByName( name );
    }

    @Override
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws Exception {
        return this.getExpressionExperimentSetDao().getAnalyses( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#load(java.lang.Long)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleLoad( java.lang.Long id )
            throws java.lang.Exception {
        return this.getExpressionExperimentSetDao().load( id );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadAll()
     */
    @Override
    protected java.util.Collection<ExpressionExperimentSet> handleLoadAll() throws java.lang.Exception {
        return ( Collection<ExpressionExperimentSet> ) this.getExpressionExperimentSetDao().loadAll();

    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected java.util.Collection<ExpressionExperimentSet> handleLoadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user ) throws java.lang.Exception {
        // @todo implement protected java.util.Collection
        // handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user)
        throw new java.lang.UnsupportedOperationException(
                "ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user) Not implemented!" );
    }

    /**
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentSetService#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {

        if ( StringUtils.isBlank( expressionExperimentSet.getName() ) ) {
            throw new IllegalArgumentException( "Attempt to update an ExpressionExperimentSet so it has no name" );
        }

        this.getExpressionExperimentSetDao().update( expressionExperimentSet );
    }

    /**
     * @param experiment
     * @return
     */
    private Taxon getTaxonForSet( BioAssaySet experiment ) {
        Taxon eeTaxon = expressionExperimentService.getTaxon( experiment );

        if ( eeTaxon == null ) {
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