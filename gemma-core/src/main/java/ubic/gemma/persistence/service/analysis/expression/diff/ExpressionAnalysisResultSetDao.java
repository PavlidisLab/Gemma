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
package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetDao;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.List;

/**
 * @see ExpressionAnalysisResultSet
 */
public interface ExpressionAnalysisResultSetDao extends AnalysisResultSetDao<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledDao<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> {

    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet resultSet );

    /**
     * @param resultSet Only thaws the factor not the probe information
     */
    void thawLite( ExpressionAnalysisResultSet resultSet );

    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    /**
     * Thaw everything but contrasts.
     *
     *  - results
     *  - probe
     *  - probe.biologicalCharacteristic
     *  - probe.biologicalCharacteristic.sequenceDatabaseEntry
     *  - experimentalFactors
     *  - experimentalFactors.factorValues
     *
     * @param resultSet
     * @return
     */
    ExpressionAnalysisResultSet thawWithoutContrasts( ExpressionAnalysisResultSet resultSet );

    /**
     * Retrieve result sets associated to a set of {@link BioAssaySet} and external database entries.
     *
     * @param bioAssaySets related {@link BioAssaySet}, or any if null
     * @param databaseEntries related external identifier associated to the {@link BioAssaySet}, or any if null
     * @param objectFilters list of object filters
     * @param limit maximum number of results to return
     * @param orderBy field by which the collection is sorted
     * @param isAsc whether the sort should be ascending or descending
     * @return
     */
    Slice<ExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> databaseEntries, List<ObjectFilter[]> objectFilters, int offset, int limit, Sort sort );
}
