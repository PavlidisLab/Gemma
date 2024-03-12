Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = ctxBasePath + '/images/default/s.gif';
/**
 * 
 * Panel containing the most interesting info about a gene group. Used as one tab of the EE page
 * 
 * pass in the gene group id obj as geneSetId
 * 
 * @class Gemma.GeneSetSummary
 * @extends Ext.Panel
 * 
 */
Gemma.GeneSetSummary = Ext
   .extend(
      Ext.Panel,
      {

         layout : 'vbox',
         layoutConfig : {
            align : 'stretch'
         },
         dirtyForm : false,
         listeners : {
            leavingTab : function() {
               if ( this.editModeOn && this.dirtyForm ) {
                  var leave = confirm( "You are still in edit mode. Your unsaved changes will be discarded when you switch tabs. Do you want to continue?" );
                  if ( leave ) {
                     return true;
                  }
                  return false;
               }
               return true;
            },
            tabChanged : function() {
               this.fireEvent( 'toggleEditMode', false );
            }
         },

         /**
          * @memberOf Gemma.GeneSetSummary
          */
         renderStatus : function( g ) {
            var statusString = "";
            if ( g.modifiable ) {
               statusString += "<img src='" + ctxBasePath + "/images/icons/shield.png' height='16' width='16' "
                  + "title='Protected; cannot have members changed, usually applies to automatically generated groups.' />&nbsp;";
            }
            var sl = Gemma.SecurityManager.getSecurityLink( "ubic.gemma.model.genome.gene.GeneSet", g.id,
               g.isPublic, g.isShared, g.userCanWrite, null, null, null, g.userOwned );

            statusString += sl;
            return statusString;
         },
         renderNumber : function( g ) {
            var numString = "" + g.size;
            return numString;
         },

         initComponent : function() {

            Gemma.GeneSetSummary.superclass.initComponent.call( this );

            var g = this.geneSet;

            if ( g.userCanWrite ) {
               this.editable = true;
            }
            var currentDescription = g.description;
            var currentName = g.name;

            save = function() {
               if ( !this.saveMask ) {
                  this.saveMask = new Ext.LoadMask( this.getEl(), {
                     msg : "Saving ..."
                  } );
               }
               this.saveMask.show();
               var description = descriptionArea.getValue();
               var name = nameArea.getValue();

               var entity = {
                  id : g.id,
                  description : description,
                  name : name
               };

               // returns ee details object
               GeneSetController.updateNameDesc( entity, function( data ) {

                  nameArea.setValue( data.name );
                  descriptionArea.setValue( data.description );

                  currentName = data.name;
                  currentDescription = data.description;

                  this.dirtyForm = false;
                  this.saveMask.hide();
               }.createDelegate( this ) );
            }.createDelegate( this );

            var descriptionArea = new Ext.form.TextArea( {
               allowBlank : true,
               resizable : true,
               readOnly : true,
               disabled : true,
               growMin : 1,
               growMax : 150,
               growAppend : '',
               grow : true,
               disabledClass : 'disabled-plain',
               fieldClass : '',
               emptyText : 'No description provided',
               enableKeyEvents : true,
               bubbleEvents : [ 'changeMade' ],
               listeners : {
                  'keyup' : function( field, e ) {
                     if ( field.isDirty() ) {
                        field.fireEvent( 'changeMade', field.isValid() );
                     }
                  },
                  'toggleEditMode' : function( editOn ) {
                     this.setReadOnly( !editOn );
                     this.setDisabled( !editOn );
                     if ( editOn ) {
                        this.removeClass( 'x-bare-field' );
                     } else {
                        this.addClass( 'x-bare-field' );
                     }
                  }
               },
               style : 'width: 100%; background-color: #f6f6f6;',
               value : currentDescription
            } );

            var nameArea = new Ext.form.TextArea( {
               allowBlank : false,
               grow : true,
               growMin : 1,
               growAppend : '',
               readOnly : true,// !this.editable,
               disabled : true,
               disabledClass : 'disabled-plain',
               emptyText : 'Name is required',
               enableKeyEvents : true,
               bubbleEvents : [ 'changeMade' ],
               listeners : {
                  'keyup' : function( field, e ) {
                     if ( field.isDirty() ) {
                        field.fireEvent( 'changeMade', field.isValid() );
                     }
                  },
                  'toggleEditMode' : function( editOn ) {
                     this.setReadOnly( !editOn );
                     this.setDisabled( !editOn );
                     if ( editOn ) {
                        this.removeClass( 'x-bare-field' );
                     } else {
                        this.addClass( 'x-bare-field' );
                     }
                  }
               },
               style : 'font-weight: bold; font-size:1.3em; width:100%',
               value : currentName
            } );

            resetEditableFields = function() {
               nameArea.setValue( currentName );
               descriptionArea.setValue( currentDescription );
               saveBtn.disable();
               cancelBtn.disable();
            };

            var editBtn = new Ext.Button( {
               // would like to use on/off slider or swtich type control here
               text : 'Start editing',
               editOn : false,
               disabled : !this.editable,
               handler : function( button, event ) {
                  this.fireEvent( 'toggleEditMode', true );
               },
               scope : this
            } );
            var cancelBtn = new Ext.Button( {
               text : 'Cancel',
               disabled : true,
               toolTip : 'Reset all fields to saved values',
               handler : function() {
                  this.fireEvent( 'toggleEditMode', false );
               },
               scope : this
            } );

            var saveBtn = new Ext.Button( {
               text : 'Save',
               disabled : true,
               handler : function() {
                  save();
                  this.fireEvent( 'toggleEditMode', false );
               },
               scope : this
            } );

            var deleteEEButton = new Ext.Button( {
               text : 'Delete Gene Group',
               icon : ctxBasePath + '/images/icons/cross.png',
               toolTip : 'Delete the gene group from the system',
               disabled : !this.editable,
               handler : this.deleteGeneSet,
               scope : this
            } );

            this.on( 'toggleEditMode', function( editOn ) {
               // is there a way to make this even propagate to all children automatically?
               this.editModeOn = editOn; // needed to warn user before tab change
               editBtn.setText( (editOn) ? 'Editing mode on' : 'Start editing' );
               editBtn.setDisabled( editOn );
               nameArea.fireEvent( 'toggleEditMode', editOn );
               descriptionArea.fireEvent( 'toggleEditMode', editOn );
               resetEditableFields();
               saveBtn.setDisabled( !editOn );
               cancelBtn.setDisabled( !editOn );
               if ( !editOn ) {
                  resetEditableFields();
                  this.dirtyForm = false;
               }
            } );

            this.on( 'changeMade', function( wasValid ) {
               // enable save button
               saveBtn.setDisabled( !wasValid );
               cancelBtn.setDisabled( !wasValid );
               this.dirtyForm = true;

            } );
            var basics = new Ext.Panel( {
               flex : 0,
               ref : 'fieldPanel',
               collapsible : false,
               bodyBorder : false,
               frame : false,
               baseCls : 'x-plain-panel',
               bodyStyle : 'padding:10px',
               defaults : {
                  bodyStyle : 'vertical-align:top;',
                  baseCls : 'x-plain-panel',
                  fieldClass : 'x-bare-field'
               },
               tbar : new Ext.Toolbar( {
                  hidden : !this.editable,
                  items : [ editBtn, ' ', saveBtn, ' ', cancelBtn, '-', deleteEEButton ]
               } ),
               items : [ nameArea, descriptionArea, {
                  layout : 'form',
                  defaults : {
                     border : false
                  },
                  items : [ {
                     fieldLabel : "Taxon",
                     html : g.taxonName
                  }, {
                     fieldLabel : 'Genes',
                     html : this.renderNumber( g )
                  }, {
                     fieldLabel : 'Status',
                     html : this.renderStatus( g )
                  } ]
               } ]
            } );

            this.add( basics );

            /* MEMBERS GRID */
            var geneMembersGrid = new Gemma.GeneMembersSaveGrid( {
               title : 'Gene Group Members',
               name : 'geneMembersGrid',
               frame : true,
               taxonId : g.taxonId,
               taxonName : g.taxonName,
               geneGroupId : g.id,
               selectedGeneSetValueObject : g,
               groupName : g.name,
               allowSaveToSession : false,
               allowRemovals : this.editable,
               allowAdditions : this.editable,
               sortableColumnsView : true,
               hideOkCancel : true,
               showSeparateSaveAs : true,
               enableSaveOnlyAfterModification : true,
               flex : 1
            } );

            geneMembersGrid.on( 'geneListSavedOver', function() {
               Ext.getBody().mask( 'Reloading set' );
               // could just update gene count, but this is easier for now since we'll probably change the tab layout
               // soon
               window.location.reload();
            } );

            geneMembersGrid.on( 'geneSetCreated', function( geneSet ) {
               Ext.getBody().mask( 'Loading new set' );
               window.location = ctxBasePath + "/geneSet/showGeneSet.html?id=" + geneSet.id;
            } );

            this.add( geneMembersGrid );

            /* EO member's grid */

            // adjust when user logs in or out
            Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {
               var appScope = this;
               GeneSetController.canCurrentUserEditGroup( this.geneSet.id, {
                  callback : function( editable ) {
                     appScope.adjustForIsEditable( editable );
                  },
                  scope : appScope
               } );

            }, this );
            Gemma.Application.currentUser.on( "logOut", function() {
               this.adjustForIsEditable( false );
            }, this );

            this.doLayout();
            this.fireEvent( "ready" );

         }, // end of initComponent
         adjustForIsEditable : function( editable ) {
            this.fieldPanel.getTopToolbar().setVisible( editable );
         },
         deleteGeneSet : function() {
            var id = this.geneSet.id;
            var redirectHome = true;
            Ext.Msg.show( {
               title : 'Delete ' + this.geneSet.name + '?',
               msg : 'Are you sure you want to delete gene group "' + this.geneSet.name + '"? This cannot be undone.',
               buttons : Ext.Msg.YESNO,
               fn : function( btn, text ) {
                  if ( btn === 'yes' ) {
                     var callParams = [];
                     callParams.push( [ {
                        id : id
                     } ] );
                     if ( !this.deleteMask ) {
                        this.deleteMask = new Ext.LoadMask( Ext.getBody(), {
                           msg : "Deleting ..."
                        } );
                     }
                     this.deleteMask.show();
                     callParams.push( {
                        callback : function( data ) {
                           window.location = ctxBasePath + '/home.html';
                        }.createDelegate( this ),
                        errorHandler : function( error ) {
                           Ext.Msg.alert( "Deletion failed", error );
                           this.deleteMask.hide();
                        }.createDelegate( this )
                     } );
                     GeneSetController.remove.apply( this, callParams );
                  }
               },
               scope : this,
               animEl : 'elId',
               icon : Ext.MessageBox.WARNING
            } );
         }
      } );
