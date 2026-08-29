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
 */
package ubic.gemma.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetValueObjectHelper;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito tests for {@link ExperimentSetsWebService}: the resource → service wiring, and the
 * guards that decide what reaches the service at all.
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
public class ExperimentSetsWebServiceTest {

    @Mock
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Mock
    private ExpressionExperimentSetValueObjectHelper expressionExperimentSetValueObjectHelper;

    @InjectMocks
    private ExperimentSetsWebService webService;

    private ExpressionExperimentSetValueObject set( long id, String name ) {
        ExpressionExperimentSetValueObject vo = new ExpressionExperimentSetValueObject();
        vo.setId( id );
        vo.setName( name );
        return vo;
    }

    @Test
    public void testListIsSortedAndPaged() {
        when( expressionExperimentSetService.loadAllExperimentSetValueObjects( false ) )
                .thenReturn( Arrays.asList( set( 2L, "zebra" ), set( 1L, "aardvark" ) ) );

        assertThat( webService.getExperimentSets( false, null, false, OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) ).getData() )
                .extracting( ExpressionExperimentSetValueObject::getName )
                .containsExactly( "aardvark", "zebra" );
    }

    @Test
    public void testMineAsksForMineRatherThanFilteringAll() {
        when( expressionExperimentSetService.loadMySetValueObjects( false ) )
                .thenReturn( Collections.singletonList( set( 1L, "mine" ) ) );

        webService.getExperimentSets( true, null, false, OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );

        verify( expressionExperimentSetService ).loadMySetValueObjects( false );
        verify( expressionExperimentSetService, never() ).loadAllExperimentSetValueObjects( anyBoolean() );
    }

    /**
     * 🛑 Members are off by default on the list. A set can hold thousands of datasets and the ids
     * are the expensive part of the query; a list endpoint that loaded them for every set would
     * make browsing sets cost the corpus.
     */
    @Test
    public void testTheListDoesNotLoadMembersUnlessAsked() {
        when( expressionExperimentSetService.loadAllExperimentSetValueObjects( anyBoolean() ) )
                .thenReturn( Collections.singletonList( set( 1L, "a" ) ) );

        webService.getExperimentSets( false, null, false, OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );
        verify( expressionExperimentSetService ).loadAllExperimentSetValueObjects( false );

        webService.getExperimentSets( false, null, true, OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );
        verify( expressionExperimentSetService ).loadAllExperimentSetValueObjects( true );
    }

    @Test
    public void testAnUnknownSetIs404() {
        when( expressionExperimentSetService.loadValueObjectById( eq( 404L ), anyBoolean() ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.getExperimentSetDatasets( 404L ) )
                .isInstanceOf( NotFoundException.class );
    }

    /**
     * A create is a create: the name is the only thing the caller must supply, and a blank one is
     * refused here rather than reaching the service, which would throw IllegalArgumentException and
     * surface as a less specific error.
     */
    @Test
    public void testCreateRequiresAName() {
        ExperimentSetsWebService.ExperimentSetRequest req = new ExperimentSetsWebService.ExperimentSetRequest();
        req.description = "no name";

        assertThatThrownBy( () -> webService.createExperimentSet( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( expressionExperimentSetValueObjectHelper, never() ).create( any() );
    }

    /**
     * 🛑 The taxon is NOT sent unless the caller declared one. Sending a derived or defaulted taxon
     * would re-impose the constraint the service was just relaxed to make optional, and a mixed
     * cohort — 179 human, 254 mouse, 16 rat in the gold reference set — would be refused again.
     */
    @Test
    public void testCreatePassesTheMembersAndLeavesTheTaxonAlone() {
        ExpressionExperimentSet created = ExpressionExperimentSet.Factory.newInstance();
        created.setId( 7L );
        when( expressionExperimentSetValueObjectHelper.create( any() ) ).thenReturn( created );
        when( expressionExperimentSetService.loadValueObjectById( 7L, true ) ).thenReturn( set( 7L, "Reference 500" ) );

        ExperimentSetsWebService.ExperimentSetRequest req = new ExperimentSetsWebService.ExperimentSetRequest();
        req.name = "  Reference 500  ";
        req.datasetIds = Arrays.asList( 11L, 12L, 13L );

        Response r = webService.createExperimentSet( req );

        assertThat( r.getStatus() ).isEqualTo( 201 );
        ArgumentCaptor<ExpressionExperimentSetValueObject> sent = ArgumentCaptor.forClass( ExpressionExperimentSetValueObject.class );
        verify( expressionExperimentSetValueObjectHelper ).create( sent.capture() );
        assertThat( sent.getValue().getName() ).as( "trimmed" ).isEqualTo( "Reference 500" );
        assertThat( sent.getValue().getTaxonId() ).as( "no taxon unless the caller declared one" ).isNull();
        assertThat( sent.getValue().getExpressionExperimentIds() ).containsExactlyInAnyOrder( 11L, 12L, 13L );
        assertThat( sent.getValue().getIsPublic() ).as( "private unless asked" ).isFalse();
    }

    /**
     * Replacing membership is a replace. An empty list empties the set, and it must reach the
     * service rather than being read as "nothing to do" — the difference is a set that still holds
     * its old members.
     */
    @Test
    public void testEmptyMembershipEmptiesTheSet() {
        when( expressionExperimentSetService.loadValueObjectById( eq( 3L ), anyBoolean() ) ).thenReturn( set( 3L, "s" ) );
        ExperimentSetsWebService.ExperimentSetMembersRequest req = new ExperimentSetsWebService.ExperimentSetMembersRequest();
        req.datasetIds = Collections.emptyList();

        webService.updateExperimentSetMembers( 3L, req );

        verify( expressionExperimentSetValueObjectHelper ).updateMembers( 3L, Collections.emptyList() );
    }

    /** A missing datasetIds is not the same as an empty one, and must not silently empty the set. */
    @Test
    public void testMissingMembershipIsRefused() {
        when( expressionExperimentSetService.loadValueObjectById( eq( 3L ), anyBoolean() ) ).thenReturn( set( 3L, "s" ) );

        assertThatThrownBy( () -> webService.updateExperimentSetMembers( 3L,
                new ExperimentSetsWebService.ExperimentSetMembersRequest() ) )
                .isInstanceOf( BadRequestException.class );
        verify( expressionExperimentSetValueObjectHelper, never() ).updateMembers( any(), any() );
    }
}
