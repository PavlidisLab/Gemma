/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

import ubic.gemma.core.lang.Nullable;

import java.io.Serializable;
import java.util.Date;

/**
 * Used to store information about what happened when the GENE2CS table was updated.
 *
 * @author paul
 * @see TableMaintenanceUtil
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class Gene2CsStatus implements Serializable {

    private static final long serialVersionUID = 1956861185764899312L;

    private Date lastUpdate;

    @Nullable
    private Exception error;

    private String annotation;

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation( String annotation ) {
        this.annotation = annotation;
    }

    @Nullable
    public Exception getError() {
        return error;
    }

    public void setError( @Nullable Exception error ) {
        this.error = error;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate( Date lastUpdate ) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString() {
        return lastUpdate.toString() + " " + ( error == null );
    }

}