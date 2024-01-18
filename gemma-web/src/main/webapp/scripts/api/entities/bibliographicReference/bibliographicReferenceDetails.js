Ext.namespace( 'Gemma.BibliographicReference' );

// Originally, this panel extended from Ext.FormPanel. It should not do so because we may put it inside
// another FormPanel, but FormPanel cannot be put inside another FormPanel. To be able to layout components
// in the same way as in FormPanel, we make it to have form layout by using the config:
// layout: 'form'
Gemma.BibliographicReference.DetailsPanel = Ext
   .extend(
      Ext.Panel,
      {
         layout : 'form',
         title : 'Bibliographic Reference Details',
         autoScroll : true, // Use overflow:'auto' to show scroll bars automatically
         collapseByDefault : false,

         defaults : {
            hidden : true,
            labelWidth : 120
         },

         // allows you to specify a bib ref to load
         loadFromId : function( id ) {
            BibliographicReferenceController.load( id, function( data ) {
               var rec = new Gemma.BibliographicReference.Record( data );
               this.updateFields( rec );
            }.createDelegate( this ) );
         },

         clear : function() {
            this.citation.hide();
            this.detailsFieldset.hide();
            this.tagsFieldset.hide();
         },

         /**
          * @memberOf Gemma.BibliographicReference.DetailsPanel
          */
         doUpdate : function( button, event ) {
            this.loadMask = new Ext.LoadMask( this.getEl() );

            var callParams = [];
            callParams.push( button.pmid );

            var successHandler = function( data ) {
               this.loadFromId( data.id );
               this.loadMask.hide();

            }.createDelegate( this );

            var errorHandler = function( data, e ) {
               this.loadMask.hide();
               Ext.Msg.alert( 'Error', e.message );
            }.createDelegate( this );

            callParams.push( {
               callback : successHandler,
               errorHandler : errorHandler
            } );

            BibliographicReferenceController.update.apply( this, callParams );
            this.loadMask.show();
         },

         initComponent : function() {

            var getPudmedAnchor = function( pubmedUrl ) {
               return (new Ext.Template( Gemma.Common.tpl.pubmedLink.simple )).apply( {
                  pubmedURL : pubmedUrl
               } );
            };

            Ext.apply( this, {
               // evidenceId is optional.
               updateFields : function( bibRefRecord, evidenceId ) {
                  this.citation.show();
                  this.detailsFieldset.show();
                  this.abstractBibli.setValue( bibRefRecord.get( 'abstractText' ) );
                  this.authors.setValue( bibRefRecord.get( 'authorList' ) );

                  if ( this.adminFieldset ) {
                     this.adminFieldset.show();
                     this.updateButton.pmid = bibRefRecord.get( 'pubAccession' );
                  }

                  if ( bibRefRecord.get( 'citation' ) ) {

                     if ( bibRefRecord.get( 'citation' ).retracted == true ) {
                        this.citation.setValue( '<font color=\'red\'><b>RETRACTED</b><br></font>'
                           + bibRefRecord.get( 'citation' ).citation + ' '
                           + getPudmedAnchor( bibRefRecord.get( 'citation' ).pubmedURL ) );
                     } else {
                        this.citation.setValue( bibRefRecord.get( 'citation' ).citation + ' '
                           + getPudmedAnchor( bibRefRecord.get( 'citation' ).pubmedURL ) );
                     }
                  }

                  var allExperiments = '';
                  var i;
                  var ee;
                  for ( i = 0; i < bibRefRecord.get( 'experiments' ).length; i++ ) {
                     ee = bibRefRecord.get( 'experiments' )[i];
                     allExperiments += '<a href="' + Gemma.LinkRoots.expressionExperimentPage + ee.id
                        + '" target="_blank" >' + ee.shortName + '</a>' + " : ";
                     allExperiments += ee.name + "<br />";
                  }
                  this.experiments.setValue( allExperiments );

                  var allMeshTerms = "";

                  if ( bibRefRecord.get( 'meshTerms' ) != null ) {
                     for ( i = 0; i < bibRefRecord.get( 'meshTerms' ).length; i++ ) {
                        allMeshTerms += bibRefRecord.get( 'meshTerms' )[i];

                        if ( i < bibRefRecord.get( 'meshTerms' ).length - 1 ) {
                           allMeshTerms += "; ";
                        }
                     }
                  }
                  this.pubmed.setValue( bibRefRecord.get( 'pubAccession' ) );
                  this.mesh.setValue( allMeshTerms );

                  var allChemicalsTerms = "";
                  if ( bibRefRecord.get( 'chemicalsTerms' ) != null ) {
                     for ( i = 0; i < bibRefRecord.get( 'chemicalsTerms' ).length; i++ ) {
                        allChemicalsTerms += bibRefRecord.get( 'chemicalsTerms' )[i];

                        if ( i < bibRefRecord.get( 'chemicalsTerms' ).length - 1 ) {
                           allChemicalsTerms += "; ";
                        }
                     }
                  }
                  this.chemicals.setValue( allChemicalsTerms );

                  this.tagsFieldset.setVisible( (allMeshTerms.length > 0 && allChemicalsTerms.length > 0) );


                  this.detailsFieldset.items.each( function( field ) {
                     if ( field.getValue() == "" ) {
                        field.hide();
                     } else {
                        field.show();
                     }
                  } );

               }
            } );

            Gemma.BibliographicReference.DetailsPanel.superclass.initComponent.call( this );

            this.citation = new Ext.form.DisplayField( {
               hideLabel : true
            } );

            this.abstractBibli = new Ext.form.TextArea( {
               anchor : '100%',
               grow : true,
               growMin : 1,
               growMax : 62,
               fieldLabel : 'Abstract',
               disabledClass : 'disabled-plain',
               disabled : true,
               boxMaxWidth : 600
            } );

            this.authors = new Ext.form.DisplayField( {
               fieldLabel : 'Authors'
            } );

            this.pubmed = new Ext.form.DisplayField( {
               fieldLabel : 'PubMed Id'
            } );

            this.mesh = new Ext.form.DisplayField( {
               fieldLabel : 'MeSH'
            } );

            this.chemicals = new Ext.form.DisplayField( {
               fieldLabel : 'Chemicals'
            } );

            this.detailsFieldset = new Ext.form.FieldSet( {
               defaults : {
                  labelStyle : 'padding-top: 1px;'
               },
               collapsed : this.collapseByDefault,
               cls : 'no-collapsed-border',
               anchor : '100%',
               title : 'Publication Details',
               collapsible : true,
               style : "margin-bottom: 3px;",
               items : [ this.abstractBibli, this.authors, this.pubmed ]
            } );

            this.experiments = new Ext.form.DisplayField( {
               fieldLabel : 'Experiments'
            } );


            this.tagsFieldset = new Ext.form.FieldSet( {
               collapsed : this.collapseByDefault,
               defaults : {
                  labelStyle : 'padding-top: 1px;'
               },
               cls : 'no-collapsed-border',
               anchor : '100%',
               title : 'Tags',
               collapsible : true,
               style : "margin-bottom: 3px;",
               items : [ this.mesh, this.chemicals ]
            } );

            this.add( this.citation, this.detailsFieldset, this.tagsFieldset );

            var isAdmin = Ext.get( "hasAdmin" ).getValue() === 'true';
            if ( isAdmin ) {
               this.updateButton = new Ext.Button( {
                  handler : this.doUpdate,
                  text : "Refresh from PubMed",
                  scope : this
               } );
               this.adminFieldset = new Ext.form.FieldSet( {
                  collapsed : this.collapseByDefault,
                  defaults : {
                     labelStyle : 'padding-top: 1px;'
                  },
                  cls : 'no-collapsed-border',
                  anchor : '100%',
                  title : 'Administration',
                  collapsible : true,
                  style : "margin-bottom: 3px;",
                  items : [ this.updateButton ]
               } );

               this.add( this.adminFieldset );

            }

            this.doLayout();
         } // initComponent
      } );
