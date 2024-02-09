/*
 * The Gemma_sec1 project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.persistence.service;

import org.springframework.security.access.annotation.Secured;

import java.nio.file.Path;

/**
 * @author paul
 */
public interface TableMaintenanceUtil {

    /**
     * Query space used by the {@code GENE2CS} table.
     * <p>
     * You may also want to synchronize to {@link ubic.gemma.model.expression.arrayDesign.ArrayDesign},
     * {@link ubic.gemma.model.expression.designElement.CompositeSequence} and {@link ubic.gemma.model.genome.Gene}
     * since entries in the GENE2CS table are removed in cascade.
     */
    String GENE2CS_QUERY_SPACE = "GENE2CS";

    /**
     * Query space used by the {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} table.
     * <p>
     * You may also want to synchronize to {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} and
     * {@link ubic.gemma.model.common.description.Characteristic} since entries in the EE2C table are removed in
     * cascade.
     */
    String EE2C_QUERY_SPACE = "EXPRESSION_EXPERIMENT2CHARACTERISTIC";

    /**
     * Query space used by the {@code EXPRESSION_EXPERIMENT2ARRAY_DESIGN} table.
     * <p>
     * You may also want to synchronize to {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} and
     * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesign} since entries in the EE2AD table are removed in
     * cascade.
     */
    String EE2AD_QUERY_SPACE = "EXPRESSION_EXPERIMENT2_ARRAY_DESIGN";

    /**
     * If necessary, update the GENE2CS table.
     */
    @Secured({ "GROUP_AGENT" })
    void updateGene2CsEntries();

    /**
     * Update the {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} table.
     * @return the number of records that were created or updated
     */
    @Secured({ "GROUP_AGENT" })
    int updateExpressionExperiment2CharacteristicEntries();

    /**
     * Update the {@code EXPRESSION_EXPERIMENT2_ARRAY_DESIGN} table.
     * @return the number of records that were created or updated
     */
    @Secured({ "GROUP_AGENT" })
    int updateExpressionExperiment2ArrayDesignEntries();

    @Secured({ "GROUP_ADMIN" })
    void setGene2CsInfoPath( Path gene2CsInfoPath );

    // for tests only, to keep from getting emails.
    @Secured({ "GROUP_ADMIN" })
    void disableEmail();
}