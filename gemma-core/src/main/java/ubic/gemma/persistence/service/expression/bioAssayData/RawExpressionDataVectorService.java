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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * @author Paul
 */
public interface RawExpressionDataVectorService extends DesignElementDataVectorService<RawExpressionDataVector> {

    @Override
    @Secured({ "GROUP_ADMIN" })
    RawExpressionDataVector load( Long id );

    /**
     * @deprecated never use this method, instead clear {@link ExpressionExperiment#getProcessedExpressionDataVectors()}
     * directly. The relationship is actually managed by Hibernate.
     */
    @Override
    @Secured({ "GROUP_ADMIN" })
    @Deprecated
    void remove( Collection<RawExpressionDataVector> vectors );

    /**
     * @deprecated never use this method, instead clear {@link ExpressionExperiment#getProcessedExpressionDataVectors()}
     * directly. The relationship is actually managed by Hibernate.
     */
    @Override
    @Secured({ "GROUP_ADMIN" })
    @Deprecated
    void remove( RawExpressionDataVector designElementDataVector );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Collection<RawExpressionDataVector> dedvs );
}
