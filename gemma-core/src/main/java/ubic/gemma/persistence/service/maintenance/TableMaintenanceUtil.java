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
package ubic.gemma.persistence.service.maintenance;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * @author paul
 */
public interface TableMaintenanceUtil {

    /**
     * Query space used by the {@code GENE2CS} table.
     * <p>
     * You may also want to synchronize to {@link ArrayDesign},
     * {@link ubic.gemma.model.expression.designElement.CompositeSequence} and {@link ubic.gemma.model.genome.Gene}
     * since entries in the GENE2CS table are removed in cascade.
     */
    String GENE2CS_QUERY_SPACE = "GENE2CS";

    /**
     * Recommended batch size to use when retrieving entries from the GENE2CS table either by gene or design element.
     */
    int GENE2CS_BATCH_SIZE = 2048;

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
     * {@link ArrayDesign} since entries in the EE2AD table are removed in
     * cascade.
     */
    String EE2AD_QUERY_SPACE = "EXPRESSION_EXPERIMENT2_ARRAY_DESIGN";

    /**
     * If necessary, update the GENE2CS table.
     */
    @Secured({ "GROUP_AGENT" })
    int updateGene2CsEntries();

    /**
     * Update the GENE2CS table for a specific {@link ArrayDesign}.
     * @param arrayDesign the platform for which to update the GENE2CS entries
     * @param force       update the table even if no platforms has been modified since the last update
     */
    @Secured({ "GROUP_AGENT" })
    int updateGene2CsEntries( ArrayDesign arrayDesign, boolean force );

    /**
     * Update the GENE2CS table.
     * @param sinceLastUpdate if not null, only update entries whose corresponding {@link ArrayDesign} hsa been updated
     *                        after the given date
     * @param truncate        if true, the GENE2CS table will be truncated before the update
     * @param force           update the table even if no platforms has been modified since the last update
     */
    @Secured({ "GROUP_AGENT" })
    int updateGene2CsEntries( @Nullable Date sinceLastUpdate, boolean truncate, boolean force );

    /**
     * Update the {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} table.
     * @return the number of records that were created or updated
     */
    @Secured({ "GROUP_AGENT" })
    int updateExpressionExperiment2CharacteristicEntries( @Nullable Date sinceLastUpdate, boolean truncate );

    /**
     * Update a specific level of the {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC} table.
     * @param level the level to update which is either {@link ubic.gemma.model.expression.experiment.ExpressionExperiment},
     *              {@link ubic.gemma.model.expression.biomaterial.BioMaterial} or {@link ubic.gemma.model.expression.experiment.ExperimentalDesign}
     * @return the number of records that were created or updated
     */
    @Secured({ "GROUP_AGENT" })
    int updateExpressionExperiment2CharacteristicEntries( Class<?> level, @Nullable Date sinceLastUpdate, boolean truncate );

    /**
     * Update the {@code EXPRESSION_EXPERIMENT2_ARRAY_DESIGN} table.
     * @return the number of records that were created or updated
     */
    @Secured({ "GROUP_AGENT" })
    int updateExpressionExperiment2ArrayDesignEntries( @Nullable Date sinceLastUpdate );

    @Secured({ "GROUP_ADMIN" })
    void evictGene2CsQueryCache();

    @Secured({ "GROUP_ADMIN" })
    void evictEe2CQueryCache();

    @Secured({ "GROUP_ADMIN" })
    void evictEe2AdQueryCache();

    // for tests only, to keep from getting emails.
    @Secured({ "GROUP_ADMIN" })
    void disableEmail();
}