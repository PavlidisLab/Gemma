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
package ubic.gemma.model.expression.experiment;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Compact dataset identity — enough to name a dataset and link to it.
 * <p>
 * Exists so an {@link ExpressionExperimentValueObject} can point at another dataset — the other parts
 * of a study Gemma split — without nesting a full dataset VO, which would carry curation details,
 * platforms, taxon and event triples that a cross-reference never renders, and would cost a
 * {@code loadValueObjectsByIds} per sibling. Mirrors
 * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignReferenceValueObject}.
 *
 * @author paul
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class ExpressionExperimentReferenceValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Gemma-internal dataset id, suitable for {@code GET /datasets/{id}}.
     */
    private Long id;

    /**
     * Short name, e.g. {@code GSE1234} or {@code Rexach-2024.3}. This is what a page displays.
     */
    @Nullable
    private String shortName;

    /**
     * Full title. A split part's title is of the form {@code Split part 3 of: … [organism part = …]},
     * which is the only place the distinguishing factor value appears, so a reference carrying only
     * the short name cannot tell one sibling from another.
     */
    @Nullable
    private String name;
}
