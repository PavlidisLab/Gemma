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

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;

/**
 *
 * @author paul
 */
@Getter
@Setter
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
}
