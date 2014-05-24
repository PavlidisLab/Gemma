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
 * @class Gemma.ExperimentSetPreview
 * @xtype Gemma.ExperimentSetPreview
 */
Gemma.ExperimentSetPreview = Ext.extend( Gemma.SetPreview, {

   /**
    * update the contents of the experiment preview box
    * 
    * @memberOf Gemma.ExperimentSetPreview
    * @private
    * @param {Number[]}
    *           ids an array of experimentIds to use to populate preview
    */
   loadExperimentPreviewFromIds : function( ids ) {
      this.entityIds = ids;
      // load some experiments to display
      var limit = (ids.size() < this.preview_size) ? ids.size() : this.preview_size;
      var previewIds = ids.slice( 0, limit );

      ExpressionExperimentController.loadExpressionExperiments( previewIds, {
         callback : function( ees ) {
            this.loadPreview( ees, ids.length );
         }.createDelegate( this ),
         errorHandler : Gemma.genericErrorHandler
      } );
   },

   /**
    * Whether we are in coexpression or differential expression mode
    */
   mode : 'coex',

   /**
    * @public update the contents of the experiment preview box
    * 
    * @param {ExperimentValueSetObject[]}
    *           experimentSet populate preview with members
    * @memberOf Gemma.ExperimentSetPreview
    */
   loadExperimentPreviewFromExperimentSet : function( eeSet ) {

      var ids = eeSet.expressionExperimentIds;
      this.setSelectedSetValueObject( eeSet );

      if ( ids.length > 0 ) {
         this.loadExperimentPreviewFromIds( ids );
      } else if ( eeSet.id > 0 ) {
         // fetch from server.
         ExpressionExperimentSetController.getExperimentsInSet.apply( this, [ eeSet.id, this.preview_size, {
            callback : function( experiments ) {
               this.loadPreview( experiments, this.selectedSetValueObject.size );
               this.fireEvent( 'previewLoaded', experiments );
            }.createDelegate( this ),
            errorHandler : Gemma.genericErrorHandler
         } ] );
      } else {
         alert( "Could not load" );
      }

   },

   /**
    * @memberOf Gemma.ExperimentSetPreview
    * @param mode
    */
   setMode : function( mode ) {
      this.mode = mode;
      this.updateTitle();
   },

   reset : function() {
      this.resetPreview();
      this.entityIds = null;
      this.previewContent.setTitle( null );
      this.selectedSetValueObject = null;
   },

   /**
    * public don't use params if you want to update name based on this.selectedEntityOrGroup
    * 
    * @param {Object}
    *           selectedSet
    * 
    */
   updateTitle : function() {

      var selectedSet = this.selectedSetValueObject;

      if ( typeof selectedSet == undefined || selectedSet == null ) {
         return;
      }

      // if an experiment group page exists for this set, make title a link
      var size = selectedSet.size > 0 ? selectedSet.size : selectedSet.expressionExperimentIds.size();

      var numWithCoex = selectedSet.numWithCoexpressionAnalysis;
      var numWithDiffex = selectedSet.numWithDifferentialExpressionAnalysis;

      if ( !(selectedSet instanceof SessionBoundExpressionExperimentSetValueObject) ) {
         name = '<a target="_blank" href="/Gemma/expressionExperimentSet/showExpressionExperimentSet.html?id='
            + selectedSet.id + '">' + selectedSet.name + '</a>';
      } else {
         name = selectedSet.name;
      }

      var usableSize;
      if ( this.mode == 'coex' ) {
         usableSize = numWithCoex;
      } else {
         usableSize = numWithDiffex;
      }

      this.previewContent.setTitle( '<span style="font-size:1.2em">' + name
         + '</span> &nbsp;&nbsp;<span style="font-weight:normal">' + usableSize
         + (usableSize > 1 ? " experiments" : " experiment ") + ' usable of ' + size );

   },

   /**
    * @memberOf Gemma.ExperimentSetPreview
    * @param combo
    * @param record
    * @param newIds
    * @private
    */
   mergeAndCreateSessionSet : function( combo, record, newIds ) {

      // FIXME make this like the gene one. IF its an existing set call it 'modified from'; if it's based on a query
      // name it after the query.
      var allIds = this.selectedSetValueObject.expressionExperimentIds;

      // don't add duplicates
      for (var i = 0; i < newIds.length; i++) {
         if ( allIds.indexOf( newIds[i] ) < 0 ) {
            allIds.push( newIds[i] );
         }
      }
      var currentTime = new Date();
      var hours = currentTime.getHours();
      var minutes = currentTime.getMinutes();
      if ( minutes < 10 ) {
         minutes = "0" + minutes;
      }
      var time = '(' + hours + ':' + minutes + ') ';

      var editedGroup;
      editedGroup = new SessionBoundExpressionExperimentSetValueObject();
      editedGroup.id = null;
      editedGroup.name = time + " Custom Experiment Group";
      editedGroup.description = "Temporary experiment group created " + currentTime.toString();
      editedGroup.expressionExperimentIds = allIds;
      editedGroup.taxonId = record.get( 'taxonId' );
      editedGroup.taxonName = record.get( 'taxonName' );
      editedGroup.numExperiments = editedGroup.expressionExperimentIds.length;
      editedGroup.modified = true;
      editedGroup.isPublic = false;

      editedGroup.numWithCoexpressionAnalysis = -1; // TODO
      editedGroup.numWithDifferentialExpressionAnalysis = -1; // TODO

      ExpressionExperimentSetController.addSessionGroup( editedGroup, true, function( newValueObject ) {
         combo.reset();
         combo.blur();
         this.setSelectedSetValueObject( newValueObject );
         this.loadExperimentPreviewFromExperimentSet( newValueObject );

         this.updateTitle();
         this.fireEvent( 'experimentListModified', newValueObject );
         this.fireEvent( 'doneModification' );

      }.createDelegate( this ) );

   },

   /**
    * @Override
    * @memberOf Gemma.ExperimentSetPreview
    */
   initComponent : function() {

      var withinSetExperimentCombo = new Gemma.ExperimentAndExperimentGroupCombo( {
         width : 300,
         style : 'margin:10px',
         hideTrigger : true,
         emptyText : 'Add experiments to your group'
      } );

      withinSetExperimentCombo.setTaxonId( this.taxonId );
      withinSetExperimentCombo.on( 'select', function( combo, record, index ) {

         var rvo = record.get( 'resultValueObject' );

         if ( rvo instanceof SessionBoundExpressionExperimentSetValueObject ) {
            this.mergeAndCreateSessionSet( combo, record, rvo.expressionExperimentIds );
         } else if ( rvo instanceof ExpressionExperimentValueObject ) {
            var singleId = [];
            singleId.push( record.data.resultValueObject.id );
            this.mergeAndCreateSessionSet( combo, record, singleId );
         } else {
            ExpressionExperimentSetController.getExperimentIdsInSet( rvo.id, {
               callback : function( expIds ) {
                  this.mergeAndCreateSessionSet( combo, record, expIds );
               }.createDelegate( this ),
               errorHandler : Gemma.genericErrorHandler
            } );
         }

      }, this );

      Ext.apply( this, {
         selectionEditor : new Gemma.ExpressionExperimentMembersGrid( {
            name : 'selectionEditor',
            hideHeaders : true,
            frame : false,
            width : 500,
            height : 500
         } ),
         defaultTpl : new Ext.XTemplate( '<tpl for="."><div style="padding-bottom:7px;">'
            + '<a target="_blank" href="/Gemma/expressionExperiment/showExpressionExperiment.html?id=', '{id}"',
            ' ext:qtip="{shortName}">{shortName}</a>&nbsp; {name} <span style="color:grey">({taxon}) '
               + '{hasCoexpressionAnalysis} {hasDifferentialExpressionAnalysis}</span></div></tpl>' ),

         defaultPreviewTitle : "Experiment Selection Preview",

         addingCombo : withinSetExperimentCombo

      } );

      Gemma.ExperimentSetPreview.superclass.initComponent.call( this );

      this.selectionEditor.on( 'experimentListModified', function( newSet ) {
         if ( typeof newSet.expressionExperimentIds !== 'undefined' && typeof newSet.name !== 'undefined' ) {
            this.setSelectedSetValueObject( newSet );
            this.loadExperimentPreviewFromExperimentSet( newSet );
            this.updateTitle();

         }
         this.listModified = true;

         this.fireEvent( 'experimentListModified', newSet );
      }, this );

   }

} );

Ext.reg( 'Gemma.ExperimentSetPreview', Gemma.ExperimentSetPreview );