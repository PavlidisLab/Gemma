/**
 * Window to choose a set of genes for overlay on the visualization
 * 
 * This is like Gemma.GeneSearchAndPreview
 */
Gemma.GeneSetOverlayPicker = Ext.extend( Ext.Window, {

   titleText : "Select genes for graph overlay",
   title : "Select genes for graph overlay",
   modal : true,
   layout : 'fit',
   stateful : false,
   autoHeight : false,
   width : 360,
   height : 400,
   closeAction : 'hide',
   easing : 3,

   /**
    * @memberOf Gemma.GeneSetOverlapPicker
    */
   initComponent : function() {

      this.addGeneChooser();

      Ext.apply( this, {
         items : [ this.geneChooser ],
         buttons : [ {
            text : 'OK',
            handler : this.applyOverlay,
            scope : this
         }, {
            text : 'Clear',
            scope : this,
            handler : this.reset
         }, {
            text : 'Cancel',
            handler : function() {
               // this.reset();
               this.hide();
            }.createDelegate( this ),
            scope : this
         } ]
      } );

      Gemma.GeneSetOverlayPicker.superclass.initComponent.call( this );
   },

   /**
    * 
    */
   reset : function() {
      this.geneChooser.reset();
      this.setTitle( this.titleText );
      this.display.coexDisplaySettings.setOverlayGeneIds( [] );
   },

   /**
    * 
    */
   applyOverlay : function() {
      this.display.coexDisplaySettings.setOverlayGeneIds( this.getSelectedIds() );
      this.hide();
   },

   /**
    * 
    * @param numNodesMatched
    * @param numNodesMatchedWithHidden
    * @returns {String}
    */
   getTextForTitle : function( numNodesMatched, numNodesMatchedWithHidden ) {
      var textForTitle = this.titleText + "<br/> (" + numNodesMatched + " gene matches in the graph";
      if ( numNodesMatchedWithHidden > 0 ) {
         textForTitle = textForTitle + ", " + numNodesMatchedWithHidden + " hidden due to stringency)";
      } else {
         textForTitle = textForTitle + ")";
      }
      return textForTitle;
   },

   show : function() {
      if ( this.getSelectedIds().length > 0 ) {
         var r = this.display.getNodesMatching( this.getSelectedIds() );
         var numNodesMatched = r.total;
         var numNodesMatchedWithHidden = r.hidden;
         this.setTitle( this.getTextForTitle( numNodesMatched, numNodesMatchedWithHidden ) );
      }
      Gemma.GeneSetOverlayPicker.superclass.show.call( this );
   },

   /**
    * For the selection of genes to overlay.
    * 
    * @returns {Gemma.GeneSearchAndPreview}
    */
   addGeneChooser : function() {
      var cytoscapeDisplay = this.display;
      this.geneChooser = new Gemma.GeneSearchAndPreview(
         {
            frame : false,
            defaults : {
               border : false
            },
            style : 'padding-bottom: 10px;',
            autoDestroy : true,
            searchForm : this,
            style : 'padding-top:10px;padding-left:10px;',
            taxonId : this.taxonId,
            showTaxonCombo : false,
            listeners : {
               geneSelected : function() {
                  var r = cytoscapeDisplay.getNodesMatching( this.searchForm.getSelectedIds() );
                  var numNodesMatched = r.total;
                  var numNodesMatchedWithHidden = r.hidden;
                  this.searchForm.setTitle( this.searchForm
                     .getTextForTitle( numNodesMatched, numNodesMatchedWithHidden ) );
               }
            }
         } );

      // change previous button to 'remove'
      if ( typeof this.geneChooser !== 'undefined' ) {
         // Ext.getCmp( 'geneOverlayChooser' + (this.geneChooserIndex - 1) + 'Button' ).show().setIcon(
         // Gemma.CONTEXT_PATH + '/images/icons/delete.png' ).setTooltip( 'Remove this gene or group from your search' ).setHandler(
         // this.reset.createDelegate( this ) );
      }
      this.geneChooser.doLayout();
   },

   // The following two functions are present because the code of
   // GeneSearchAndPreview is tightly coupled to its container
   // events would probably be a better way(at least for taxonChanged) because
   // then widgets choosing to use GeneSearchAndPreview can just choose to not
   // listen to the event

   taxonChanged : function( taxonId, taxonName ) {
      // taxon never changes for GeneSetOverlayPicker, it is defined by the
      // search
      // need this because GeneSearchAndPreview(the widget I am reusing)
      // expects
      // this method to be present
   },

   getTaxonName : function() {
      // taxon never changes for GeneSetOverlayPicker, it is defined by the
      // search
      // need this because GeneSearchAndPreview(the widget I am reusing)
      // expects
      // this method to be present, the return value is only used to call
      // taxonChanged(taxonId, taxonName)
      // which is empty
      return "";
   },

   getTaxonId : function() {
      return this.taxonId;
   },

   getSelectedIds : function() {
      var vo = this.geneChooser.getSelectedGeneSetValueObject();
      if ( !vo )
         return [];
      return vo.geneIds;
   }
} );