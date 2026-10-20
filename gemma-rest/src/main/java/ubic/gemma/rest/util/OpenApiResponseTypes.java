/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.rest.util;

import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

import java.util.List;

/**
 * Concrete response containers that exist only so the OpenAPI specification can name them.
 *
 * <h2>Why these exist</h2>
 *
 * An endpoint that supports both offset and cursor pagination returns {@link Object} and declares
 * its two shapes with {@code @Schema(oneOf = {...})}. An annotation can only name a {@link Class},
 * and Java erases type arguments, so naming a raw generic container there — {@code
 * PaginatedResponseDataObject.class} — hands swagger-core a container whose {@code data} is an
 * array of bare {@code object}. The payload type is gone from the spec.
 *
 * <p>That is worse than a generator error. A generated client deserializes the untyped payload to
 * raw camelCase dictionaries, and a wrapper that reads snake_case field names off them gets nulls
 * for every column rather than a failure. gemmapy carried a private table of the missing item types
 * for exactly this reason.
 *
 * <p>Binding the type argument in a named subclass restores it: swagger-core resolves the subclass
 * to a schema of the subclass's own simple name, with {@code data} typed as an array of the bound
 * value object.
 *
 * <h2>Using them</h2>
 *
 * Name the bound subclass in the {@code oneOf}, not the raw generic, and make sure it matches what
 * the method actually returns — the return type is {@code Object}, so nothing else checks:
 *
 * <pre>
 * &#64;ApiResponse(responseCode = "200",
 *         content = &#64;Content(schema = &#64;Schema(oneOf = {
 *                 PaginatedResponseDataObjectGeneValueObject.class,
 *                 CursorPaginatedResponseDataObjectGeneValueObject.class
 *         })))
 * public Object getGenes( ... )
 * </pre>
 *
 * {@code OpenApiTest.testPaginatedResponsesDeclareTheirPayloadType} fails the build when a response
 * schema reachable from a path has lost its {@code data} item type, so a new raw-generic
 * {@code oneOf} cannot reach the published spec.
 *
 * <p>These classes are never instantiated; the endpoints build the generic parent. The constructors
 * mirror the parent's so the subclass compiles and stays usable as a real return type.
 *
 * <p>{@code DatasetsWebService} declares its own bound containers rather than using this class,
 * because their generic parents ({@code FilteredAndInferredAndPaginatedResponseDataObject} and
 * friends, which carry inferred ontology terms) are themselves nested there.
 *
 * @author phase3
 * @see ResponseDataObject
 * @see PaginatedResponseDataObject
 * @see CursorPaginatedResponseDataObject
 * @see FilteredAndPaginatedResponseDataObject
 * @see FilteredAndCursorPaginatedResponseDataObject
 */
public final class OpenApiResponseTypes {

    private OpenApiResponseTypes() {
    }

    // --- ResponseDataObject<List<T>> — endpoints whose legacy mode is an unpaginated list.

    /** Legacy shape for {@code GET /datasets/{dataset}/tickets} and {@code GET /platforms/{platform}/tickets}. */
    public static class ResponseDataObjectListTicketValueObject extends ResponseDataObject<List<TicketValueObject>> {

        public ResponseDataObjectListTicketValueObject( List<TicketValueObject> payload ) {
            super( payload );
        }
    }

    /** Legacy shape for {@code GET /tickets/{id}/events}. */
    public static class ResponseDataObjectListTicketEventValueObject extends ResponseDataObject<List<TicketEventValueObject>> {

        public ResponseDataObjectListTicketEventValueObject( List<TicketEventValueObject> payload ) {
            super( payload );
        }
    }

    // --- PaginatedResponseDataObject<T> — legacy offset/limit, no echoed filter.

    /** Legacy shape for {@code GET /genes} and {@code GET /taxa/{taxon}/genes}. */
    public static class PaginatedResponseDataObjectGeneValueObject extends PaginatedResponseDataObject<GeneValueObject> {

        public PaginatedResponseDataObjectGeneValueObject( Slice<GeneValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Legacy shape for {@code GET /genes/{gene}/probes} and {@code GET /taxa/{taxon}/genes/{gene}/probes}. */
    public static class PaginatedResponseDataObjectCompositeSequenceValueObject extends PaginatedResponseDataObject<CompositeSequenceValueObject> {

        public PaginatedResponseDataObjectCompositeSequenceValueObject( Slice<CompositeSequenceValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Legacy shape for {@code GET /platforms/{platform}/datasets}. */
    public static class PaginatedResponseDataObjectExpressionExperimentValueObject extends PaginatedResponseDataObject<ExpressionExperimentValueObject> {

        public PaginatedResponseDataObjectExpressionExperimentValueObject( Slice<ExpressionExperimentValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Legacy shape for {@code GET /tickets}. */
    public static class PaginatedResponseDataObjectTicketValueObject extends PaginatedResponseDataObject<TicketValueObject> {

        public PaginatedResponseDataObjectTicketValueObject( Slice<TicketValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    // --- FilteredAndPaginatedResponseDataObject<T> — legacy offset/limit, echoes the ?filter= arg.

    /** Legacy shape for {@code GET /platforms}, {@code GET /platforms/{platform}} and {@code GET /platforms/blacklisted}. */
    public static class FilteredAndPaginatedResponseDataObjectArrayDesignValueObject extends FilteredAndPaginatedResponseDataObject<ArrayDesignValueObject> {

        public FilteredAndPaginatedResponseDataObjectArrayDesignValueObject( Slice<ArrayDesignValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Legacy shape for {@code GET /platforms/{platform}/elements} and {@code GET /platforms/{platform}/elements/{probes}}. */
    public static class FilteredAndPaginatedResponseDataObjectCompositeSequenceValueObject extends FilteredAndPaginatedResponseDataObject<CompositeSequenceValueObject> {

        public FilteredAndPaginatedResponseDataObjectCompositeSequenceValueObject( Slice<CompositeSequenceValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Legacy shape for {@code GET /platforms/{platform}/elements/{probe}/genes}. */
    public static class FilteredAndPaginatedResponseDataObjectGeneValueObject extends FilteredAndPaginatedResponseDataObject<GeneValueObject> {

        public FilteredAndPaginatedResponseDataObjectGeneValueObject( Slice<GeneValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Legacy shape for {@code GET /taxa/{taxon}/datasets}. */
    public static class FilteredAndPaginatedResponseDataObjectExpressionExperimentValueObject extends FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> {

        public FilteredAndPaginatedResponseDataObjectExpressionExperimentValueObject( Slice<ExpressionExperimentValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Legacy shape for {@code GET /resultSets}. */
    public static class FilteredAndPaginatedResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject extends FilteredAndPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> {

        public FilteredAndPaginatedResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject( Slice<DifferentialExpressionAnalysisResultSetValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    // --- CursorPaginatedResponseDataObject<T> — keyset pagination, no echoed filter.

    /** Cursor shape for {@code GET /genes} and {@code GET /taxa/{taxon}/genes}. */
    public static class CursorPaginatedResponseDataObjectGeneValueObject extends CursorPaginatedResponseDataObject<GeneValueObject> {

        public CursorPaginatedResponseDataObjectGeneValueObject( CursorPage<GeneValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Cursor shape for {@code GET /genes/{gene}/probes} and {@code GET /taxa/{taxon}/genes/{gene}/probes}. */
    public static class CursorPaginatedResponseDataObjectCompositeSequenceValueObject extends CursorPaginatedResponseDataObject<CompositeSequenceValueObject> {

        public CursorPaginatedResponseDataObjectCompositeSequenceValueObject( CursorPage<CompositeSequenceValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Cursor shape for {@code GET /platforms/{platform}/datasets}. */
    public static class CursorPaginatedResponseDataObjectExpressionExperimentValueObject extends CursorPaginatedResponseDataObject<ExpressionExperimentValueObject> {

        public CursorPaginatedResponseDataObjectExpressionExperimentValueObject( CursorPage<ExpressionExperimentValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Cursor shape for {@code GET /tickets}, {@code GET /datasets/{dataset}/tickets} and {@code GET /platforms/{platform}/tickets}. */
    public static class CursorPaginatedResponseDataObjectTicketValueObject extends CursorPaginatedResponseDataObject<TicketValueObject> {

        public CursorPaginatedResponseDataObjectTicketValueObject( CursorPage<TicketValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    /** Cursor shape for {@code GET /tickets/{id}/events}. */
    public static class CursorPaginatedResponseDataObjectTicketEventValueObject extends CursorPaginatedResponseDataObject<TicketEventValueObject> {

        public CursorPaginatedResponseDataObjectTicketEventValueObject( CursorPage<TicketEventValueObject> payload, String[] groupBy ) {
            super( payload, groupBy );
        }
    }

    // --- FilteredAndCursorPaginatedResponseDataObject<T> — keyset pagination, echoes the ?filter= arg.

    /** Cursor shape for {@code GET /platforms}, {@code GET /platforms/{platform}} and {@code GET /platforms/blacklisted}. */
    public static class FilteredAndCursorPaginatedResponseDataObjectArrayDesignValueObject extends FilteredAndCursorPaginatedResponseDataObject<ArrayDesignValueObject> {

        public FilteredAndCursorPaginatedResponseDataObjectArrayDesignValueObject( CursorPage<ArrayDesignValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Cursor shape for {@code GET /platforms/{platform}/elements} and {@code GET /platforms/{platform}/elements/{probes}}. */
    public static class FilteredAndCursorPaginatedResponseDataObjectCompositeSequenceValueObject extends FilteredAndCursorPaginatedResponseDataObject<CompositeSequenceValueObject> {

        public FilteredAndCursorPaginatedResponseDataObjectCompositeSequenceValueObject( CursorPage<CompositeSequenceValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Cursor shape for {@code GET /platforms/{platform}/elements/{probe}/genes}. */
    public static class FilteredAndCursorPaginatedResponseDataObjectGeneValueObject extends FilteredAndCursorPaginatedResponseDataObject<GeneValueObject> {

        public FilteredAndCursorPaginatedResponseDataObjectGeneValueObject( CursorPage<GeneValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Cursor shape for {@code GET /taxa/{taxon}/datasets}. */
    public static class FilteredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject extends FilteredAndCursorPaginatedResponseDataObject<ExpressionExperimentValueObject> {

        public FilteredAndCursorPaginatedResponseDataObjectExpressionExperimentValueObject( CursorPage<ExpressionExperimentValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }

    /** Cursor shape for {@code GET /resultSets}. */
    public static class FilteredAndCursorPaginatedResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject extends FilteredAndCursorPaginatedResponseDataObject<DifferentialExpressionAnalysisResultSetValueObject> {

        public FilteredAndCursorPaginatedResponseDataObjectDifferentialExpressionAnalysisResultSetValueObject( CursorPage<DifferentialExpressionAnalysisResultSetValueObject> payload, @Nullable Filters filters, @Nullable String[] groupBy ) {
            super( payload, filters, groupBy );
        }
    }
}
