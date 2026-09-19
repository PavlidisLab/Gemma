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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * A gene product that {@link GeneWriteService#upsert(ubic.gemma.model.genome.Gene, boolean, java.util.function.Consumer)}
 * removed, or would have removed, from a gene, or moved from one gene to another.
 * <p>
 * The association counts and platforms are those of the product's BLAT and annotation associations, counted before
 * anything was deleted. They are the probe-to-gene mappings that GENE2CS and the platform annotation files are built
 * from: a removal takes them away from the gene, a switch hands them to the receiving gene.
 *
 * @param kind                   what happened to the product
 * @param applied                false for a removal that was not made because removal of gene products was off
 * @param taxon                  common name of the gene's taxon
 * @param geneNcbiId             NCBI id of the gene losing the product; null for a product that had no gene
 * @param geneSymbol             official symbol of the gene losing the product
 * @param toGeneNcbiId           for a {@link Kind#SWITCH}, NCBI id of the gene receiving the product
 * @param toGeneSymbol           for a {@link Kind#SWITCH}, official symbol of the gene receiving the product
 * @param productId              database id of the gene product
 * @param productName            name of the gene product
 * @param productGi              NCBI GI of the gene product
 * @param blatAssociations       number of BLAT associations of the product
 * @param annotationAssociations number of annotation associations of the product
 * @param platforms              sorted, distinct short names of the platforms with an element whose sequence has one of
 *                               those associations
 */
public record GeneProductChange(
        Kind kind,
        boolean applied,
        @Nullable String taxon,
        @Nullable Integer geneNcbiId,
        @Nullable String geneSymbol,
        @Nullable Integer toGeneNcbiId,
        @Nullable String toGeneSymbol,
        @Nullable Long productId,
        @Nullable String productName,
        @Nullable String productGi,
        long blatAssociations,
        long annotationAssociations,
        List<String> platforms ) implements Serializable {

    public enum Kind {
        /**
         * The product is no longer listed by NCBI for the gene, or is an outdated duplicate of one that is.
         */
        REMOVE,
        /**
         * The product was moved from one gene to another.
         */
        SWITCH
    }

    public GeneProductChange {
        platforms = List.copyOf( platforms );
    }

    /**
     * A short description of the product and its gene, for logs and batch summaries.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append( kind ).append( " " ).append( productName ).append( " (id=" ).append( productId ).append( ", GI=" )
                .append( productGi ).append( ")" );
        if ( geneNcbiId != null ) {
            sb.append( " of " ).append( geneSymbol ).append( " [NCBI " ).append( geneNcbiId ).append( "]" );
        } else {
            sb.append( " of no gene" );
        }
        if ( kind == Kind.SWITCH ) {
            sb.append( " to " ).append( toGeneSymbol ).append( " [NCBI " ).append( toGeneNcbiId ).append( "]" );
        }
        return sb.toString();
    }
}
