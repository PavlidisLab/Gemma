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
package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Spring Service base class for <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSetService</code>,
 * provides access to all services and entities referenced by this service.
 *
 * @see ExpressionExperimentSetService
 */
@Service
public class ExpressionExperimentSetServiceImpl
        extends AbstractVoEnabledService<ExpressionExperimentSet, ExpressionExperimentSetValueObject>
        implements ExpressionExperimentSetService {

    private final ExpressionExperimentSetDao expressionExperimentSetDao;
    private SecurityService securityService;
    private ExpressionExperimentService expressionExperimentService;
    private TaxonService taxonService;
    private ExpressionExperimentSetValueObjectHelper expressionExperimentValueObjectHelper;

    @Autowired
    public ExpressionExperimentSetServiceImpl( ExpressionExperimentSetDao expressionExperimentSetDao ) {
        super( expressionExperimentSetDao );
        this.expressionExperimentSetDao = expressionExperimentSetDao;
    }

    @Autowired
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    @Autowired
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Autowired
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    @Autowired
    public void setExpressionExperimentValueObjectHelper(
            ExpressionExperimentSetValueObjectHelper expressionExperimentValueObjectHelper ) {
        this.expressionExperimentValueObjectHelper = expressionExperimentValueObjectHelper;
    }

    @Override
    @Transactional
    public ExpressionExperimentSet createFromValueObject( ExpressionExperimentSetValueObject eesvo ) {

        /*
         * Sanity check.
         */
        Collection<ExpressionExperimentSet> dups = this.findByName( eesvo.getName() );
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
                Taxon eeTaxon = this.getTaxonForSet( bioAssaySet );
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

        ExpressionExperimentSet newEESet = this.create( newSet );

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
    public void deleteDatabaseEntity( ExpressionExperimentSetValueObject eesvo ) {
        try {
            this.remove( this.loadOrFail( eesvo.getId() ) );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.expressionExperimentSetDao.find( bioAssaySet );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> findByName( String name ) {
        return this.expressionExperimentSetDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> findIds( BioAssaySet bioAssaySet ) {
        Collection<Long> ids = new ArrayList<>();
        Collection<ExpressionExperimentSet> eesets = this.expressionExperimentSetDao.find( bioAssaySet );
        for ( ExpressionExperimentSet eeset : eesets ) {
            ids.add( eeset.getId() );
        }

        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.expressionExperimentSetDao.getExperimentsInSet( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentDetailsValueObject> getExperimentValueObjectsInSet( Long id ) {
        return this.expressionExperimentSetDao.getExperimentValueObjectsInSet( id );
    }

    /**
     * Instantiate non-persistent experiment set with description = "Automatically generated for ## EEs.". Mostly for
     * use in Gene2GenePopulationServiceImpl.intializeNewAnalysis(Collection, Taxon, Collection,
     * String, int). By convention, these sets should not be modifiable.
     */
    @Override
    public ExpressionExperimentSet initAutomaticallyGeneratedExperimentSet(
            Collection<ExpressionExperiment> expressionExperiments, Taxon taxon ) {
        ExpressionExperimentSet eeSet;
        eeSet = ExpressionExperimentSet.Factory.newInstance();
        eeSet.setTaxon( taxon );
        eeSet.setName( this.getMasterSetName( taxon ) );
        eeSet.setDescription(
                String.format( ExpressionExperimentSetService.AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION,
                        String.valueOf( expressionExperiments.size() ) ) );
        eeSet.getExperiments().addAll( expressionExperiments );
        return eeSet;
    }

    /**
     * Determines if set was automatically generated by matching the description to that used in
     * ubic.gemma.core.analysis.expression
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

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.expressionExperimentSetDao.loadAllExperimentSetsWithTaxon();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSetValueObject> loadAllExperimentSetValueObjects( boolean loadEEIds ) {
        return this.expressionExperimentSetDao.loadAllValueObjects( loadEEIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSetValueObject> loadMySetValueObjects( boolean loadEEIds ) {
        return this.expressionExperimentSetDao.loadAllValueObjects( loadEEIds );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSetValueObject loadValueObjectById( Long id, boolean loadEEIds ) {
        return this.expressionExperimentSetDao.loadValueObject( id, loadEEIds );
    }

    @Override
    @Transactional
    public void updateDatabaseEntity( ExpressionExperimentSetValueObject eesvo ) {
        try {
            ExpressionExperimentSet eeset = expressionExperimentValueObjectHelper.convertToEntity( eesvo );
            if ( eeset == null ) {
                throw new IllegalArgumentException( "Cannot update null set" );
            }
            this.update( eeset );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * update the members of the experiment set with the given ids
     *
     * @param groupId set to update
     * @param eeIds   new set member ids
     */
    @Override
    @Transactional
    public void updateDatabaseEntityMembers( Long groupId, Collection<Long> eeIds ) {

        if ( eeIds.isEmpty() ) {
            throw new IllegalArgumentException( "No expression experiment ids provided. Cannot save an empty set." );

        }
        ExpressionExperimentSet eeSet = this.load( groupId );

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
            Taxon eeTaxon = this.getTaxonForSet( experiment );

            // make sure experiments being added are from the right taxon
            if ( eeTaxon == null || !eeTaxon.equals( eeSet.getTaxon() ) ) {
                throw new IllegalArgumentException(
                        experiment + " is of the wrong taxon to add to eeset. EESet taxon is " + eeSet.getTaxon() );
            }

            basColl.add( experiment );

        }

        eeSet.getExperiments().clear();
        eeSet.getExperiments().addAll( basColl );

        this.update( eeSet );
    }

    @Override
    @Transactional
    public ExpressionExperimentSetValueObject updateDatabaseEntityNameDesc( ExpressionExperimentSetValueObject eeSetVO,
            boolean loadEEIds ) {

        Long groupId = eeSetVO.getId();
        ExpressionExperimentSet eeSet = this.load( groupId );
        if ( eeSet == null ) {
            throw new IllegalArgumentException( "No experiment set with id=" + groupId + " could be loaded" );
        }

        eeSet.setDescription( eeSetVO.getDescription() );
        if ( eeSetVO.getName() != null && eeSetVO.getName().length() > 0 )
            eeSet.setName( eeSetVO.getName() );
        this.update( eeSet );

        return this.loadValueObjectById( eeSet.getId(), loadEEIds );

    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSetValueObject loadValueObjectById( Long id ) {
        return this.expressionExperimentSetDao.loadValueObject( id, false );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperimentSetValueObject> loadValueObjectsByIds( Collection<Long> eeSetIds ) {
        return this.expressionExperimentSetDao.loadValueObjects( eeSetIds, false );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSet thaw( ExpressionExperimentSet expressionExperimentSet ) {
        expressionExperimentSet = loadOrFail( expressionExperimentSet.getId() );
        this.expressionExperimentSetDao.thaw( expressionExperimentSet );
        return expressionExperimentSet;
    }

    /**
     * @see ExpressionExperimentSetService#update(ExpressionExperimentSet)
     */
    @Override
    @Transactional
    public void update( final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException( "Cannot update null set" );
        }
        if ( expressionExperimentSet.getId() == null || expressionExperimentSet.getId() < 0 ) {
            throw new IllegalArgumentException(
                    "Can only update an existing eeset (passed id=" + expressionExperimentSet.getId() + ")" );
        }

        if ( StringUtils.isBlank( expressionExperimentSet.getName() ) ) {
            throw new IllegalArgumentException( "You must provide a name" );
        }

        // make sure potentially new experiment members are of the right taxon
        Taxon groupTaxon = expressionExperimentSet.getTaxon();
        Taxon eeTaxon;
        for ( BioAssaySet ee : expressionExperimentSet.getExperiments() ) {
            eeTaxon = this.getTaxonForSet( ee );

            if ( eeTaxon == null ) {
                // this can happen if there are 0 samples
                continue;
            }

            if ( !eeTaxon.equals( groupTaxon ) ) {
                throw new IllegalArgumentException(
                        "Failed to add experiments of wrong taxa (" + ee + ") to eeset. " + "EESet taxon is "
                                + groupTaxon + ", experiment was " + eeTaxon );
            }
        }

        if ( StringUtils.isBlank( expressionExperimentSet.getName() ) ) {
            throw new IllegalArgumentException( "Attempt to update an ExpressionExperimentSet so it has no name" );
        }

        super.update( expressionExperimentSet );
    }

    @Override
    @Transactional
    public void update( Collection<ExpressionExperimentSet> entities ) {
        entities.forEach( this::update );
    }

    private String getMasterSetName( Taxon taxon ) {
        return "Master set for " + taxon.getCommonName();
    }

    private Taxon getTaxonForSet( BioAssaySet experiment ) {
        Taxon eeTaxon = expressionExperimentService.getTaxon( experiment );

        if ( eeTaxon == null ) {
            // can happen if the experiment has no samples.
            return null;
        }

        return eeTaxon;
    }
}