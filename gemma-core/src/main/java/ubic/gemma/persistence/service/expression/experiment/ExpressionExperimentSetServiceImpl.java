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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;

import java.util.ArrayList;
import java.util.Collection;
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

    private static final String AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION = "Automatically generated for %s EEs";

    private final ExpressionExperimentSetDao expressionExperimentSetDao;
    private final ExpressionExperimentService expressionExperimentService;

    @Autowired
    public ExpressionExperimentSetServiceImpl( ExpressionExperimentSetDao expressionExperimentSetDao, ExpressionExperimentService expressionExperimentService ) {
        super( expressionExperimentSetDao );
        this.expressionExperimentSetDao = expressionExperimentSetDao;
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> find( ExpressionExperiment ee ) {
        return this.expressionExperimentSetDao.find( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> findByName( String name ) {
        return this.expressionExperimentSetDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> findByAccession( String accession ) {
        return expressionExperimentSetDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> findIds( ExpressionExperiment ee ) {
        Collection<Long> ids = new ArrayList<>();
        Collection<ExpressionExperimentSet> eesets = this.expressionExperimentSetDao.find( ee );
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
        eeSet.setName( "Master set for " + taxon.getCommonName() );
        eeSet.setDescription( String.format( AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION, expressionExperiments.size() ) );
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
        String regexDesc = String.format( AUTOMATICALLY_GENERATED_EXPERIMENT_GROUP_DESCRIPTION, ".*" );
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
        for ( ExpressionExperiment ee : expressionExperimentSet.getExperiments() ) {
            eeTaxon = expressionExperimentService.getTaxon( ee );

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

    @Override
    @Transactional
    public int removeFromSets( ExpressionExperiment bas ) {
        Collection<ExpressionExperimentSet> sets = expressionExperimentSetDao.find( bas );
        for ( ExpressionExperimentSet eeSet : sets ) {
            log.info( "Removing " + bas + " from " + eeSet );
            eeSet.getExperiments().remove( bas );
            if ( eeSet.getExperiments().isEmpty() ) {
                // remove the set because in only contains this experiment
                // TODO: do we want to check for ACLs? the current user might not have the right to do that, even if the
                //       set is now empty
                log.info( "Removing now empty set " + eeSet );
                expressionExperimentSetDao.remove( eeSet );
            }
        }
        return sets.size();
    }
}