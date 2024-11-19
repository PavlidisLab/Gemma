/**
 * A gene search combo and a geneSetPreview.
 * 
 * @author thea
 *
 */
Ext.namespace( 'Gemma' );

Gemma.GeneSearchAndPreview = Ext
   .extend(
      Ext.Panel,
      {

         /**
          * 
          * @param gsvo
          *           {GeneSetValueObject}
          */
         setSelectedGeneSetValueObject : function( gsvo ) {
            if ( gsvo.resultValueObject != null ) {
               this.selectedGeneSetValueObject = gsvo.resultValueObject;

            } else {
               this.selectedGeneSetValueObject = gsvo;
            }
            // This is goofy, but has to be done to get the preview in the right state. Triggers 'updateTitle()'.
            this.preview.setSelectedSetValueObject( this.selectedGeneSetValueObject );

         },

         /**
          * @memberOf Gemma.GeneSearchAndPreview
          */
         getSelectedGeneSetValueObject : function() {
            return this.selectedGeneSetValueObject;
         },

         reset : function() {
            if ( this.loadMask )
               this.loadMask.hide();
            this.selectedGeneSetValueObject = null;
            this.preview.reset();
            this.geneCombo.reset();
            this.geneCombo.enable().show();
            this.helpBtn.show();
            this.symbolListButton.show();
            this.preview.hide();
            this.doLayout();
         },

         resetGenePreview : function() {
            this.preview.reset();
         },

         showGenePreview : function() {
            this.preview.showPreview();
         },

         collapsePreview : function() {
            this.preview.collapsePreview();
         },

         maskGenePreview : function() {
            if ( !this.loadMask && this.getEl() ) {
               this.loadMask = new Ext.LoadMask( this.getEl(), {
                  msg : Gemma.StatusText.Loading.genes
               } );
            }
            this.loadMask.show();

         },

         showTaxonCombo : true,

         /**
          * called when a record is selected from geneAndGeneGroupCombo
          * 
          * @param {Object}
          *           record
          * @param {Object}
          *           query
          */
         loadGeneOrGroup : function( record, query ) {

            var vo = record.get( 'resultValueObject' );
            var id = vo.id;
            var size = record.get( "size" );
            var name = record.get( "name" );
            var taxonId = vo.taxonId;
            Gemma.EVENTBUS.fireEvent( 'taxonchanged', taxonId );

            this.preview.setTaxonId( taxonId );

            if ( vo instanceof GeneSetValueObject ) {
               this.setSelectedGeneSetValueObject( vo );
            } else if ( vo instanceof GeneValueObject ) {
               console.log( "Got a single gene, converting to a session-bound geneset" );
               // we should deal with sets, not
               // single gene objects, we end up having two cases too often. Convert gene directly to set and stick with
               // it.
               var newset = this.makeSessionBoundGeneSet( [ id ], taxonId, "From query",
                  'Group made from genes entered' );
               this.setSelectedGeneSetValueObject( newset );
            } else {
               throw "Don't know what kind of object was received";
            }

            // for bookmarking diff ex viz
            if ( id === null ) {
               var queryToGetSelected = "";
               if ( vo instanceof GOGroupValueObject && vo.name.match( /^GO_\d+/ ) ) {
                  queryToGetSelected = "taxon:" + taxonId + ";GO:" + name;
               } else if ( vo instanceof FreeTextGeneResultsValueObject && vo.name.indexOf( query ) != -1 ) {
                  queryToGetSelected = "taxon:" + taxonId + ";query:" + query;
               }
               this.queryUsedToGetSessionGroup = queryToGetSelected;
            }

            this.preview.loadGenePreviewFromGeneSet( this.getSelectedGeneSetValueObject() );

         },

         /**
          * 
          * @param {Array}
          *           genes of gene ids
          * @param taxonId
          * @param name
          * @param description
          * @returns {SessionBoundGeneSetValueObject}
          */
         makeSessionBoundGeneSet : function( geneIds, taxonId, name, description ) {
            // debugger;
            var newGeneSet = new SessionBoundGeneSetValueObject();

            newGeneSet.modified = false;
            newGeneSet.geneIds = geneIds;
            newGeneSet.taxonId = taxonId;
            newGeneSet.name = name;// 'From Symbol List' etc.;
            newGeneSet.description = description, // e.g. 'Group made from gene symbols entered.';
            newGeneSet.size = geneIds.length;
            newGeneSet.id = null;
            return newGeneSet;
         },

         /**
          * Used when user pastes in a list of genes; or when reaching from a URL
          * 
          * @private
          * @param queryToGenes
          *           hash of queries to genes
          * @param taxonId
          * @return SessionBoundGeneSetValueObject
          */
         processGeneMultiSearch : function( queryToGenes, taxonId ) {

            // debugger;
            var geneIds = [];
            var queriesWithNoResults = [];
            var allGenes = [];

            for ( var query in queryToGenes) {
               var gene = queryToGenes[query];

               if ( gene == null ) {
                  queriesWithNoResults.push( query );
                  continue;
               }
               geneIds.push( gene.id );
               allGenes.push( gene );
            }

            this.searchForm.geneIds = geneIds; // why?

            var geneset = this.makeSessionBoundGeneSet( geneIds, taxonId, 'From user query',
               'Group made from user query entered.' );

            // if some genes weren't found
            // prepare a msg for the user

            var msgMany = "";
            var msgNone = "";

            if ( queriesWithNoResults.length > 0 ) {
               msgNone = queriesWithNoResults.length + ((queriesWithNoResults.length === 1) ? " query" : " queries")
                  + " did not match any genes in Gemma:<br><br>";
               // for each query
               var query = '';
               for (var i = 0; i < queriesWithNoResults.length; i++) {
                  query = queriesWithNoResults[i];
                  msgNone += query + "<br>";
               }
            }

            this.resetGenePreview();

            this.preview.setTaxonId( taxonId );

            var message = queriesWithNoResults.length > 0 ? String.format(
               Gemma.HelpText.WidgetDefaults.GeneSearchAndPreview.inexactFromList, msgMany, msgNone ) : '';

            this.setSelectedGeneSetValueObject( geneset );
            this.preview.setSelectedSetValueObject( geneset ); // ??
            this.preview.loadGenePreviewFromGeneSet( geneset, message );

            this.preview.show();

            return geneset;
         },

         /**
          * Given text, search Gemma for matching genes. Used to 'bulk load' genes from the GUI (via GeneImportPanel).
          * After loading this will trigger processGeneMultiSearch.
          * 
          * @param {}
          *           e - text supplied
          * @param {Number}
          *           taxonId
          */
         getGenesFromList : function( e, taxonId ) {

            var taxonName;
            if ( !taxonId && this.searchForm.getTaxonId() ) {
               taxonId = this.searchForm.getTaxonId();
               taxonName = this.searchForm.getTaxonName();
            } else {
               taxonId = this.symbolList._taxonCombo.getTaxon().id;
               taxonName = this.symbolList._taxonCombo.getTaxon().data.commonName;
            }

            if ( isNaN( taxonId ) ) {
               Ext.Msg.alert( Gemma.HelpText.CommonErrors.MissingInput.title,
                  Gemma.HelpText.CommonErrors.MissingInput.taxon );
               return;
            }

            this.maskGenePreview();

            this.searchForm.taxonChanged( taxonId, taxonName );

            var queries = e.geneNames.split( '\n' );

            GenePickerController.searchMultipleGenesGetMap( queries, taxonId, {

               callback : function( queryToGenes ) {
                  this.processGeneMultiSearch( queryToGenes, taxonId );
                  this.changeDisplayAfterSelection();

               }.createDelegate( this ),

               errorHandler : function( e ) {
                  // this.getEl().unmask();
                  Ext.Msg.alert( 'There was an error', e );
               }
            } );

            this.fireEvent( 'madeFirstSelection' ); // see MetaAnalysisSelectExperimentPanel ? why outside the
            // callback?
            this.fireEvent( 'select' ); // who listens?
         },

         /**
          * 
          */
         getGenesFromUrl : function() {
            var urlparams = Ext.urlDecode( location.search.substring( 1 ) );

            if ( isNaN( urlparams.taxon ) ) {
               Ext.Msg.alert( Gemma.HelpText.CommonErrors.MissingInput.title,
                  Gemma.HelpText.CommonErrors.MissingInput.taxon );
               return;
            }
            this.maskGenePreview();

            var queries = urlparams.geneList.split( "," );
            this.searchForm.taxonChanged( urlparams.taxon, urlparams.taxonName);

            GenePickerController.searchMultipleGenesGetMap( queries, urlparams.taxon, {
               callback : function( queryToGenes ) {
                  this.processGeneMultiSearch( queryToGenes, urlparams.taxon );
                  this.changeDisplayAfterSelection();

                  this.fireEvent( 'geneListUrlSelectionComplete' );
               }.createDelegate( this ),

               errorHandler : function( e ) {
                  // this.getEl().unmask();
                  Ext.Msg.alert( 'There was an error', e );
               }
            } );

            this.fireEvent( 'madeFirstSelection' ); // // see MetaAnalysisSelectExperimentPanel why outside the
            // callback?
            this.fireEvent( 'select' ); // who listens?
         },

         /**
          * Allows updates to the query genes.
          * 
          * @param {Array}
          *           geneIds
          * 
          */
         getGenes : function( geneIds ) {

            this.searchForm.geneIds = geneIds;
            var taxonId = this.searchForm.getTaxonId();
            this.preview.setTaxonId( taxonId );

            this.resetGenePreview();
            this.changeDisplayAfterSelection();

            var geneset = this.makeSessionBoundGeneSet( geneIds, taxonId, 'From query IDs', 'Group made from gene IDs' );

            this.preview.loadGenePreviewFromGeneSet( geneset, "Updated genes" );

            this.setSelectedGeneSetValueObject( geneset );

            this.preview.show();
            this.fireEvent( 'madeFirstSelection' ); // see MetaAnalysisSelectExperimentPanel
            this.fireEvent( 'select' );
            return geneset;

         },

         /**
          * @private
          */
         changeDisplayAfterSelection : function() {
            this.geneCombo.disable().hide();
            this.helpBtn.hide();
            this.symbolListButton.hide();
            this.doLayout();
            if ( this.loadMask )
               this.loadMask.hide();
         },

         /**
          * 
          * @returns {Gemma.GeneImportPanel}
          */
         createGeneImportPanel : function() {
            return new Gemma.GeneImportPanel( {
               height : 300,
               showTaxonCombo : this.showTaxonCombo,
               listeners : {
                  'commit' : {
                     fn : this.getGenesFromList.createDelegate( this ),
                     scope : this
                  },
                  'show' : {
                     fn : function() {
                        if ( this.showTaxonCombo && this.searchForm.getTaxonId() !== null
                           && this.searchForm.getTaxonId() && typeof this.searchForm.getTaxonId() !== 'undefined' ) {
                           this.symbolList._taxonCombo.setTaxonById( this.searchForm.getTaxonId() );
                           this.symbolList._taxonCombo.disable();
                        }
                     },
                     scope : this
                  }
               }
            } );
         },

         /**
          * @Override
          */
         initComponent : function() {

            this.newBoxTriggered = false;
            this.geneCombo = new Gemma.GeneAndGeneGroupCombo( {
               width : 282,
               hideTrigger : true,
               taxonId : this.taxonId,
               emptyText : 'Search by keyword, GO term, ID or symbol'
            } );

            this.geneCombo.on( 'select', function( combo, record, index ) {

               this.searchForm.taxonChanged( record.get( "taxonId" ), record.get( "taxonName" ) );

               var query = combo.store.baseParams.query;
               this.loadGeneOrGroup( record, query );
               this.preview.showPreview();
               // this.preview.show();

               this.preview.setTaxonId( record.get( "taxonId" ) );

               // if this was the first time a selection was
               // made using this box
               if ( combo.startValue === '' && this.newBoxTriggered === false ) {
                  this.fireEvent( 'madeFirstSelection' );
                  this.newBoxTriggered = true;
               }

               combo.disable().hide();
               this.symbolListButton.hide();
               this.helpBtn.hide();

               this.doLayout();

               // currently the event 'geneSelected' is just used for GeneSetOverlayPicker
               this.fireEvent( 'geneSelected' );

            }, this );

            this.relayEvents( this.geneCombo, [ 'select' ] );

            this.symbolListButton = new Ext.Button( {
               icon : Gemma.CONTEXT_PATH + "/images/icons/page_upload.png",
               cls : "x-btn-icon",
               tooltip : Gemma.HelpText.WidgetDefaults.GeneSearchAndPreview.symbolListButtonInstructions,
               disabled : false,
               style : 'padding-right:5px',
               handler : function() {
                  this.geneCombo.reset();
                  this.symbolList = this.createGeneImportPanel();
                  this.symbolList.show();
               }.createDelegate( this, [], true )
            } );

            this.preview = new Gemma.GeneSetPreview();

            this.preview.on( 'geneListModified', function( newSet ) {
               if ( typeof newSet.geneIds !== 'undefined' && typeof newSet.name !== 'undefined' ) {
                  this.setSelectedGeneSetValueObject( newSet );
               }

               // currently the event 'geneSelected' is just used for GeneSetOverlayPicker
               this.fireEvent( 'geneSelected' );

            }, this );

            this.preview.on( 'maskParentContainer', function() {
               this.searchForm.getEl().mask();
            }, this );

            this.preview.on( 'unmaskParentContainer', function() {
               this.searchForm.getEl().unmask();
            }, this );

            this.preview.on( 'removeMe', function() {
               this.reset();
               this.fireEvent( 'removeGene', this );
            }, this );

            this.helpBtn = new Gemma.InlineHelpIcon( {
               tooltipText : Gemma.HelpText.WidgetDefaults.GeneSearchAndPreview.instructions
            } );

            Gemma.EVENTBUS.on( 'taxonchanged', function( taxonId ) {
               this.preview.setTaxonId( taxonId );
            }, this );

            Ext.apply( this, {
               width : 335,
               frame : false,
               border : false,
               hideBorders : true,
               items : [ {
                  layout : 'hbox',
                  hideBorders : true,
                  items : [ this.symbolListButton, this.geneCombo, this.helpBtn ]
               }, this.preview ]
            } );

            this.addEvents( 'geneListUrlSelectionComplete' );

            Gemma.GeneSearchAndPreview.superclass.initComponent.call( this );

         }

      } );

Ext.reg( 'geneSearchAndPreview', Gemma.GeneSearchAndPreview );