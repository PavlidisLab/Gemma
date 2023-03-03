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
package ubic.gemma.persistence.service.analysis;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.Map;

/**
 * @see ubic.gemma.model.analysis.Analysis
 */
public interface AnalysisDao<T extends SingleExperimentAnalysis> extends BaseDao<T> {

    /**
     * @param name name
     * @return a collection of analysis that have a name that starts with the given name
     */
    Collection<T> findByName( String name );

    Collection<T> findByTaxon( Taxon taxon );
}
