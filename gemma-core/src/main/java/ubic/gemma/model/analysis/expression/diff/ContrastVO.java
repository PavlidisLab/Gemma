/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.diff;

import lombok.Data;
import ubic.gemma.model.common.ValueObject;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Helper object, not for general use.
 *
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Data
@ValueObject
public class ContrastVO implements Serializable {

    private Long id;
    @Nullable
    private Long factorValueId;
    @Nullable
    private Long secondFactorValueId;
    @Nullable
    private Double logFoldChange;
    @Nullable
    private Double pvalue;

    public ContrastVO() {

    }

    public ContrastVO( Long id, @Nullable Long factorValueId, @Nullable Long secondFactorValueId, @Nullable Double logFoldchange, @Nullable Double pvalue ) {
        super();
        this.id = id;
        this.factorValueId = factorValueId; // can be null if it's a continuous factor
        this.secondFactorValueId = secondFactorValueId;
        this.logFoldChange = logFoldchange;
        this.pvalue = pvalue;
    }

    public ContrastVO( ContrastResult c ) {
        this( c.getId(), c.getFactorValue() == null ? null : c.getFactorValue().getId(),
                c.getSecondFactorValue() != null ? c.getSecondFactorValue().getId() : null,
                c.getLogFoldChange(), c.getPvalue() );
    }
}