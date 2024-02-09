/*
 * The Gemma project
 *
 * Copyright (c) 2024 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;


@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Getter
@Setter
@ToString
public class ExperimentalFactorValueWebUIObject extends ExperimentalFactorValueObject {

    private String bioMaterialCharacteristicCategoryToUse;
    public ExperimentalFactorValueWebUIObject() {
        super();
    }

    public ExperimentalFactorValueWebUIObject( Long id ) {
        super( id );
    }
}
