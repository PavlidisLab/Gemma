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

   beforeEach( function( done ) {
      // have to call this first, before the form is built.
      spyOn( GenePickerController, "searchGenesAndGeneGroups" );

      var coexpressionSearchData = new Gemma.CoexpressionSearchData();

      searchForm = new Gemma.AnalysisResultsSearchForm( {
         width : Gemma.SEARCH_FORM_WIDTH,
         observableSearchResults : coexpressionSearchData
      } );

      searchForm.geneSearchAndPreview.geneCombo.setValue( '' );
      searchForm.geneSearchAndPreview.geneCombo.fireEvent( 'focus' );

      // wait for the call; there is a delay of 1200ms before we do an empty search
      setTimeout( function() {
         done();
      }, 1300 );

   } );

   it( 'check GenePickerController was called', function() {
      expect( GenePickerController.searchGenesAndGeneGroups ).toHaveBeenCalledWith( '', null, {
         callback : jasmine.any( Function ),
         errorHandler : jasmine.any( Function )
      } );
   } );

   afterEach( function() {
      searchForm.destroy();
   } );

} );
