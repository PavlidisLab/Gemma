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
package ubic.gemma.persistence.service.common.measurement;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.measurement.Unit;

/**
 * @author paul
 */
public interface UnitService {

    @Secured({ "GROUP_USER" })
    java.util.Collection<Unit> create( java.util.Collection<Unit> entities );

    @Secured({ "GROUP_USER" })
    ubic.gemma.model.common.measurement.Unit create( ubic.gemma.model.common.measurement.Unit unit );

    Unit find( Unit unit );

    @Secured({ "GROUP_USER" })
    Unit findOrCreate( Unit unit );

    ubic.gemma.model.common.measurement.Unit load( java.lang.Long id );

    java.util.Collection<Unit> loadAll();

    @Secured({ "GROUP_ADMIN" })
    void remove( java.lang.Long id );

    @Secured({ "GROUP_ADMIN" })
    void remove( java.util.Collection<Unit> entities );

    @Secured({ "GROUP_USER" })
    void remove( ubic.gemma.model.common.measurement.Unit unit );

    @Secured({ "GROUP_USER" })
    void update( java.util.Collection<Unit> entities );

    @Secured({ "GROUP_USER" })
    void update( ubic.gemma.model.common.measurement.Unit unit );

}
