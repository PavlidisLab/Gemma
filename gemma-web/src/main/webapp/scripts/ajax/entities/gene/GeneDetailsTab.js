/**
 * $Id$
 */
Ext.namespace( 'Gemma' );

/**
 * need to set geneId as config
 */
Gemma.GeneDetails = Ext
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
         renderHomologues : function( homologues, mainGeneSymbol ) {
            var homologueStr = '';
            if ( homologues && homologues.length > 0 ) {
               homologues.sort( function( a, b ) {
                  var A = a.taxonCommonName.toLowerCase();
                  var B = b.taxonCommonName.toLowerCase();
                  if ( A < B )
                     return -1;
                  if ( A > B )
                     return 1;
                  return 0;
               } );

               var j, homologue;
               for (j = 0; j < homologues.length; j++) {
                  homologue = homologues[j];
                  homologueStr += "<a title=\"View this homologous gene in Gemma\" href=\"/Gemma/gene/showGene.html?id="
                     + homologue.id
                     + "\">"
                     + homologue.officialSymbol
                     + "&nbsp;["
                     + homologue.taxonCommonName
                     + "]</a>&nbsp;&nbsp;&nbsp;";
               }
            }

            homologueStr = "None defined"; // or if not available...

            return homologueStr;
         },

         renderGeneSets : function( geneSets ) {
            var geneSetLinks = [];
            if ( geneSets != null && geneSets.length > 0 ) {
               geneSets.sort( function( a, b ) {
                  var A = a.name.toLowerCase();
                  var B = b.name.toLowerCase();
                  if ( A < B )
                     return -1;
                  if ( A > B )
                     return 1;
                  return 0;
               } );

               for (var i = 0; i < geneSets.length; i++) {
                  if ( geneSets[i] && geneSets[i].name && geneSets[i].id ) {
                     geneSetLinks.push( '<a target="_blank" href="/Gemma/geneSet/showGeneSet.html?id=' + geneSets[i].id
                        + '">' + geneSets[i].name + '</a>' );
                  }
               }
            } else {
               geneSetLinks.push( 'Not currently a member of any gene group' );
            }
            return geneSetLinks;
         },

         /**
          * 
          * @param geneDetails
          * @returns {String}
          */
         renderMultifunctionality : function( geneDetails ) {
            var text;
            if ( geneDetails.multifunctionalityRank ) {
               text = geneDetails.numGoTerms + " GO Terms; Overall multifunctionality "
                  + geneDetails.multifunctionalityRank.toFixed( 2 );
               text += "&nbsp;<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='View the GO term tab'"
                  + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;goGrid&#39;)'>";
            } else {
               text = "[ Not available ]";
            }

            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : function( c ) {
                     jQuery( '#multifuncHelp' ).qtip( {
                        content : Gemma.HelpText.WidgetDefaults.GeneDetails.multifuncTT,
                        style : {
                           name : 'cream'
                        }
                     } );
                  }
               }
            } );
         },

         /**
          * 
          * @param geneDetails
          * @returns {String}
          */
         renderPhenotypes : function( geneDetails ) {
            var text;
            if ( geneDetails.phenotypes && geneDetails.phenotypes.length > 0 ) {
               var phenotypes = geneDetails.phenotypes;
               phenotypes.sort( function( a, b ) {
                  var A = a.value.toLowerCase();
                  var B = b.value.toLowerCase();
                  if ( A < B )
                     return -1;
                  if ( A > B )
                     return 1;
                  return 0;
               } );
               var i = 0;
               var text = '';
               var limit = Math.min( 3, phenotypes.length );
               for (i = 0; i < limit; i++) {
                  text += '<a target="_blank" ext:qtip="View all genes for this phenotype" href="'
                     + Gemma.LinkRoots.phenotypePage + phenotypes[i].urlId + '">' + phenotypes[i].value + '</a>';
                  if ( (i + 1) !== limit ) {
                     text += ', ';
                  }
               }
               if ( limit < phenotypes.length ) {
                  text += ', ' + (phenotypes.length - limit) + ' more';
               }
               text += "&nbsp;<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='View the phenotype tab'"
                  + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;phenotypes&#39;)'>";

            } else {
               text = "[ None ]";
            }

            return new Ext.Panel( {
               border : false,
               html : text,
               listeners : {
                  'afterrender' : function( c ) {
                     jQuery( '#phenotypeHelp' ).qtip( {
                        content : Gemma.HelpText.WidgetDefaults.GeneDetails.phenotypeTT,
                        style : {
                           name : 'cream'
                        }
                     } );
                  }
               }
            } );

         },

         changeTab : function( tabName ) {
            this.fireEvent( 'changeTab', tabName );
         },
         /**
          * 
          * @param ncbiId
          * @param count
          * @returns {String}
          */
         renderAssociatedExperiments : function( ncbiId, count ) {
            return new Ext.Panel( {
               border : false,
               html : (count > 0 ? '<a href="/Gemma/searcher.html?query=http://purl.org/commons/record/ncbi_gene/'
                  + ncbiId + '&scope=E">' + count + '</a>' : "No studies known to be about this gene"),
               listeners : {
                  'afterrender' : function( c ) {
                     jQuery( "#studiesHelp" ).qtip( {
                        content : Gemma.HelpText.WidgetDefaults.GeneDetails.assocExpTT,
                        style : {
                           name : 'cream'
                        }
                     } );
                  }
               }
            } );
         },

         renderNodeDegree : function( geneDetails ) {
            if ( geneDetails.nodeDegrees && geneDetails.nodeDegrees.length > 1 ) {
               // Note: we need a panel here so we can pick up the rendering event so jquery can do its work.
               return new Ext.Panel(
                  {
                     border : false,
                     html : '<span id="nodeDegreeSpark">...</span> Max support '
                        + (geneDetails.nodeDegrees.length - 1)
                        + "&nbsp;<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='View the coexpression tab'"
                        + "onClick='Ext.getCmp(&#39;" + this.id + "&#39;).changeTab(&#39;coex&#39;)'>",
                     listeners : {
                        'afterrender' : function( c ) {

                           /*
                            * Compute cumulative counts
                            */
                           var cumul = new Array();
                           cumul[geneDetails.nodeDegrees.length - 1] = 0;
                           for (var j = geneDetails.nodeDegrees.length - 1; j >= 0; j--) {
                              cumul[j - 1] = geneDetails.nodeDegrees[j] + cumul[j];
                           }
                           cumul.pop();

                           /*
                            * Build array of arrays for plot
                            */
                           var nd = new Array();
                           var k = 0;
                           for (var i = 0; i < cumul.length; i++) {
                              nd.push( [ i + 1, Math.log( cumul[i] + 0.01 ) / Math.log( 10.0 ) ] );
                              k++;
                           }

                           jQuery( '#nodeDegreeSpark' ).sparkline(
                              nd,
                              {
                                 height : 40,
                                 chartRangeMin : -1,
                                 width : 150,
                                 tooltipFormatter : function( spl, ops, fields ) {
                                    return "Links at support level " + fields.x + " or higher: "
                                       + Math.pow( 10, fields.y ).toFixed( 0 ) + "  (Plot is log10 scaled)";
                                 }
                              } );

                           jQuery( "#nodeDegreeHelp" ).qtip( {
                              content : Gemma.HelpText.WidgetDefaults.GeneDetails.nodeDegreeTT,
                              style : {
                                 name : 'cream'
                              }
                           } );

                        }
                     }
                  } );
            } else {
               return "[ Not available ]"; // no help shown - FIXME
            }
         },

         renderAliases : function( aliases ) {
            if ( aliases != null && aliases.length > 0 ) {
               aliases.sort();
               return aliases.join( ', ' );
            }
            return 'None available';
         },

         initComponent : function() {
            Gemma.GeneDetails.superclass.initComponent.call( this );

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

                     GeneController
                        .loadGeneDetails(
                           this.geneId,
                           function( geneDetails ) {
                              this.loadMask.hide();
                              this
                                 .add( [
                                        {
                                           html : '<div style="font-weight: bold; font-size:1.2em;">'
                                              + geneDetails.name
                                              + '<br />'
                                              + geneDetails.officialName
                                              + '&nbsp;&nbsp;<a target="_blank" '
                                              + 'href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gene&cmd=Retrieve&dopt=full_report&list_uids='
                                              + geneDetails.ncbiId
                                              + '"><img ext:qtip="View NCBI record in a new window" alt="NCBI Gene Link" src="/Gemma/images/logo/ncbi.gif"/></a>'
                                              + '<br/></div>'

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

                                           items : [
                                                    {
                                                       fieldLabel : 'Taxon',
                                                       html : geneDetails.taxonCommonName
                                                    },
                                                    {
                                                       fieldLabel : 'Aliases',
                                                       html : this.renderAliases( geneDetails.aliases )
                                                    },

                                                    {
                                                       fieldLabel : 'Homologues',
                                                       html : this.renderHomologues( geneDetails.homologues,
                                                          geneDetails.name )
                                                    },
                                                    {
                                                       fieldLabel : 'Gene Groups',
                                                       html : this.renderGeneSets( geneDetails.geneSets ).join( ', ' )
                                                    },
                                                    {
                                                       fieldLabel : 'Multifunc.'
                                                          + '&nbsp;<i id="multifuncHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                       items : this.renderMultifunctionality( geneDetails )
                                                    },
                                                    {
                                                       fieldLabel : 'Coexp. deg'
                                                          + '&nbsp<i id="nodeDegreeHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                       items : this.renderNodeDegree( geneDetails )
                                                    },
                                                    {
                                                       fieldLabel : 'Phenotypes&nbsp; <i id="phenotypeHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                       items : this.renderPhenotypes( geneDetails ),
                                                       hidden : !(geneDetails.taxonId == 1 || geneDetails.taxonId == 2
                                                          || geneDetails.taxonId == 3 || geneDetails.taxonId == 13 || geneDetails.taxonId == 14)
                                                    },
                                                    {
                                                       fieldLabel : 'Studies'
                                                          + '&nbsp;<i id="studiesHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                       items : this.renderAssociatedExperiments( geneDetails.ncbiId,
                                                          geneDetails.associatedExperimentCount )
                                                    },
                                                    {
                                                       fieldLabel : 'Elements'
                                                          + '&nbsp; <i id="elementsHelp" class="fa fa-question-circle fa-fw" style="font-size:smaller;color:grey"></i>',
                                                       items : new Ext.Panel(
                                                          {
                                                             border : false,
                                                             html : geneDetails.compositeSequenceCount
                                                                + " on "
                                                                + geneDetails.platformCount
                                                                + " different platforms&nbsp;"
                                                                + "&nbsp;<img style='cursor:pointer' src='/Gemma/images/magnifier.png' ext:qtip='View all the elements for this gene'"
                                                                + "onClick='Ext.getCmp(&#39;" + this.id
                                                                + "&#39;).changeTab(&#39;elements&#39;)'>",
                                                             listeners : {
                                                                // FIXME refactor this common code
                                                                'afterrender' : function( c ) {
                                                                   jQuery( '#elementsHelp' )
                                                                      .qtip(
                                                                         {
                                                                            content : Gemma.HelpText.WidgetDefaults.GeneDetails.probesTT,
                                                                            style : {
                                                                               name : 'cream'
                                                                            }
                                                                         } );
                                                                }
                                                             }
                                                          } )
                                                    }
                                           // ,
                                           // {
                                           // fieldLabel : 'Notes',
                                           // html : geneDetails.description
                                           // }
                                           ]
                                        } ] );
                              this.syncSize();
                           }.createDelegate( this ) );
                  } );
         }
      } );