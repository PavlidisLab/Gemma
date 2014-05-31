/**
 * Window to choose a set of genes for overlay on the visualization
 */
Gemma.GeneSetOverlayPicker = Ext.extend( Ext.Window,
   {

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
         this.geneChoosers = new Ext.Panel( {
            frame : false,
            defaults : {
               border : false
            },
            style : 'padding-bottom: 10px;',
            autoDestroy : true
         } );
         this.geneChooserIndex = -1;
         this.initialGeneChooser = this.addGeneChooser();

         Ext.apply( this, {
            items : [ this.geneChoosers ],
            buttons : [ {
               text : 'OK',
               handler : function() {
                  this.applyOverlay();
               },
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

      reset : function() {
         this.geneChoosers.removeAll();
         this.addGeneChooser();
         this.setTitle( this.titleText );
      },

      applyOverlay : function() {
         this.display.coexDisplaySettings.setOverlayGeneIds( this.getSelectedIds() );
         this.hide();
      },

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

      addGeneChooser : function() {
         this.geneChooserIndex++;
         var cytoscapeDisplay = this.display;
         var chooser = new Gemma.GeneSearchAndPreview( {
            searchForm : this,
            style : 'padding-top:10px;padding-left:10px;',
            id : 'geneOverlayChooser' + this.geneChooserIndex,
            taxonId : this.taxonId,
            showTaxonCombo : false,
            listeners : {
               geneSelected : function() {
                  var r = cytoscapeDisplay.getNodesMatching( this.searchForm.getSelectedIds() );
                  var numNodesMatched = r.total;
                  var numNodesMatchedWithHidden = r.hidden;

                  this.searchForm.setTitle( this.searchForm
                     .getTextForTitle( numNodesMatched, numNodesMatchedWithHidden ) );
               },
               removeGene : function() {
                  this.searchForm.removeGeneChooser( this.getId() );
               }
            }
         } );

         this.geneChoosers.add( chooser );

         // change previous button to 'remove'
         if ( typeof Ext.getCmp( 'geneOverlayChooser' + (this.geneChooserIndex - 1) + 'Button' ) !== 'undefined' ) {
            Ext.getCmp( 'geneOverlayChooser' + (this.geneChooserIndex - 1) + 'Button' ).show().setIcon(
               '/Gemma/images/icons/delete.png' ).setTooltip( 'Remove this gene or group from your search' )
               .setHandler(
                  this.removeGeneChooser.createDelegate( this, [ 'geneChooserPanel' + (this.geneChooserIndex - 1) ],
                     false ) );
         }
         this.geneChoosers.doLayout();
         return chooser;
      },

      removeGeneChooser : function( panelId ) {
         this.geneChoosers.remove( panelId, true );
         this.geneChoosers.doLayout();
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
         var selectedIds = [];
         // we only have one genechooser currently, so this each loop may be a
         // little unnecessary
         this.geneChoosers.items.each( function() {
            if ( this instanceof Gemma.GeneSearchAndPreview && this.getSelectedGeneOrGeneSetValueObject() ) {
               var vo = this.getSelectedGeneOrGeneSetValueObject();
               if ( vo instanceof GeneValueObject ) {
                  selectedIds.push( vo.id );
               } else {
                  for (var i = 0; i < vo.geneIds.length; i++) {
                     selectedIds.push( vo.geneIds[i] );
                  }
               }
            }
         } );
         return selectedIds;
      }
   } );