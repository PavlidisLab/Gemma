Ext.namespace( 'Gemma' );
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
/**
 * 
 * Panel containing the most interesting info about an experiment. Used as one tab of the EE page
 * 
 * pass in the ee details obj as experimentDetails
 * 
 * @class Gemma.ExpressionExperimentDetails
 * @extends Ext.Panel
 * 
 */
Gemma.ExpressionExperimentDetails = Ext
   .extend(
      Ext.Panel,
      {

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
          * @memberOf Gemma.ExpressionExperimentDetails
          */
         renderArrayDesigns : function( arrayDesigns ) {
            var result = '';
            for (var i = 0; i < arrayDesigns.length; i++) {
               var ad = arrayDesigns[i];
               result = result + '<a href="/Gemma/arrays/showArrayDesign.html?id=' + ad.id + '">' + ad.shortName
                  + '</a> - ' + ad.name;

               if ( arrayDesigns[i].troubled ) {
                  result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ext:qtip="'
                     + arrayDesigns[i].troubleDetails + '"/>';
               }
               if ( i < arrayDesigns.length - 1 ) {
                  result = result + "<br/>";
               }
            }
            return result;
         },
         renderCoExpressionLinkCount : function( ee ) {

            if ( ee.coexpressionLinkCount === null ) {
               return "Unavailable"; // analysis not run.
            }

            var downloadCoExpressionDataLink = String
               .format(
                  "<span style='cursor:pointer'  ext:qtip='Download all coexpression  data in a tab delimited format'  "
                     + "onClick='fetchCoExpressionData({0})' > &nbsp; <img src='/Gemma/images/download.gif'/> &nbsp; </span>",
                  ee.id );
            var count;

            return ee.coexpressionLinkCount + "&nbsp;" + downloadCoExpressionDataLink;

         },

         /**
          * 
          */
         renderSourceDatabaseEntry : function( ee ) {
            var result = '';

            var logo = '';
            if ( ee.externalDatabase == 'GEO' ) {
               var acc = ee.accession;
               acc = acc.replace( /\.[1-9]$/, '' ); // in case of multi-species.
               logo = '/Gemma/images/logo/geoTiny.png';
               result = '<a target="_blank" href="http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=' + acc
                  + '"><img src="' + logo + '"/></a>';

            } else if ( ee.externalDatabase == 'ArrayExpress' ) {
               logo = '/Gemma/images/logo/arrayExpressTiny.png';
               result = '<a target="_blank" href="http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession='
                  + ee.accession + '"><img src="' + logo + '"/></a>';
            } else {
               result = "Direct upload";
            }

            return result;

         },

         /**
          * Link for samples details page.
          * 
          * @param {}
          *           ee
          * @return {}
          */
         renderSamples : function( ee ) {
            var result = ee.bioAssayCount;
            if ( this.editable ) {
               result = result
                  + '&nbsp;&nbsp<a href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id='
                  + ee.id
                  + '"><img ext:qtip="View the details of the samples" src="/Gemma/images/icons/magnifier.png"/></a>';
            }
            return '' + result; // hack for possible problem with extjs 3.1 - bare
            // number not displayed, coerce to string.
         },

         renderStatus : function( ee ) {

            var result = '';
            if ( ee.validated ) {
               result = result
                  + '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" title="validated"/>';
            }

            if ( ee.troubled ) {
               result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ext:qtip="trouble: '
                  + ee.troubleDetails + '"/>';
            }

            if ( ee.hasMultiplePreferredQuantitationTypes ) {
               result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ' + 'title="'
                  + Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.statusMultiplePreferredQuantitationTypes
                  + '"/>';
            }

            if ( ee.hasMultipleTechnologyTypes ) {
               result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" ' + 'title="'
                  + Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.statusMultipleTechnologyTypes + '"/>';
            }

            var isUserLoggedIn = (Ext.getDom( 'hasUser' ) && Ext.getDom( 'hasUser' ).getValue() === 'true') ? true
               : false;

            if ( isUserLoggedIn ) {
               result = result
                  + Gemma.SecurityManager.getSecurityLink(
                     'ubic.gemma.model.expression.experiment.ExpressionExperimentImpl', ee.id, ee.isPublic,
                     ee.isShared, ee.userCanWrite, null, null, null, ee.userOwned );
            }

            return result || "No flags";

         },

         linkAnalysisRenderer : function( ee ) {
            var id = ee.id;
            var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
               + id
               + '-eemanager\').doLinks('
               + id
               + ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></span>';
            if ( ee.dateLinkAnalysis ) {
               var type = ee.linkAnalysisEventType;
               var color = "#000";
               var suggestRun = true;
               var qtip = 'ext:qtip="OK"';
               if ( type == 'FailedLinkAnalysisEventImpl' ) {
                  color = 'red';
                  qtip = 'ext:qtip="Failed"';
               } else if ( type == 'TooSmallDatasetLinkAnalysisEventImpl' ) {
                  color = '#CCC';
                  qtip = 'ext:qtip="Too small"';
                  suggestRun = false;
               }

               return '<span style="color:' + color + ';" ' + qtip + '>'
                  + Ext.util.Format.date( ee.dateLinkAnalysis, 'y/M/d' ) + '&nbsp;' + (suggestRun ? runurl : '');
            } else {
               return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }

         },

         missingValueAnalysisRenderer : function( ee ) {
            var id = ee.id;
            var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
               + id
               + '-eemanager\').doMissingValues('
               + id
               + ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></span>';

            /*
             * Offer missing value analysis if it's possible (this might need tweaking).
             */
            if ( ee.technologyType != 'ONECOLOR' && ee.technologyType != 'NONE' && ee.hasEitherIntensity ) {

               if ( ee.dateMissingValueAnalysis ) {
                  var type = ee.missingValueAnalysisEventType;
                  var color = "#000";
                  var suggestRun = true;
                  var qtip = 'ext:qtip="OK"';
                  if ( type == 'FailedMissingValueAnalysisEventImpl' ) {
                     color = 'red';
                     qtip = 'ext:qtip="Failed"';
                  }

                  return '<span style="color:' + color + ';" ' + qtip + '>'
                     + Ext.util.Format.date( ee.dateMissingValueAnalysis, 'y/M/d' ) + '&nbsp;'
                     + (suggestRun ? runurl : '');
               } else {
                  return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
               }

            } else {
               return '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>';
            }
         },

         processedVectorCreateRenderer : function( ee ) {
            var id = ee.id;
            var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
               + id
               + '-eemanager\').doProcessedVectors('
               + id
               + ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></span>';

            if ( ee.dateProcessedDataVectorComputation ) {
               var type = ee.processedDataVectorComputationEventType;
               var color = "#000";

               var suggestRun = true;
               var qtip = 'ext:qtip="OK"';
               if ( type == 'FailedProcessedVectorComputationEventImpl' ) { // note:
                  // no
                  // such
                  // thing.
                  color = 'red';
                  qtip = 'ext:qtip="Failed"';
               }

               return '<span style="color:' + color + ';" ' + qtip + '>'
                  + Ext.util.Format.date( ee.dateProcessedDataVectorComputation, 'y/M/d' ) + '&nbsp;'
                  + (suggestRun ? runurl : '');
            } else {
               return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
         },

         // FIXME is this used?
         differentialAnalysisRenderer : function( ee ) {
            var id = ee.id;
            var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\''
               + panelId
               + 'eemanager\').doDifferential('
               + id
               + ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></span>';

            if ( ee.numPopulatedFactors > 0 ) {
               if ( ee.dateDifferentialAnalysis ) {
                  var type = ee.differentialAnalysisEventType;

                  var color = "#000";
                  var suggestRun = true;
                  var qtip = 'ext:qtip="OK"';
                  if ( type == 'FailedDifferentialExpressionAnalysisEventImpl' ) {
                     color = 'red';
                     qtip = 'ext:qtip="Failed"';
                  } else if ( record.get( 'differentialExpressionAnalyses' ).length == 0 ) {
                     // we ran it, but the analyses were apparently deleted.
                     return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
                  }

                  return '<span style="color:' + color + ';" ' + qtip + '>'
                     + Ext.util.Format.date( ee.dateDifferentialAnalysis, 'y/M/d' ) + '&nbsp;'
                     + (suggestRun ? runurl : '');
               } else {
                  return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
               }
            } else {
               return '<span style="color:#CCF;">NA</span>';
            }
         },
         renderProcessedExpressionVectorCount : function( e ) {
            return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
         },
         renderEESets : function( eeSets ) {
            eeSets.sort( function( a, b ) {
               var A = a.name.toLowerCase();
               var B = b.name.toLowerCase();
               if ( A < B )
                  return -1;
               if ( A > B )
                  return 1;
               return 0;
            } );
            var eeSetLinks = [];
            var i;
            for (i = 0; i < eeSets.length; i++) {
               if ( eeSets[i] && eeSets[i].name && eeSets[i].id ) {
                  eeSetLinks
                     .push( ' <a target="_blank" href="/Gemma/expressionExperimentSet/showExpressionExperimentSet.html?id='
                        + eeSets[i].id + '">' + eeSets[i].name + '</a>' );
               }
            }
            if ( eeSetLinks.length === 0 ) {
               eeSetLinks.push( 'Not currently a member of any experiment group' );
            }
            return eeSetLinks;
         },

         initComponent : function() {

            this.panelId = this.getId();
            Gemma.ExpressionExperimentDetails.superclass.initComponent.call( this );

            // this.editable && this.admin may also have been set in component configs

            var panelId = this.getId();
            var e = this.experimentDetails;
            var currentDescription = e.description;
            var currentName = e.name;
            var currentShortName = e.shortName;
            var currentPubMedId = (e.primaryCitation) ? e.primaryCitation.pubmedAccession : '';
            var currentPrimaryCitation = e.primaryCitation;
            var manager = new Gemma.EEManager( {
               editable : this.editable,
               id : e.id + '-eemanager'
            } );
            this.manager = manager;

            /* PUB MED REGION */

            var pubMedDisplay = new Ext.Panel( {
               xtype : 'panel',
               fieldLabel : 'Publication',
               baseCls : 'x-plain-panel',
               style : 'padding-top:5px',
               tpl : new Ext.XTemplate( Gemma.Common.tpl.pubmedLink.complex ),
               data : {
                  pubAvailable : (currentPrimaryCitation) ? 'true' : 'false',
                  primaryCitationStr : (currentPrimaryCitation) ? currentPrimaryCitation.citation : '',
                  pubmedURL : (currentPrimaryCitation) ? currentPrimaryCitation.pubmedURL : ''
               },
               listeners : {
                  'toggleEditMode' : function( editOn ) {
                     this.setVisible( !editOn );
                  }
               }
            } );
            var pubMedIdField = new Ext.form.NumberField( {
               xtype : 'numberfield',
               allowDecimals : false,
               minLength : 1,
               maxLength : 9,
               allowNegative : false,
               emptyText : (this.isAdmin || this.editable) ? 'Enter pubmed id' : 'Not Available',
               width : 100,
               value : currentPubMedId,
               enableKeyEvents : true,
               bubbleEvents : [ 'changeMade' ],
               listeners : {
                  'keyup' : function( field, event ) {
                     if ( field.isDirty() ) {
                        field.fireEvent( 'changeMade', field.isValid() );
                     }
                  },
                  scope : this

               }
            } );
            var pubMedDelete = {
               xtype : 'button',
               text : 'Clear',
               icon : '/Gemma/images/icons/cross.png',
               tooltip : 'Remove this experiment\'s association with this publication',
               bubbleEvents : [ 'changeMade' ],
               handler : function() {
                  pubMedIdField.setValue( '' );
                  field.fireEvent( 'changeMade', true );
               },
               scope : this
            };
            var pubMedForm = new Ext.Panel( {
               fieldLabel : 'Publication',
               xtype : 'panel',
               layout : 'hbox',
               hidden : true,// hide until edit mode is activated
               padding : 3,
               items : [ pubMedIdField, pubMedDelete ],
               listeners : {
                  'toggleEditMode' : function( editOn ) {
                     this.setVisible( editOn );
                     this.doLayout();
                  }
               }
            } );

            /*
             * This is needed to make the annotator initialize properly.
             */
            new Gemma.CategoryCombo( {} );

            var taggerurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'' + e.id
               + '-eemanager\').tagger(' + e.id + ',' + e.taxonId + ',' + this.editable + ','
               + (e.validatedAnnotations !== null)
               + ')"><img src="/Gemma/images/icons/pencil.png" alt="view tags" title="view tags"/></span>';

            tagView = new Gemma.AnnotationDataView( {
               readParams : [ {
                  id : e.id,
                  classDelegatingFor : "ExpressionExperimentImpl"
               } ]
            } );

            manager.on( 'tagsUpdated', function() {
               tagView.store.reload();
            } );

            manager.on( 'done', function() {
               /*
                * After a process that requires refreshing the page.
                */
               window.location.reload();
            } );

            manager.on( 'reportUpdated', function( data ) {
               ob = data[0];
               var k = Ext.get( 'coexpressionLinkCount-region' );
               Ext.DomHelper.overwrite( k, {
                  html : ob.coexpressionLinkCount
               } );
               k.highlight();
               k = Ext.get( 'processedExpressionVectorCount-region' );
               Ext.DomHelper.overwrite( k, {
                  html : ob.processedExpressionVectorCount
               } );
               k.highlight();
            }, this );

            manager.on( 'differential', function() {
               window.location.reload( true );
            } );

            save = function() {
               if ( !this.saveMask ) {
                  this.saveMask = new Ext.LoadMask( this.getEl(), {
                     msg : "Saving ..."
                  } );
               }
               this.saveMask.show();
               var shortName = shortNameField.getValue();
               var description = descriptionArea.getValue();
               var name = nameArea.getValue();
               var newPubMedId = pubMedIdField.getValue();

               var entity = {
                  entityId : e.id
               };

               if ( shortName != currentShortName ) {
                  entity.shortName = shortName;
               }

               if ( description != currentDescription ) {
                  entity.description = description;
               }

               if ( name != currentName ) {
                  entity.name = name;
               }

               if ( !newPubMedId ) {
                  entity.pubMedId = currentPubMedId;
                  entity.removePrimaryPublication = true;
               } else if ( newPubMedId !== currentPubMedId ) {
                  entity.pubMedId = newPubMedId;
                  entity.removePrimaryPublication = false;
               } else {
                  entity.removePrimaryPublication = false;
               }
               // returns ee details object
               ExpressionExperimentController.updateBasics( entity, function( data ) {

                  shortNameField.setValue( data.shortName );
                  nameArea.setValue( data.name );
                  descriptionArea.setValue( data.description );
                  pubMedIdField.setValue( data.pubmedId );
                  pubMedDisplay.update( {
                     pubAvailable : (data.pubmedId) ? 'true' : 'false',
                     primaryCitation : (data.primaryCitation) ? data.primaryCitation.citation : '',
                     pubmedURL : (data.primaryCitation) ? data.primaryCitation.pubmedURL : ''
                  } );

                  currentShortName = data.shortName;
                  currentName = data.name;
                  currentDescription = data.description;
                  currentPubMedId = (data.primaryCitation) ? data.primaryCitation.pubmedAccession : '';
                  currentPrimaryCitation = data.primaryCitation;

                  this.dirtyForm = false;
                  this.saveMask.hide();

               }.createDelegate( this ) );

            }.createDelegate( this );

            var descriptionArea = new Ext.form.TextArea( {
               allowBlank : true,
               resizable : true,
               readOnly : true,
               disabled : false,
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
                     if ( editOn ) {
                        this.removeClass( 'x-bare-field' );
                     } else {
                        this.addClass( 'x-bare-field' );
                     }
                  }
               },
               style : 'width: 100%; background-color: #fcfcfc; border: 1px solid #cccccc;',
               value : currentDescription
            } );

            var shortNameField = new Ext.form.TextField( {
               enableKeyEvents : true,
               allowBlank : false,
               grow : true,
               disabledClass : 'disabled-plain',
               readOnly : true,
               // disabled: true,
               style : 'font-weight: bold; font-size:1.4em; height:1.5em; color:black',
               bubbleEvents : [ 'changeMade' ],
               listeners : {
                  'keyup' : function( field, e ) {
                     if ( field.isDirty() ) {
                        field.fireEvent( 'changeMade', field.isValid() );
                     }
                  },
                  'toggleEditMode' : function( editOn ) {
                     this.setReadOnly( !editOn );
                     // this.setDisabled(!editOn);
                     if ( editOn ) {
                        this.removeClass( 'x-bare-field' );
                     } else {
                        this.addClass( 'x-bare-field' );
                     }
                  }
               },
               value : currentShortName
            } );

            var nameArea = new Ext.form.TextArea( {
               allowBlank : false,
               grow : true,
               // growMin: 22,
               growAppend : '',
               readOnly : true,// !this.editable,
               cls : 'disabled-plain',
               emptyText : 'No description provided',
               enableKeyEvents : true,
               bubbleEvents : [ 'changeMade' ],
               editOn : false,
               listeners : {
                  'keyup' : function( field, e ) {
                     if ( field.isDirty() ) {
                        field.fireEvent( 'changeMade', field.isValid() );
                     }
                  },
                  'focus' : function( field ) {
                     if ( !field.editOn ) {
                        this.removeClass( 'x-form-focus' );
                     }
                  },
                  'toggleEditMode' : function( editOn ) {
                     this.setReadOnly( !editOn );
                     this.editOn = editOn;
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
               shortNameField.setValue( currentShortName );
               nameArea.setValue( currentName );
               descriptionArea.setValue( currentDescription );
               pubMedIdField.setValue( currentPubMedId );
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
            var editEEButton = new Ext.Button( {
               text : 'More edit options',
               icon : '/Gemma/images/icons/wrench.png',
               toolTip : 'Go to editor page for this experiment',
               disabled : !this.editable,
               handler : function() {
                  window.open( '/Gemma/expressionExperiment/editExpressionExperiment.html?id='
                     + this.experimentDetails.id );
               },
               scope : this
            } );
            var deleteEEButton = new Ext.Button( {
               text : 'Delete Experiment',
               icon : '/Gemma/images/icons/cross.png',
               toolTip : 'Delete the experiment from the system',
               disabled : !this.editable,
               handler : function() {
                  manager.deleteExperiment( this.experimentDetails.id, true );
               },
               scope : this
            } );

            this.on( 'toggleEditMode', function( editOn ) {
               // is there a way to make this even propagate to all children automatically?
               this.editModeOn = editOn; // needed to warn user before tab change
               editBtn.setText( (editOn) ? 'Editing mode on' : 'Start editing' );
               editBtn.setDisabled( editOn );
               nameArea.fireEvent( 'toggleEditMode', editOn );
               descriptionArea.fireEvent( 'toggleEditMode', editOn );
               shortNameField.fireEvent( 'toggleEditMode', editOn );
               pubMedForm.fireEvent( 'toggleEditMode', editOn );
               pubMedDisplay.fireEvent( 'toggleEditMode', editOn );
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
            var basics = new Ext.Panel(
               {
                  ref : 'fieldPanel',
                  collapsible : false,
                  bodyBorder : false,
                  frame : false,
                  baseCls : 'x-plain-panel',
                  bodyStyle : 'padding:10px',
                  defaults : {
                     bodyStyle : 'vertical-align:top;font-size:12px;color:black',
                     baseCls : 'x-plain-panel',
                     fieldClass : 'x-bare-field'
                  },
                  tbar : new Ext.Toolbar( {
                     hidden : !this.editable,
                     items : [ editBtn, ' ', saveBtn, ' ', cancelBtn, '-', editEEButton, '-', deleteEEButton ]
                  } ),
                  items : [
                           shortNameField,
                           nameArea,
                           {
                              layout : 'form',
                              defaults : {
                                 border : false
                              },
                              items : [
                                       {
                                          fieldLabel : "Taxon",
                                          html : e.taxon
                                       },
                                       {
                                          fieldLabel : 'Tags&nbsp;' + taggerurl,
                                          items : [ tagView ]
                                       },
                                       {
                                          fieldLabel : 'Experiment Groups',
                                          html : this.renderEESets( e.expressionExperimentSets ).join( ',' )
                                       },
                                       {
                                          fieldLabel : 'Samples',
                                          html : this.renderSamples( e ),
                                          width : 60
                                       },
                                       {
                                          fieldLabel : 'Profiles',
                                          // id: 'processedExpressionVectorCount-region',
                                          html : '<div id="downloads"> '
                                             + this.renderProcessedExpressionVectorCount( e )
                                             + '&nbsp;&nbsp;'
                                             + '<i>Downloads:</i> &nbsp;&nbsp; <span class="link"  ext:qtip="Download the tab delimited data" onClick="fetchData(true,'
                                             + e.id
                                             + ', \'text\', null, null)">Filtered</span> &nbsp;&nbsp;'
                                             + '<span class="link" ext:qtip="Download the tab delimited data" onClick="fetchData(false,'
                                             + e.id
                                             + ', \'text\', null, null)">Unfiltered</span> &nbsp;&nbsp;'
                                             + '<a class="helpLink" href="?" onclick="showHelpTip(event, \''
                                             + Gemma.HelpText.WidgetDefaults.ExpressionExperimentDetails.profileDownloadTT
                                             + '\');' + ' return false"> <img src="/Gemma/images/help.png" /> </a>'
                                             + '</div>',
                                          width : 400
                                       }, {
                                          fieldLabel : 'Platforms',
                                          html : this.renderArrayDesigns( e.arrayDesigns ),
                                          width : 480
                                       }, {
                                          fieldLabel : 'Coexpr. Links',
                                          html : this.renderCoExpressionLinkCount( e ),
                                          width : 80
                                       }, {
                                          fieldLabel : 'Differential Expr. Analyses',
                                          items : new Gemma.DifferentialExpressionAnalysesSummaryTree( {
                                             experimentDetails : e,
                                             editable : this.editable,
                                             listeners : {
                                                'analysisDeleted' : function() {
                                                   this.fireEvent( 'experimentDetailsReloadRequired' );
                                                },
                                                scope : this
                                             }
                                          } )
                                       }, {
                                          fieldLabel : 'Status',
                                          html : this.renderStatus( e )
                                       } ]
                           },
                           descriptionArea,
                           {
                              layout : 'form',
                              defaults : {
                                 border : false
                              },
                              items : [
                                       pubMedDisplay,
                                       pubMedForm,
                                       {
                                          fieldLabel : 'Created',
                                          html : Ext.util.Format.date( e.dateCreated ) + ' from '
                                             + this.renderSourceDatabaseEntry( e )
                                       },
                                       {
                                          html : 'The last time a platform associated with this experiment was updated: '
                                             + e.lastArrayDesignUpdateDate,
                                          hidden : !e.lastArrayDesignUpdateDate
                                       } ]
                           } ]
               } );

            this.add( basics );

            // adjust when user logs in or out
            Gemma.Application.currentUser.on( "logIn", function( userName, isAdmin ) {
               var appScope = this;
               ExpressionExperimentController.canCurrentUserEditExperiment( this.experimentDetails.id, {
                  callback : function( editable ) {
                     // console.log(this);
                     appScope.adjustForIsEditable( editable );
                  },
                  scope : appScope
               } );

            }, this );
            Gemma.Application.currentUser.on( "logOut", function() {
               this.adjustForIsEditable( false );
               // TODO reset widget if experiment is private!
            }, this );

            this.doLayout();
            this.fireEvent( "ready" );

         }, // end of initComponent
         adjustForIsEditable : function( editable ) {
            this.fieldPanel.getTopToolbar().setVisible( editable );
         }
      } );
