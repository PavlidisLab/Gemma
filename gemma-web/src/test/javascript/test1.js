describe( "Basic Assumptions", function() {

   it( "has ExtJS loaded", function() {
      expect( Ext ).toBeDefined();
      expect( Ext.version ).toBeTruthy();
      expect( Ext.versionDetail.major ).toEqual( 3 );
   } );

} );

describe( "CoexVOUtil", function() {
   it( "getEntityIds", function() {

      expect( Gemma.CoexVOUtil.getEntityIds( [ {
         id : 1
      }, {
         id : 2
      }, {
         id : 3
      } ] ) ).toEqual( [ 1, 2, 3 ] );

   } );

   it( "getAllGeneIds", function() {

      expect( Gemma.CoexVOUtil.getAllGeneIds( [ {
         id : 1,
         queryGene : {
            id : 10
         },
         foundGene : {
            id : 11
         }
      }, {
         id : 2,
         queryGene : {
            id : 10
         },
         foundGene : {
            id : 12
         }
      }, {
         id : 3,
         queryGene : {
            id : 14
         },
         foundGene : {
            id : 15
         }
      } ] ).length ).toEqual( 5 );

   } );
} );

describe( "search For Genes with empty query", function() {
   var searchForm = {};

   jasmine.getJSONFixtures().fixturesPath = 'spec/fixtures/json/';

   beforeEach( function( done ) {

      this.mock = new FakeDWR( GenePickerController );

      // have to call this first, before the form is built.
      this.data = getJSONFixture( 'GenePickerControllerResultOfEmptyQuery.json' );

      this.mock.mock_method( "searchGenesAndGeneGroups", this.data );

      spyOn( GenePickerController, "searchGenesAndGeneGroups" ).and.callThrough();

      /*
       * Set up a search form.
       */
      var coexpressionSearchData = new Gemma.CoexpressionSearchData();
      searchForm = new Gemma.AnalysisResultsSearchForm( {
         width : Gemma.SEARCH_FORM_WIDTH,
         observableSearchResults : coexpressionSearchData
      } );

      this.se = spyOnEventExt( searchForm.geneSearchAndPreview.geneCombo.store, 'load' );
      spyOn( searchForm.geneSearchAndPreview.geneCombo.store.proxy, 'request' ).and.callThrough();

      /*
       * Give the genecombo focus with no input.
       */
      searchForm.geneSearchAndPreview.geneCombo.setValue( '' );
      searchForm.geneSearchAndPreview.geneCombo.fireEvent( 'focus' );
      // searchForm.geneSearchAndPreview.geneCombo.focus(); // doesn't trigger the event.

      // wait for the call to be triggered; there is a delay of 1200ms before we do an empty search. Otherwise would not
      // need this much time.
      setTimeout( function() {
         done();

      }, 1300 );

   } );

   it( 'checks GenePickerController.searchGenesAndGeneGroups was called and records loaded', function() {
      expect( GenePickerController.searchGenesAndGeneGroups ).toHaveBeenCalledWith( '', null, {
         callback : jasmine.any( Function ),
         errorHandler : jasmine.any( Function )
      } );

      expect( searchForm.geneSearchAndPreview.geneCombo.store.proxy.request ).toHaveBeenCalled();

      expect( this.se ).toHaveBeenTriggered();

      expect( searchForm.geneSearchAndPreview.geneCombo.store.getCount() ).toEqual( 4 );

   } );

   afterEach( function() {
      searchForm.destroy();
      this.mock.unmock_method( "searchGenesAndGeneGroups" );
   } );

} );
