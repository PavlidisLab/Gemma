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
package ubic.gemma.persistence.service.common.description;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.persistence.service.BaseImmutableService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

/**
 * @author kelsey
 */
public interface DatabaseEntryService extends BaseImmutableService<DatabaseEntry>, FilteringVoEnabledService<DatabaseEntry, DatabaseEntryValueObject> {

    /**
     * Find the latest (as per its version or ID) database entry by accession.
     */
    DatabaseEntry findLatestByAccession( String accession );

    @Override
    DatabaseEntry findOrCreate( DatabaseEntry entity );
}
