Ext.namespace( 'Gemma', 'Gemma.PlatformDetails' );

/**
 * Need to set platformId as config.
 * <p>
 * Note: 'edit' functionality not implemented here.
 * 
 * @memberOf Gemma
 */
Gemma.PlatformDetails = Ext
   .extend(
      Ext.Panel,
      {

         padding : 10,
         defaults : {
            border : false,
            flex : 0
         },
         layoutConfig : {
            align : 'stretch'
         },
         layout : 'vbox',

         changeTab : function( tabName ) {
            this.fireEvent( 'changeTab', tabName );
         },

         renderTaxon : function( platformDetails ) {
            return new Ext.Panel(
               {
                  border : false,
                  html : platformDetails.taxon,
                  listeners : {
                     'afterrender' : function( c ) {
                        jQuery( '#taxonHelp' )
                           .qtip(
                              {
                                 content : "The primary taxon for sequences on this platform (i.e., what it was designed for), "
                                    + "and other taxa used as sequence sources",
                                 style : {
                                    name : 'cream'
                                 }
                              } );
                     }
                  }
               } );
         },

         /**
          * @memberOf Gemma.PlatformDetails
          */
         renderMerged : function( pd ) {
            var text = '';

            // FIXME this sucks.
            if ( pd.mergees != null && pd.mergees.length > 0 ) {
               for (var i = 0; i < pd.mergees.length; i++) {
                  var s = pd.mergees[i];
                  text = text + "Merges " + Gemma.arrayDesignLink( s );
                  var m = s.mergees;
                  if ( m != null && m.length > 0 ) {
                     text = text + " &#187; (Merges ";
                     for (var j = 0; j < m.length; j++) {
                        text = text + Gemma.arrayDesignLink( m[j] );
                        // Could keep recursing, but this is rare.
                     }
                     text = text + ")";

                  }
                  m = s.subsumees;
                  if ( m != null && m.length > 0 ) {
                     text = text + " &#187; (Subsumes ";
                     for (var j = 0; j < m.length; j++) {
                        text = text + Gemma.arrayDesignLink( m[j] );
                        // Could keep recursing, but this is rare.
                     }
                     text = text + ")";

                  }
                  text = text + "<br />";
               }
            } else if ( pd.merger != null ) {
               text = "Merged into " + Gemma.arrayDesignLink( pd.merger );
               var s = pd.merger;
               var m = s.mergees;
               if ( m != null && m.length > 0 ) {
                  text = text + " &#187; (Merges ";
                  for (var j = 0; j < m.length; j++) {
                     text = text + Gemma.arrayDesignLink( m[j] ) + "&nbsp;";
                     // Could keep recursing, but this is rare.
                  }
                  text = text + ")";
               }
               m = s.subsumees;
               if ( m != null && m.length > 0 ) {
                  text = text + " &#187; (Subsumes ";
                  for (var j = 0; j < m.length; j++) {
                     text = text + Gemma.arrayDesignLink( m[j] );
                     // Could keep recursing, but this is rare.
                  }
                  text = text + ")";

               }
               text = text + "<br />";
            } else if ( pd.subsumer != null ) {
               var s = pd.subsumer;
               text = "Subsumed by " + Gemma.arrayDesignLink( s );
            } else if ( pd.subsumees != null && pd.subsumees.size() > 0 ) {
               for (var i = 0; i < pd.subsumees.length; i++) {
                  var s = pd.subsumees[i];
                  text = text + "Subsumes " + Gemma.arrayDesignLink( subsumee );
                  m = s.subsumees;
                  if ( m != null && m.length > 0 ) {
                     text = text + " &#187; Subsumes ";
                     for (var j = 0; j < m.length; j++) {
                        text = text + Gemma.arrayDesignLink( m[j] );
                        // Could keep recursing, but this is rare.
                     }
                  }
                  text = text + "<br />";
               }
            } else {
               text = "None";
            }

            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : Gemma.helpTip( "#mergedHelp",
                     "Relationship this platform has with others, such as merging." )
               }
            } );
         },

         renderReport : function( platformDetails ) {

            var text = '';

            var updateT = '';
            var isAdmin = Ext.get( "hasAdmin" ).getValue() == 'true';
            if ( isAdmin ) {
               // FIXME does not refresh after report update.
               updateT = '&nbsp;<input type="button" value="Refresh report" onClick="updateArrayDesignReport('
                  + platformDetails.id + ')" />';
            }

            text = {
               tag : "ul",
               style : 'padding:8px;background:#DFDFDF;width:400px',
               children : [
                           {
                              tag : 'li',
                              html : 'Elements: ' + platformDetails.designElementCount,
                              style : platformDetails.dateCached == null ? 'visibility:hidden' : ''
                           },
                           {
                              tag : 'li',
                              html : 'With sequence: '
                                 + platformDetails.numProbeSequences
                                 + '&nbsp;<span style="font-size:smaller;color:grey">(Number of elements with sequences)</span>',
                              style : platformDetails.numProbeSequences == null
                                 || platformDetails.technologyType == "NONE" ? 'visibility:hidden' : ''
                           },
                           {
                              tag : 'li',
                              html : 'With alignments: '
                                 + platformDetails.numProbeAlignments
                                 + '&nbsp;<span style="font-size:smaller;color:grey">(Number of elements with at least one genome alignment)</span>',
                              style : platformDetails.numProbeAlignments == null
                                 || platformDetails.technologyType == "NONE" ? 'visibility:hidden' : ''
                           },
                           {
                              tag : 'li',
                              html : 'Mapped to genes: '
                                 + platformDetails.numProbesToGenes
                                 + '&nbsp;<span style="font-size:smaller;color:grey">(Number of elements mapped to at least one gene)</span>',
                              style : platformDetails.numProbesToGenes === null ? 'visibility:hidden' : ''
                           },
                           {
                              tag : 'li',
                              html : 'Unique genes: '
                                 + platformDetails.numGenes
                                 + '&nbsp;<span style="font-size:smaller;color:grey">(Number of distinct genes represented on the platform)</span>',
                              style : platformDetails.numGenes === null ? 'visibility:hidden' : ''
                           },
                           {
                              tag : 'li',
                              html : (platformDetails.dateCached != null ? 'As of: ' + platformDetails.dateCached
                                 + "&nbsp;" : 'No report available')
                                 + updateT
                           } ]
            };

            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : Gemma.helpTip( "#reportHelp",
                     "Platform elements in Gemma are mapped to genes either using sequence analysis (most array-based platforms)"
                        + " or directly (sequencing-based platforms and arrays for some model organisms)" )
               }
            } );
         },

         renderAnnotationFileLinks : function( platformDetails ) {
            var text = '';
            if ( platformDetails.noParentsAnnotationLink )
               text += '<a  ext:qtip="Recommended version for ermineJ" class="annotationLink" href="'
                  + platformDetails.noParentsAnnotationLink + '" >Basic</a>&nbsp;';
            if ( platformDetails.allParentsAnnotationLink )
               text += '<a class="annotationLink" href="' + platformDetails.allParentsAnnotationLink
                  + '" >All terms</a>&nbsp;';
            if ( platformDetails.bioProcessAnnotationLink )
               text += '<a ext:qtip="Biological process terms" class="annotationLink" href="'
                  + platformDetails.bioProcessAnnotationLink + '" >BP only</a>';

            return new Ext.Panel(
               {
                  border : false,
                  html : text,
                  listeners : {
                     'afterrender' : Gemma
                        .helpTip(
                           "#annotationHelp",
                           "Download annotation files for this platform. The files include gene information as well as "
                              + "GO terms and are compatible with ermineJ. "
                              + "Up to three versions fo GO annotations are provided. "
                              + "<p>The 'basic' version includes only directly annotated terms. "
                              + "<p>The 'All terms' version includes inferred terms based on propagation in the term hierarchy. "
                              + "<p>'BP only' includes only terms from the biological process ontology. "
                              + "<p>For ermineJ and uses where GO annotations are not needed, "
                              + "we recommend using the 'basic' one as it is the smallest and the other information can be inferred." )
                  }
               } );
         },

         renderExternalAccesions : function( platformDetails ) {

            var text = "";

            var er = platformDetails.externalReferences;
            if ( er == null || er.length == 0 ) {
               text = "None";
            } else {

               for (var i = 0; i < er.length; i++) {

                  var dbr = er[i];
                  var ac = dbr.accession;

                  var db = dbr.externalDatabase.name;

                  if ( db == "GEO" ) {
                     text = text + ac + "&nbsp;<a "
                        + " target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + ac
                        + "'><img  ext:qtip='NCBI page for this entry' src='/Gemma/images/logo/geoTiny.png' /></a>";
                  } else if ( db == "ArrayExpress" ) {
                     text = text
                        + ac
                        + "&nbsp;<a title='ArrayExpress page for this entry'"
                        + " target='_blank' href='http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession="
                        + ac
                        + "'><img  ext:qtip='NCBI page for this entry' src='/Gemma/images/logo/arrayExpressTiny.png' /></a>";

                  } else {
                     text = text + "&nbsp;" + ac + " (" + databaseEntry.getExternalDatabase().getName() + ")";
                  }

               }
            }

            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : Gemma.helpTip( "#sourcesHelp", "Identifiers in other systems, if any" )
               }
            } );
         },

         promptForAlternateName : function( id ) {
            var dialog = new Ext.Window( {
               title : "Enter a new alternate name",
               modal : true,
               layout : 'fit',
               autoHeight : true,
               width : 300,
               closeAction : 'hide',
               easing : 3,
               defaultType : 'textfield',
               items : [ {
                  id : "alternate-name-textfield",
                  fieldLabel : 'Name',
                  name : 'name',
                  listeners : {
                     afterrender : function( field ) {
                        field.focus();
                     }
                  }
               } ],

               buttons : [ {
                  text : 'Cancel',
                  handler : function() {
                     dialog.hide();
                  }
               }, {
                  text : 'Save',
                  handler : function() {
                     var name = Ext.get( "alternate-name-textfield" ).getValue();
                     this.addAlternateName( id, name );
                     dialog.hide();
                  },
                  scope : this
               } ]

            } );

            dialog.show();

         },

         addAlternateName : function( id, newName ) {

            var callParams = [];

            callParams.push( id, newName );

            var delegate = function( data ) {
               if ( Ext.get( "alternate-names" ) !== null ) {
                  Ext.DomHelper.overwrite( "alternate-names", data );
               }
            };
            var errorHandler = function( e, ex ) {
               Ext.Msg.alert( "Error", er + "\n" + exception.stack );
            };

            callParams.push( {
               callback : delegate,
               errorHandler : errorHandler
            } );

            ArrayDesignController.addAlternateName.apply( this, callParams );

         },

         renderExperimentLink : function( platformDetails ) {
            var text = platformDetails.expressionExperimentCount + "";
            if ( platformDetails.expressionExperimentCount > 0 ) {
               text += "&nbsp;<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='View the experiments tab'"
                  + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;experiments&#39;)'>";
            }
            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : Gemma.helpTip( "#experimentsHelp",
                     "How many experiments use this platform. Click the icon to access them" )
               }
            } );
         },

         renderDescription : function( description ) {
            return new Ext.Panel( {
               border : false,
               html : '<div class="clob">' + description + "</div>",
               listeners : {
                  'afterrender' : Gemma.helpTip( "#descriptionHelp",
                     "The description includes that obtained from the data provider (i.e., GEO)"
                        + " but may include additional information added by Gemma, such as "
                        + "information on samples remvoed due to overlap with other data sets." )
               }
            } );
         },

         renderElementsLink : function( platformDetails ) {
            var text = platformDetails.designElementCount;
            text += "&nbsp;<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='View the elements tab'"
               + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;elements&#39;)'>";
            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : Gemma.helpTip( "#numElementsHelp",
                     "How many elements (e.g. probes) the platform has. Click the icon to view in details" )
               }
            } );
         },

         renderAlternateNames : function( platformDetails ) {

            var text = '<span id="alternate-names">'
               + (platformDetails.alternateNames.length > 0 ? platformDetails.alternateNames : '') + '</span>';

            var isAdmin = Ext.get( "hasAdmin" ).getValue() == 'true';
            if ( isAdmin ) {
               text = text
                  + '&nbsp;<img  style="cursor:pointer" onClick="Ext.getCmp(&#39;'
                  + this.id
                  + '&#39;).promptForAlternateName('
                  + platformDetails.id
                  + ');return false;" ext:qtip="Add a new alternate name for this design" src="/Gemma/images/icons/add.png" />';
            }
            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : Gemma.helpTip( "#aliasHelp", "Other names used for this platform" )
               }
            } );
         },

         renderType : function( platformDetails ) {
            return new Ext.Panel(
               {
                  border : false,
                  html : platformDetails.colorString,
                  listeners : {
                     'afterrender' : Gemma
                        .helpTip(
                           "#typeHelp",
                           "Array platforms are one-color, two-color or dual mode if they can be used either way;"
                              + " generic platforms used for sequence-based methods (RNAseq) are listed as 'non-array-based'" )
                  }
               } );
         },

         initComponent : function() {
            Gemma.PlatformDetails.superclass.initComponent.call( this );

            // need to do this on render so we can show a load mask
            this
               .on(
                  'afterrender',
                  function() {
                     if ( !this.loadMask && this.getEl() ) {
                        this.loadMask = new Ext.LoadMask( this.getEl(), {
                           msg : Gemma.StatusText.Loading.generic,
                           msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                        } );
                     }
                     this.loadMask.show();

                     ArrayDesignController
                        .getDetails(
                           this.platformId,
                           {
                              callback : function( platformDetails ) {
                                 console.log( platformDetails );
                                 this.loadMask.hide();
                                 this
                                    .add( [
                                           {
                                              html : '<div style="font-weight: bold; font-size:1.2em;">'
                                                 + platformDetails.shortName + '<br />' + platformDetails.name
                                                 + '</div>'

                                           },
                                           {
                                              layout : 'form',
                                              labelWidth : 140,
                                              labelAlign : 'right',
                                              labelSeparator : ':',
                                              labelStyle : 'font-weight:bold;',
                                              flex : 1,
                                              defaults : {
                                                 border : false
                                              },
                                              // have an ArrayDesignValueObjectExt
                                              items : [
                                                       // Validated / troubled?
                                                       {
                                                          fieldLabel : "Aliases "
                                                             + '&nbsp;<i id="aliasHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderAlternateNames( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Taxon'
                                                             + '&nbsp<i id="taxonHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderTaxon( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Number of elements'
                                                             + '&nbsp<i id="numElementsHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderElementsLink( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Description'
                                                             + '&nbsp<i id="descriptionHelp" class="fa fa-question-circle fa-fw " style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderDescription( platformDetails.description )
                                                       },
                                                       {
                                                          fieldLabel : 'Experiments'
                                                             + '&nbsp<i id="experimentsHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderExperimentLink( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Platform type'
                                                             + '&nbsp<i id="typeHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderType( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Annotation files'
                                                             + '&nbsp<i id="annotationHelp"  class="fa fa-question-circle fa-fw-circle" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderAnnotationFileLinks( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Sources'
                                                             + '&nbsp<i id="sourcesHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderExternalAccesions( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Relationships'
                                                             + '&nbsp<i id="mergedHelp"  class="fa fa-question-circle fa-fw " style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderMerged( platformDetails )
                                                       },
                                                       {
                                                          fieldLabel : 'Gene map summary'
                                                             + '&nbsp<i id="reportHelp"  class="fa fa-question-circle fa-fw " style="font-size:smaller;color:grey"></i>',
                                                          items : this.renderReport( platformDetails )
                                                       } ]
                                           /*
                                              * Edit button?
                                              */
                                           } ] );
                                 this.syncSize();
                              }.createDelegate( this ),
                              errorHandler : function( er, exception ) {
                                 Ext.Msg.alert( "Error", er + "\n" + exception.stack );
                                 console.log( exception.stack );
                              }
                           } );
                  } );
         }

      } );
