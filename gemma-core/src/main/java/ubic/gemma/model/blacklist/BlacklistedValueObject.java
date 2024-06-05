/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.model.blacklist;

import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;

/**
 *
 * @author paul
 */
public class BlacklistedValueObject extends IdentifiableValueObject<BlacklistedEntity> {
    private static final long serialVersionUID = -4817418347388923905L;

    public static BlacklistedValueObject fromEntity( BlacklistedEntity e ) {
        BlacklistedValueObject result = new BlacklistedValueObject( e.getId() );
        if ( e.getExternalAccession() != null ) {
            result.setAccession( e.getExternalAccession().getAccession() );
            result.setExternalDatabase( new ExternalDatabaseValueObject( e.getExternalAccession().getExternalDatabase() ) );
        }
        result.setReason( e.getReason() );
        result.setShortName( e.getShortName() );
        result.setName( e.getName() );
        result.setType( e.getClass().getSimpleName() );
        return result;
    }

    private String accession;
    private ExternalDatabaseValueObject externalDatabase;
    private String name;
    private String reason;
    private String shortName;
    private String type;

    /**
     * Required when using the class as a spring bean.
     */
    public BlacklistedValueObject() {
        super();
    }

    @SuppressWarnings("unused")
    private BlacklistedValueObject( Long id ) {
        super( id );
    }

    public String getAccession() {
        return accession;
    }

    public ExternalDatabaseValueObject getExternalDatabase() {
        return externalDatabase;
    }

    public String getName() {
        return name;
    }

    public String getReason() {
        return reason;
    }

    public String getShortName() {
        return shortName;
    }

    public String getType() {
        return type;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setExternalDatabase( ExternalDatabaseValueObject externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setReason( String reason ) {
        this.reason = reason;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setType( String type ) {
        this.type = type;
    }
}
