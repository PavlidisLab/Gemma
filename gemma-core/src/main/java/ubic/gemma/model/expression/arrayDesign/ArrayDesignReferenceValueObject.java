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
package ubic.gemma.model.expression.arrayDesign;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Compact platform identity — enough to name a platform and link to it.
 * <p>
 * Exists so {@link ArrayDesignValueObject} can point at another platform (its merge target, or the
 * platforms merged into it) without nesting a full platform VO, which would recurse and carry
 * curation details, taxon, external references and event triples that a cross-reference never
 * renders.
 *
 * @author paul
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class ArrayDesignReferenceValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Gemma-internal platform id, suitable for {@code GET /platforms/{id}}.
     */
    private Long id;

    /**
     * Short name, e.g. {@code GPL96}. This is what a page displays.
     */
    @Nullable
    private String shortName;

    @Override
    public String toString() {
        return "ArrayDesignReferenceValueObject [id=" + id
                + ( shortName != null ? ", shortName=" + shortName : "" ) + "]";
    }
}
