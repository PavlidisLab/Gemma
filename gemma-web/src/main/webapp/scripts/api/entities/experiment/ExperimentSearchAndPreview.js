/**
 * A combo box that shows previews of the selection
 * 
 * @author thea
 * @version $Id$
 */
Ext.namespace( 'Gemma' );

Gemma.ExperimentSearchAndPreview = Ext.extend( Ext.Panel, {
   width : 330,

   /**
    * @type {Number}
    */
   taxonId : null, // might be set by parent to control combo

   listModified : false,

   emptyText : "Search by keyword or ID",

   mode : 'coex',

   /**
    * @private
    * @type {ExpressionExperimentSetValueObject}
    */
   selectedExpressionExperimentGroup : null,

   diffExMode : function() {
      this.mode = 'diffex';
      this.fireEvent( 'modechange', this.mode );
   },

   /**
    * Clear current state
    */
   reset : function() {
      this.listModified = false;
      this.queryUsedToGetSessionGroup = null;
      this.preview.reset();
      this.experimentCombo.reset();
      this.selectedExpressionExperimentGroup = null;
      this.experimentCombo.enable().show();
      this.helpBtn.show();
      this.preview.hide();
      this.doLayout();

   },

   coExMode : function() {
      this.mode = 'coex';
      this.fireEvent( 'modechange', this.mode );
   },

   /**
    * 
    * @param {ExpressionExperimentSetValueObject}
    *           eesvo
    */
   setSelectedExpressionExperimentSetValueObject : function( eesvo ) {

      if ( eesvo.resultValueObject != null ) {
         console.log( "Got a wrapped valueobject (has resultValueObject)" );
         this.selectedExpressionExperimentGroup = eesvo.resultValueObject;
      } else {
         this.selectedExpressionExperimentGroup = eesvo;
      }
      this.preview.selectedSetValueObject = this.selectedExpressionExperimentGroup;

   },

   /**
    * @public
    * @return {ExpressionExperimentSetValueObject}
    */
   getSelectedExpressionExperimentSetValueObject : function() {
      return this.selectedExpressionExperimentGroup;
   },

   resetExperimentPreview : function() {
      this.preview.reset();
   },

   showExperimentPreview : function() {
      this.preview.showPreview();
   },

   /**
    * @public
    */
   collapsePreview : function() {
      if ( typeof this.preview !== 'undefined' ) {
         this.preview.collapsePreview();
      }
   },

   /**
    * @memberOf Gemma.ExperimentSearchAndPreview
    */
   maskExperimentPreview : function() {
      if ( !this.loadMask && this.getEl() ) {
         this.loadMask = new Ext.LoadMask( this.getEl(), {
            msg : Gemma.StatusText.Loading.experiments
         } );
      }
      if ( this.loadMask ) {
         this.loadMask.show();
      }
   },

   /**
    * 
    * @param experimentIds
    * @param taxonId
    * @param name
    * @param description
    * @returns {SessionBoundExpressionExperimentSetValueObject}
    */
   makeSessionBoundExperimentSet : function( experimentIds, taxonId, name, description ) {
      // debugger;
      var newEESet = new SessionBoundExpressionExperimentSetValueObject();
      newEESet.modified = false;
      newEESet.expressionExperimentIds = experimentIds;
      newEESet.taxonId = taxonId;
      newEESet.name = name;// 'From Symbol List' etc.;
      newEESet.description = description, newEESet.size = experimentIds.length;
      newEESet.id = null;
      return newEESet;
   },

   /**
    * Show the selected eeset members
    */
   loadExperimentOrGroup : function( record, query ) {

      var vo = record.get( 'resultValueObject' );

      var taxonId = this.searchForm.getTaxonId();
      Gemma.EVENTBUS.fireEvent( 'taxonchanged', taxonId );
      this.queryUsedToGetSessionGroup = (id === null || id === -1) ? query : null;

      if ( vo instanceof SessionBoundExpressionExperimentSetValueObject ) {
         this.setSelectedExpressionExperimentSetValueObject( vo );
      } else if ( vo instanceof ExpressionExperimentValueObject ) {
         var newset = this.makeSessionBoundExperimentSet( [ vo.id ], taxonId, "From experiment",
            'Group made from one experiment' );
         this.setSelectedExpressionExperimentSetValueObject( newset );
      } else {
         this.setSelectedExpressionExperimentSetValueObject( vo );
      }

      this.preview.loadExperimentPreviewFromExperimentSet( this.getSelectedExpressionExperimentSetValueObject() );

      // for bookmarking diff ex viz
      if ( id === null || id === -1 ) {
         var queryToGetSelected = '';
         // figure out if the query is useful to include in a bookmark?
         if ( vo instanceof FreeTextExpressionExperimentResultsValueObject && vo.name.indexOf( query ) != -1 ) {
            queryToGetSelected = "taxon:" + taxonId + "query:" + query;
         }
         this.queryUsedToGetSessionGroup = queryToGetSelected;
      }

   },

   /**
    * handler for select combo.
    * 
    * @param record
    * @param combo
    * @param index
    */
   showPreview : function( combo, record, index ) {

      // if the EE has changed taxon, reset the experiment combo
      this.searchForm.taxonChanged( record.get( 'taxonId' ), record.get( 'taxonName' ) );
      Gemma.EVENTBUS.fireEvent( 'taxonchanged', record.get( 'taxonId' ) );

      // store the eeid(s) selected and load some EE into the
      // previewer
      // store the taxon associated with selection
      var query = combo.store.baseParams.query;
      this.loadExperimentOrGroup( record, query );
      this.preview.showPreview();

      // if this was the first time a selection was made using
      // this box
      if ( combo.startValue === '' && this.newBoxTriggered === false ) {
         this.fireEvent( 'madeFirstSelection' );
         this.newBoxTriggered = true;
      }

      combo.disable().hide();
      this.helpBtn.hide();
      this.doLayout();

   },

   /**
    * @override
    */
   initComponent : function() {

      // Shows the combo box for EE groups
      this.newBoxTriggered = false;
      this.experimentCombo = new Gemma.ExperimentAndExperimentGroupCombo( {
         width : 310,
         taxonId : this.taxonId,
         emptyText : this.emptyText,
         hideTrigger : true
      } );

      this.experimentCombo.on( 'select', this.showPreview, this );

      this.preview = new Gemma.ExperimentSetPreview( {
         hideUnanalyzedDatasets : true
      } );

      this.preview.on( 'experimentListModified', function( newSet ) {

         if ( typeof newSet.expressionExperimentIds !== 'undefined' && typeof newSet.name !== 'undefined' ) {
            // update record
            this.setSelectedExpressionExperimentSetValueObject( newSet );
         }

      }, this );

      this.preview.on( 'maskParentContainer', function() {
         this.searchForm.getEl().mask();
      }, this );

      this.preview.on( 'unmaskParentContainer', function() {
         this.searchForm.getEl().unmask();
      }, this );

      this.preview.on( 'removeMe', function() {
         this.reset();
         this.fireEvent( 'removeExperiment', this );
      }, this );

      this.searchForm.on( 'modechange', function( mode ) {
         this.mode = mode;
         this.experimentCombo.setMode( mode );
         this.preview.setMode( mode );
      }, this );

      this.helpBtn = new Gemma.InlineHelpIcon( {
         tooltipText : Gemma.HelpText.WidgetDefaults.ExperimentSearchAndPreview.widgetHelpTT
      } );

      Ext.apply( this, {
         frame : false,
         border : false,
         hideBorders : true,
         items : [ {
            layout : 'hbox',
            hideBorders : true,
            items : [ this.experimentCombo, this.helpBtn ]
         }, this.preview ]
      } );
      Gemma.ExperimentSearchAndPreview.superclass.initComponent.call( this );
   }
} );

Ext.reg( 'experimentSearchAndPreview', Gemma.ExperimentSearchAndPreview );
