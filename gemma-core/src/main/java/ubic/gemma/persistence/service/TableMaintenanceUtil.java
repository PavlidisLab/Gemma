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
     * If necessary, update the GENE2CS table.
     */
    @Secured({ "GROUP_AGENT" })
    void updateGene2CsEntries();

    @Secured({ "GROUP_AGENT" })
    void updateExpressionExperiment2CharacteristicEntries();

    @Secured({ "GROUP_ADMIN" })
    void setGene2CsInfoPath( Path gene2CsInfoPath );

    // for tests only, to keep from getting emails.
    @Secured({ "GROUP_ADMIN" })
    void disableEmail();
}