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
package ubic.gemma.persistence.service.analysis.expression;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @see ExpressionExperimentSet
 */
public interface ExpressionExperimentSetDao
        extends BaseVoEnabledDao<ExpressionExperimentSet, ExpressionExperimentSetValueObject> {

    /**
     * @param ee bio assay set
     * @return expressionExperimentSets that contain the given ee.
     */
    Collection<ExpressionExperimentSet> find( ExpressionExperiment ee );

    Collection<ExpressionExperimentSet> findByName( String name );

    Collection<ExpressionExperimentSet> findByAccession( String accession );

    Collection<ExpressionExperimentSet> findByAccession( String accession, ExternalDatabase externalDatabase );

    /**
     * @param id id
     * @return the security-filtered list of experiments in a set. It is possible for the return to be empty even if the set
     * is not (due to security filters). Use this instead of expressionExperimentSet.getExperiments.
     */
    Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them &amp; have a taxon value.
     */
    Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();

    void thaw( ExpressionExperimentSet expressionExperimentSet );

    /**
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return ee vos
     */
    Collection<ExpressionExperimentSetValueObject> loadAllValueObjects( boolean loadEEIds );

    /**
     * @param eeSetIds  ids
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return ee vos
     */
    List<ExpressionExperimentSetValueObject> loadValueObjects( Collection<Long> eeSetIds, boolean loadEEIds );

    Collection<ExpressionExperimentDetailsValueObject> getExperimentValueObjectsInSet( Long id );

    /**
     * @param id        id
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     * @return ee vos
     */
    @Nullable
    ExpressionExperimentSetValueObject loadValueObject( Long id, boolean loadEEIds );
}
