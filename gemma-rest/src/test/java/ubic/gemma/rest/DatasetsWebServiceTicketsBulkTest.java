/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketSearchHitValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.util.ResponseDataObject;

import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatasetsWebService#getDatasetTicketsBulk} &mdash; the bulk read behind the
 * experiment list's "on a ticket" glyph. Pure Mockito: the grouping, the presence contract and the
 * open-state scope are pinned against real MySQL in {@code TicketPersistenceIT}; what is tested here
 * is what only the route can get wrong &mdash; the order of the ACL filter against the ticket
 * lookup, and the request guards.
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
public class DatasetsWebServiceTicketsBulkTest {

    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private TicketsWebService ticketsWebService;

    @InjectMocks
    private DatasetsWebService webService;

    private static DatasetsWebService.DatasetTicketsBulkRequest request( Long... ids ) {
        DatasetsWebService.DatasetTicketsBulkRequest body = new DatasetsWebService.DatasetTicketsBulkRequest();
        body.setDatasetIds( new ArrayList<>( Arrays.asList( ids ) ) );
        return body;
    }

    private static ExpressionExperiment ee( Long id ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( id );
        return ee;
    }

    private void visibleDatasets( ExpressionExperiment... visible ) {
        // Typed local: getFilter is overloaded on (…, T) and (…, Collection<T>), and a bare
        // anyCollection() matches both.
        Collection<Long> anyIds = anyCollection();
        lenient().when( expressionExperimentService.getFilter( anyString(), eq( Long.class ),
                        any( Filter.Operator.class ), anyIds ) )
                .thenReturn( mockFilter() );
        // isNull(), not any(Sort.class): the route loads unsorted, and any(Class) does not match null.
        when( expressionExperimentService.load( any( Filters.class ), ArgumentMatchers.<Sort>isNull() ) )
                .thenReturn( Arrays.asList( visible ) );
    }

    private Filter mockFilter() {
        return org.mockito.Mockito.mock( Filter.class );
    }

    private static TicketSearchHitValueObject hit( Long id, String title ) {
        return new TicketSearchHitValueObject( id, title, TicketState.OPEN, TicketType.CURATION, 7L, new Date() );
    }

    /**
     * 🛑 The ticket table carries no ACL of its own, so the readability filter has to run BEFORE the
     * lookup, not after it. Asking about a dataset the caller cannot see must not reach the ticket
     * query at all &mdash; otherwise "is this under review" is answerable about a private dataset.
     */
    @Test
    public void unreadableDatasetsNeverReachTheTicketLookup() {
        visibleDatasets( ee( 1L ) );
        when( ticketsWebService.openTicketSummariesForExpressionExperiments( anyCollection() ) )
                .thenReturn( Collections.singletonMap( 1L, Collections.singletonList( hit( 99L, "review" ) ) ) );

        ResponseDataObject<Map<Long, List<TicketSearchHitValueObject>>> res =
                webService.getDatasetTicketsBulk( request( 1L, 2L ) );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> asked = ArgumentCaptor.forClass( Collection.class );
        verify( ticketsWebService ).openTicketSummariesForExpressionExperiments( asked.capture() );
        assertThat( asked.getValue() ).containsExactly( 1L );
        assertThat( res.getData() ).containsOnlyKeys( 1L );
    }

    /** Nothing readable is an empty map, and no reason to run the ticket query at all. */
    @Test
    public void noReadableDatasetsShortCircuits() {
        visibleDatasets();

        ResponseDataObject<Map<Long, List<TicketSearchHitValueObject>>> res =
                webService.getDatasetTicketsBulk( request( 1L, 2L ) );

        assertThat( res.getData() ).isEmpty();
        verify( ticketsWebService, never() ).openTicketSummariesForExpressionExperiments( anyCollection() );
    }

    /**
     * A repeated id must not become a repeated key or a doubled ticket list; the other two bulk
     * dataset reads deduplicate the same way.
     */
    @Test
    public void repeatedIdsAreDeduplicated() {
        visibleDatasets( ee( 1L ) );
        when( ticketsWebService.openTicketSummariesForExpressionExperiments( anyCollection() ) )
                .thenReturn( Collections.emptyMap() );

        webService.getDatasetTicketsBulk( request( 1L, 1L, 1L ) );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> asked = ArgumentCaptor.forClass( Collection.class );
        verify( ticketsWebService ).openTicketSummariesForExpressionExperiments( asked.capture() );
        assertThat( asked.getValue() ).containsExactly( 1L );
    }

    @Test
    public void missingOrEmptyBodyIsRefused() {
        assertThatThrownBy( () -> webService.getDatasetTicketsBulk( null ) )
                .isInstanceOf( BadRequestException.class );
        assertThatThrownBy( () -> webService.getDatasetTicketsBulk( new DatasetsWebService.DatasetTicketsBulkRequest() ) )
                .isInstanceOf( BadRequestException.class );
        assertThatThrownBy( () -> webService.getDatasetTicketsBulk( request() ) )
                .isInstanceOf( BadRequestException.class );
        verify( ticketsWebService, never() ).openTicketSummariesForExpressionExperiments( anyCollection() );
    }

    /** Over the cap is a 400 that names the cap, not a request that quietly truncates the page. */
    @Test
    public void overTheCapIsRefusedRatherThanTruncated() {
        Long[] tooMany = new Long[1001];
        for ( int i = 0; i < tooMany.length; i++ ) {
            tooMany[i] = ( long ) i;
        }

        assertThatThrownBy( () -> webService.getDatasetTicketsBulk( request( tooMany ) ) )
                .isInstanceOf( BadRequestException.class )
                .hasMessageContaining( "1000" );
        verify( ticketsWebService, never() ).openTicketSummariesForExpressionExperiments( anyCollection() );
    }
}
