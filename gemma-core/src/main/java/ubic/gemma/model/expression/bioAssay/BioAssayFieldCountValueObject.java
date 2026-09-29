/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.model.expression.bioAssay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * One value a dataset's samples carry for a single {@link BioAssay} field, and how many carry it.
 * <p>
 * The unit is a dataset: a list of these summarises one field across every assay the dataset has, so the
 * counts sum to the dataset's {@code numberOfBioAssays}. That is what makes the list readable as a
 * distribution rather than a sample of one — a single-entry list says the field is constant, and a
 * two-entry list says exactly how the dataset splits.
 *
 * @author paul
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject#getLibraryStrategies()
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "value" })
public class BioAssayFieldCountValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The value itself, spelled exactly as the per-assay field spells it in
     * {@link BioAssayValueObject} — so a client can compare the two without a mapping table.
     * <p>
     * {@code null} is a value here, not an absence: assays that carry nothing for the field are counted
     * under a null-valued entry rather than dropped. Dropping them would make the counts stop summing to
     * the dataset's assay count, and would render a microarray dataset's {@code librarySelection} — null on
     * every assay, because the technology has no selection step — as an empty list, which reads as "no
     * assays" instead of "no value".
     */
    @Nullable
    @Schema(description = "The value, spelled as the per-sample field spells it. Null means the samples counted here carry no value for this field.")
    private String value;

    /**
     * How many of the dataset's assays carry {@link #value}. Never zero.
     */
    @Schema(description = "How many of the dataset's samples carry this value.")
    private int numberOfBioAssays;

    @Override
    public String toString() {
        return "BioAssayFieldCountValueObject [value=" + value + ", numberOfBioAssays=" + numberOfBioAssays + "]";
    }
}
