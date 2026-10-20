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
package ubic.gemma.model.genome.gene;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Compact gene identity — just enough to label a row and link to the gene page.
 * <p>
 * Used where a listing needs to say <em>which</em> gene a row maps to without paying for a full
 * {@link GeneValueObject} per row: {@code GET /platforms/{platform}/elements?withGenes=true}
 * attaches one of these per mapped gene on every element of the page. A platform page showing a
 * 50-row element table needs the symbol to display and the id to link with; shipping the full
 * gene VO there would carry taxon, aliases, multifunctionality and GO counts that nothing on the
 * row renders.
 *
 * @author paul
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class GeneReferenceValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Gemma-internal gene id, suitable for {@code GET /genes/{id}}.
     */
    private Long id;

    /**
     * Official gene symbol, e.g. {@code BRCA1}. Null for the (rare) gene rows that carry none.
     */
    @Nullable
    private String officialSymbol;

    /**
     * NCBI gene id. Null for genes Gemma tracks without one.
     */
    @Nullable
    private Integer ncbiId;

    @Override
    public String toString() {
        return "GeneReferenceValueObject [id=" + id
                + ( officialSymbol != null ? ", officialSymbol=" + officialSymbol : "" )
                + ( ncbiId != null ? ", ncbiId=" + ncbiId : "" ) + "]";
    }
}
