/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Data vector associated to a {@link CompositeSequence}.
 */
@Getter
@Setter
public abstract class DesignElementDataVector extends DataVector {

    private CompositeSequence designElement;

    /**
     * The original design element that was used to create this vector.
     * <p>
     * This is generally null, but if the data was imported and a mapping was done between some external data source and
     * one of Gemma's platform, this will contain the original ID.
     */
    @Nullable
    public abstract String getOriginalDesignElement();

    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), designElement );
    }
}