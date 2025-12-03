/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
Ext.namespace( 'Gemma' );

/**
 * Basic form to allow user to search for genes or show 'random' set of vectors for the experiment.
 * 
 * Used in GeneSearchVisualizationPage.js
 * 
 * @author paul, based on older code.
 * 
 */
Gemma.EEDetailsVisualizationWidget = Ext.extend( Gemma.GeneGrid, {

   height : 220,
   width : 550,
   name : 'eedvw',

   vizButtonId : "visualizeButton-" + Ext.id(),

   /**
    * @memberOf Gemma.EEDetailsVisualizationWidget
    */
   initComponent : function() {

      // has to be done after constructor is done creating the handler...
      this.geneGroupCombo = new Gemma.GeneGroupCombo( {
         id : "visGeneGroupCombo",
         hideTrigger : true,
         emptyText : 'Search for genes to visualize',
         listeners : {
            'select' : {
               fn : function( combo, record, index ) {
                  var loadMask = new Ext.LoadMask( this.getEl(), {
                     msg : "Loading Genes for " + record.get( 'name' ) + " ..."
                  } );
                  loadMask.show();

                  this.loadGenes( record.get( 'geneIds' ), function() {
                     loadMask.hide();
                  } );
               },
               scope : this
            }
         }
      } );

      Ext.apply( this, {
         extraButtons : [ this.geneGroupCombo, new Ext.Button( {
            text : "Clear",
            tooltip : "Clear gene selection",
            handler : this.clearButHandler,
            scope : this
         } ), {
            xtype : 'tbfill'
         },

         new Ext.Button( {
            id : this.vizButtonId,
            text : "Visualize",
            tooltip : Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.visualizaButtonTT,
            handler : this.showButHandler,
            scope : this,
            cls : 'x-toolbar-standardbutton'
         } ), new Ext.Button( {
            icon : Gemma.CONTEXT_PATH + '/images/icons/information.png',
            // text : 'help',
            tootltip : "Get help",
            handler : function() {
               Ext.Msg.show( {
                  title : 'Visualization',
                  msg : Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.instructions,
                  buttons : Ext.Msg.OK,
                  icon : Ext.MessageBox.INFO
               } );
            }.createDelegate( this )
         } ) ]
      } );
      Gemma.EEDetailsVisualizationWidget.superclass.initComponent.call( this );

      this.on( 'ready', function() {
         /*
          * Taxon is passed in during construction.
          */
         var foundTaxon = this.getTopToolbar().taxonCombo.setTaxonByCommonName( this.taxon.commonName );
         this.getTopToolbar().taxonCombo.hide();
         this.geneGroupCombo.taxon = this.taxon;
         this.taxonChanged( foundTaxon, false );
         this.getTopToolbar().taxonCombo.disable( false );
      } );

   },

   clearButHandler : function() {
      this.removeAllGenes();
   },

   /**
    * @memberOf Gemma.EEDetailsVisualizationWidget
    */
   showButHandler : function() {

      if ( this.visWindow ) {
         this.visWindow.close();
         this.visWindow.destroy();
      }

      var geneList = this.getGeneIds();
      var eeId = (Ext.get( "eeId" )) ? Ext.get( "eeId" ).getValue() : this.eeId;
      var title = '';
      var downloadLink = '';
      if ( geneList.length > 0 ) {
         title = "Data for selected genes";
         downloadLink = String.format( Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}&g={1}", eeId, geneList.join( ',' ) );
      } else {
         geneList = [];
         title = "Data for a 'random' sampling of probes";
         downloadLink = String.format( Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}", eeId );
      }
      this.visWindow = new Gemma.VisualizationWithThumbsWindow( {
         title : title,
         thumbnails : false,
         downloadLink : downloadLink
      } );

      this.visWindow.show( {
         params : [ [ eeId ], geneList ]
      } );
   }

} );

/**
 * toolbar for selecting genes or gene groups and adding them to a grid if this.taxonId is set, then searches will be
 * limited by taxon
 */
Gemma.VisualizationWidgetGeneSelectionToolbar = Ext.extend( Ext.Toolbar, {
   extraButtons : [],
   geneIds : [],
   taxonId : null,

   initComponent : function() {
      // debugger;
      Gemma.VisualizationWidgetGeneSelectionToolbar.superclass.initComponent.call( this );

      this.visPanel.on( 'loadSucceeded', function( returnedGeneCount, queryGeneCount ) {
         if ( returnedGeneCount !== undefined && queryGeneCount !== undefined && returnedGeneCount !== null
            && queryGeneCount !== null && queryGeneCount > 0 ) {
            this.updateFoundVsQueryText( returnedGeneCount, queryGeneCount );
         } else {
            this.updateStatusText();
         }
      }, this );

      this.visPanel.on( 'loadFiled', function() {
      }, this );

      this.geneCombo = new Gemma.GeneAndGeneGroupCombo( {
         typeAhead : false,
         width : this.geneComboWidth || 300,
         taxonId : this.taxonId,
         listeners : {
            'select' : {
               fn : function( combo, rec, index ) {
                  this.setGeneIds( rec.get( 'memberIds' ) ); // I want to deprecate memberIds.
                  this.setSelectedComboRecord( rec.data );
                  this.editBtn.enable();
                  this.clearBtn.enable();
                  this.updateButtonText();

               }.createDelegate( this )
            }
         }
      } );
      this.vizBtn = new Ext.Toolbar.Button( {
         tooltip : "Visualize selected gene(s)",
         text : Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.GoButtonText.random,
         cls : 'x-toolbar-standardbutton',
         handler : this.vizBtnHandler.createDelegate( this )
      } );

      this.editBtn = new Ext.Toolbar.Button( {
         tooltip : "Edit your selection",
         text : 'Edit Selection',
         cls : 'x-toolbar-outline',
         disabled : true,
         handler : this.launchGeneSelectionEditor.createDelegate( this )
      } );

      this.clearBtn = new Ext.Toolbar.Button( {
         tooltip : "Clear your selection",
         text : 'Clear',
         cls : 'x-toolbar-outline',
         disabled : true,
         handler : this.clearHandler.createDelegate( this )
      } );

      this.geneSelectionEditor = new Gemma.GeneMembersSaveGrid( {
         name : 'geneSelectionEditor',
         hideHeaders : true,
         frame : false
      } );
      this.geneSelectionEditor.setTaxonId( this.taxonId );

      this.geneSelectionEditor.on( 'geneListModified', function( geneSet ) {
         // This will (always? generally?) be a SessionBoundGeneSetValueObject.
         this.setGeneIds( geneSet.geneIds );
         this.updateButtonText();
         this.geneCombo.setValue( geneSet.name );
         this.listModified = true;
      }, this );

      this.geneSelectionEditor.on( 'doneModification', function() {
         this.getEl().unmask();
         this.geneSelectionEditorWindow.hide();
      }, this );

      this.geneSelectionEditorWindow = new Ext.Window( {
         closable : false,
         layout : 'fit',
         width : 450,
         height : 500,
         items : this.geneSelectionEditor,
         title : 'Edit Your Gene Selection'
      } );
      this.tbarText = new Ext.Panel( {
         xtype : 'panel',
         html : 'Visualizing 20 \'random\' genes',
         border : false,
         bodyStyle : 'background-color:transparent; color:grey; padding-left:5px'
      } );
      // if(this.showRefresh){
      // this.refreshButton = {
      // xtype : "button",
      // text: "Refresh",
      // icon: Gemma.CONTEXT_PATH + '/images/icons/arrow_refresh_small.png',
      // tooltip: "Refresh the caches for this experiment",
      // handler:function(){
      // if(this.eeId && this.eeId > 0){
      // Ext.getBody().mask('Refreshing ...');
      // var callBackFunc = function( msg ){
      // Ext.getBody().unmask();
      // if( msg == null || msg == "" ){
      // this.fireEvent('refreshVisualisation');
      // }else{
      // Ext.Msg.alert( "Refresh failed: " + msg );
      // }
      // };
      // ExperimentalDesignController.clearDesignCaches( this.eeId, callBackFunc.createDelegate(this) );
      // }else{
      // Ext.Msg.alert('Missing experiment parameter.');
      // }
      // },
      // scope: this
      // };
      // }

   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   afterRender : function( c, l ) {
      Gemma.GeneAndGroupAdderToolbar.superclass.afterRender.call( this, c, l );
      this.add( this.geneCombo, this.editBtn );
      this.addSpacer();
      this.add( this.clearBtn, this.vizBtn );
      this.addButton( this.extraButtons );
      this.add( this.tbarText );
      // if(this.showRefresh){
      // this.addFill();
      // this.add(this.refreshButton);
      // }

   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   launchGeneSelectionEditor : function() {

      var geneIds = this.getGeneIds();
      if ( !geneIds || geneIds === null || geneIds.length === 0 ) {
          return;
      }
      this.getEl().mask();

      this.geneSelectionEditorWindow.show();

      this.geneSelectionEditor.setSelectedGeneSetValueObject( this.getSelectedValueObject() ); // for possible saving

      this.geneSelectionEditor.loadMask = new Ext.LoadMask( this.geneSelectionEditor.getEl(), {
         msg : "Loading genes ..."
      } );
      this.geneSelectionEditor.loadMask.show();
      Ext.apply( this.geneSelectionEditor, {
         taxonId : this.taxonId
      } );
      this.geneSelectionEditor.loadGenes( geneIds, function() {
         this.geneSelectionEditor.loadMask.hide();
      }.createDelegate( this, [], false ) );
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   getGeneIds : function() {
      return this.geneIds;
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   setGeneIds : function( ids ) {
      this.geneIds = ids;
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   setSelectedComboRecord : function( record ) {
      this.selectedComboRecord = record;
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   getSelectedComboRecord : function() {
      return this.selectedComboRecord;
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   getSelectedValueObject : function() {
      return (this.selectedComboRecord) ? this.selectedComboRecord.resultValueObject : null;
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   updateButtonText : function() {
      var numIds = this.getGeneIds().length;
      if ( numIds === 0 ) {
         this.vizBtn.setText( Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.GoButtonText.random );
      } else if ( numIds === 1 ) {
         this.vizBtn.setText( Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.GoButtonText.one );
      } else {
         this.vizBtn.setText( String.format(
            Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.GoButtonText.multiple, numIds ) );
      }
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   updateStatusText : function( status ) {
      if ( status && status.length > 0 ) {
         this.tbarText.update( status );
      } else {
         var numIds = this.getGeneIds().length;
         if ( numIds === 0 ) {
            this.tbarText.update( Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.StatusText.random );
         } else if ( numIds === 1 ) {
            this.tbarText.update( Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.StatusText.one );
         } else {
            this.tbarText.update( String.format(
               Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.StatusText.multiple, numIds ) );
         }
      }
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   updateFoundVsQueryText : function( foundCount, queryCount ) {
      if ( foundCount !== undefined && queryCount !== undefined && foundCount !== null && queryCount !== null ) {
         this.tbarText.update( String.format(
            Gemma.HelpText.WidgetDefaults.EEDetailsVisualizationWidget.StatusText.geneMatchCount, foundCount,
            queryCount ) );
      } else {
         this.tbarText.update( '' );
      }
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   clearHandler : function() {
      this.setGeneIds( [] );
      this.updateButtonText();
      this.geneCombo.reset();
      this.editBtn.disable();
      this.clearBtn.disable();
   },

   /**
    * @memberOf Gemma.VisualizationWidgetGeneSelectionToolbar
    */
   vizBtnHandler : function() {
      var geneList = this.getGeneIds();
      var eeId = this.eeId;
      var title = '';
      var downloadLink = '';
      if ( geneList.length > 0 ) {
         title = "Data for selected genes";
         downloadLink = String.format( Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}&g={1}", eeId, geneList.join( ',' ) );
      } else {
         geneList = [];
         title = "Data for a 'random' sampling of probes";
         downloadLink = String.format( Gemma.CONTEXT_PATH + "/dedv/downloadDEDV.html?ee={0}", eeId );
         this.editBtn.disable();
      }
      Ext.apply( this.visPanel, {
         downloadLink : downloadLink
      } );
      this.visPanel.loadFromParam( {
         params : [ [ eeId ], geneList ]
      } );
   }
} );
