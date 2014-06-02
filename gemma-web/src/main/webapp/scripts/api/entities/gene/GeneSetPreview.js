/**
 * 
 * @author thea
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

/**
 * 
 * Displays a small number of elements from the set with links to the set's page and to an editor
 * 
 * @class Gemma.GeneSetPreview
 * @xtype Gemma.GeneSetPreview
 */
Gemma.GeneSetPreview = Ext.extend( Gemma.SetPreview, {

   /**
    * Fetch some genes as examples.
    * 
    * @private
    * 
    * @param {Number[]}
    *           ids an array of geneIds to use to populate preview
    * @memberOf Gene.GeneSetPreview
    */
   _loadGenePreviewFromIds : function( ids, message ) {

      // load some genes to display
      var limit = (ids.size() < this.preview_size) ? ids.size() : this.preview_size;
      var previewIds = ids.slice( 0, limit );
      GenePickerController.getGenes( previewIds, {
         callback : function( genes ) {
            this.loadPreview( genes, ids.length, message );
            this.fireEvent( 'previewLoaded', genes );
         }.createDelegate( this ),
         errorHandler : Gemma.genericErrorHandler
      } );
   },

   reset : function() {
      this.resetPreview();
      this.previewContent.setTitle( null );
      this.setSelectedSetValueObject( null );
   },

   /**
    * public update the contents of the gene preview box
    * 
    * @param {GeneValueSetObject[]}
    *           geneSet populate preview with members
    */
   loadGenePreviewFromGeneSet : function( geneSet, message ) {

      /*
       * FIXME this is a little confusing 'preview' and setting the actual set.
       */
      this.setSelectedSetValueObject( geneSet );

      var ids = geneSet.geneIds;

      if ( ids.length > 0 ) {
         this._loadGenePreviewFromIds( ids, message );
      } else if ( geneSet.id > 0 ) {
         // fetch from server.
         GeneSetController.getGenesInGroup.apply( this, [ geneSet.id, this.preview_size, {
            callback : function( genes ) {
               this.loadPreview( genes, this.selectedSetValueObject.size, message );
               this.fireEvent( 'previewLoaded', genes );
            }.createDelegate( this ),
            errorHandler : Gemma.genericErrorHandler
         } ] );
      } else {
         alert( "Could not load" );
      }

   },

   /**
    * @public update the contents of the gene preview box. This is used when other components are adding genes to the
    *         query (e.g.. from the cytoscape view)
    * 
    * @param {GeneValueObject[]}
    *           genes an array of genes to use to populate preview
    */
   loadGenePreviewFromGenes : function( genes, message ) {
      var limit = (genes.size() < this.preview_size) ? genes.size() : this.preview_size;
      var previewGenes = genes.slice( 0, limit );
      this.loadPreview( previewGenes, genes.length, message );
   },

   /**
    * 
    */
   updateTitle : function() {
      var selectedSet = this.selectedSetValueObject;

      if ( typeof selectedSet == undefined || selectedSet == null ) {
         return;
      }
      var size = selectedSet.size > 0 ? selectedSet.size : selectedSet.geneIds.size();

      if ( selectedSet instanceof DatabaseBackedGeneSetValueObject ) {

         name = "<a target=\"_blank\" href=\"" + Gemma.LinkRoots.geneSetPage + selectedSet.id + '">' + selectedSet.name
            + '</a>';

      } else if ( selectedSet instanceof PhenotypeGroupValueObject ) {

         name = "<a target=\"_blank\" href=\"" + Gemma.LinkRoots.phenotypePage + selectedSet.phenotypeName + '">'
            + selectedSet.name + ": " + selectedSet.description + '</a>';

      } else if ( selectedSet instanceof GOGroupValueObject ) {
         name = selectedSet.name + ": " + selectedSet.description;
      } else if ( selectedSet instanceof PhenotypeGroupValueObject ) {
         name = selectedSet.name + ": " + selectedSet.description;
      } else {
         name = selectedSet.name;
      }

      this.previewContent.setTitle( '<span style="font-size:1.2em">' + name
         + '</span> &nbsp;&nbsp;<span style="font-weight:normal">(' + size + ((size > 1) ? " genes)" : " gene)") );
   },

   /**
    * Given the current selection, when the user selects another result from the combo: we merge it in (FIXME harmonize
    * with ExperimentSetPreview)
    * 
    * @param combo
    * @param record
    * @param index
    * @returns
    */
   addToPreviewedSet : function( combo, record, index ) {

      var geneSet = record.get( 'resultValueObject' );

      if ( geneSet.geneIds.length > 0 ) {
         /*
          * Work with those directly.
          */
         var newIds = geneSet.geneIds;
         this._appendAndUpdate( newIds );

      } else if ( geneSet.id != null ) {
         /*
          * Add the genes to the current set. This is a bit clumsy...
          */
         GeneSetController.load( geneSet.id, function( fetched ) {
            this._appendAndUpdate( fetched.geneIds );
         }.createDelegate( this ) );

      } else {
         throw 'Cannot add to preview from this type of object';
      }

      var allIds = this.selectedSetValueObject.geneIds;

   },

   /**
    * @private
    * @param geneIdsToAdd
    *           {Array}
    */
   _appendAndUpdate : function( geneIdsToAdd ) {

      var allIds = this.selectedSetValueObject.geneIds;
      /*
       * FIXME: are these always all the geneIds? Preview-mode suggests not
       * 
       */

      for (var i = 0; i < geneIdsToAdd.length; i++) {
         if ( allIds.indexOf( geneIdsToAdd[i] ) < 0 ) {
            allIds.push( geneIdsToAdd[i] );
         }
      }

      /*
       * if the current selection is just a session group, don't create a new one.
       */
      var editedGroup;

      if ( typeof this.selectedSetValueObject == 'SessionBoundGeneSetValueObject' ) {
         /*
          * Don't wipe it, just add on.
          */
         editedGroup = this.selectedSetValueObject;
         editedGroup.modified = true;
         editedGroup.geneIds = allIds;
         editedGroup.size = editedGroup.geneIds.length;

         GeneSetController.updateSessionGroup( editedGroup, true, // returns datasets added
         function( geneSet ) {

            this._loadGenePreviewFromIds( geneSet.geneIds ); // async
            this.setSelectedSetValueObject( geneSet );
            this.updateTitle();
            this.withinSetGeneCombo.reset();
            this.withinSetGeneCombo.blur();

            Fthis.fireEvent( 'geneListModified', geneSet );
            this.fireEvent( 'doneModification' );

         }.createDelegate( this ) /* FIXME error handler */);

      } else {

         editedGroup = new SessionBoundGeneSetValueObject();
         editedGroup.id = null;

         if ( !(this.selectedSetValueObject.name.lastIndexOf( "Modification of:\n" ) === 0) ) {
            var currentTime = new Date();
            var hours = currentTime.getHours();
            var minutes = currentTime.getMinutes();
            var time = '(' + hours + ':' + minutes + ') ';
            editedGroup.name = "Modification of:\n" + this.selectedSetValueObject.name;
            editedGroup.description = "You created this set by combining multiple items. Starting point was:\n"
               + this.selectedSetValueObject.name + " (at " + time + ")";
         } else {
            editedGroup.name = this.selectedSetValueObject.name;
            editedGroup.description = this.selectedSetValueObject.description;
         }

         editedGroup.geneIds = allIds;
         editedGroup.taxonId = this.selectedSetValueObject.taxonId;
         editedGroup.taxonName = this.selectedSetValueObject.taxonName;
         editedGroup.size = editedGroup.geneIds.length;
         editedGroup.modified = true;
         editedGroup.isPublic = false;

         GeneSetController.addSessionGroup( editedGroup, true, // returns datasets added
         function( geneSet ) {

            this._loadGenePreviewFromIds( geneSet.geneIds ); // async
            this.setSelectedSetValueObject( geneSet );
            this.updateTitle();

            this.withinSetGeneCombo.reset();
            this.withinSetGeneCombo.blur();

            this.fireEvent( 'geneListModified', geneSet );
            this.fireEvent( 'doneModification' );

         }.createDelegate( this ) /* FIXME error handler */);
      }
   },

   /**
    * @override
    */
   initComponent : function() {

      this.withinSetGeneCombo = new Gemma.GeneAndGeneGroupCombo( {
         width : 300,
         style : 'margin:10px',
         hideTrigger : true,
         taxonId : this.taxonId,
         emptyText : 'Add genes to your group'
      } );
      this.withinSetGeneCombo.setTaxonId( this.taxonId );

      // FIXME might want to do this by firing an event and let container handle (as it stands, preview and management
      // are intertwined)
      this.withinSetGeneCombo.on( 'select', this.addToPreviewedSet.createDelegate( this ), this );

      Ext.apply( this, {
         selectionEditor : new Gemma.GeneMembersSaveGrid( {
            name : 'geneSelectionEditor',
            hideHeaders : true,
            width : 500,
            height : 500,
            frame : false
         } ),
         defaultTpl : new Ext.Template( '<div style="padding-bottom:7px;">'
            + '<a target="_blank" href="/Gemma/gene/showGene.html?id={id}">{officialSymbol}</a> {officialName} '
            + '<span style="color:grey">({taxonCommonName})</span></div>' ),

         defaultPreviewTitle : "Gene Selection Preview",

         addingCombo : this.withinSetGeneCombo

      } );

      Gemma.GeneSetPreview.superclass.initComponent.call( this );

      this.selectionEditor.on( 'geneListModified', function( geneset ) {
         if ( typeof geneset.geneIds !== 'undefined' && typeof geneset.name !== 'undefined' ) {
            this._loadGenePreviewFromIds( geneset.geneIds );
            this.setSelectedSetValueObject( geneset );
            this.updateTitle();
         }
         this.fireEvent( 'geneListModified', geneset );
      }, this );

   }

} );

Ext.reg( 'Gemma.GeneSetPreview', Gemma.GeneSetPreview );