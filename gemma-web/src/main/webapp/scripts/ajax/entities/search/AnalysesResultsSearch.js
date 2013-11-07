Ext.namespace('Gemma');

Gemma.AnalysisResultsSearch = Ext.extend(Ext.Panel, {
      SEARCH_FORM_WIDTH : 900,
      autoScroll : true,

      initComponent : function() {
         Gemma.AnalysisResultsSearch.superclass.initComponent.call(this);

         this.admin = (Ext.get('hasAdmin') !== null) ? Ext.get('hasAdmin').getValue() : null;
         this.user = (Ext.get('hasUser') !== null) ? Ext.get('hasUser').getValue() : null;

         var errorPanel = new Ext.Panel({
               tpl : '<img src="/Gemma/images/icons/warning.png">{msg}',
               border : false,
               hidden : true
            });

         this.add(errorPanel);

         // Check if canvas is supported (not supported in IE < 9; need to use excanvas in IE8)
         var browserWarning = "";
         if (!document.createElement("canvas").getContext) {
            // Not supported
            // excanvas doesn't cover all functionality of new diff ex metaheatmap visualization
            if (Ext.isIE8) {
               browserWarning = Gemma.HelpText.CommonWarnings.BrowserWarnings.ie8;
            } else if (Ext.isIE) {
               browserWarning = Gemma.HelpText.CommonWarnings.BrowserWarnings.ieNot8;
            } else {
               browserWarning = Gemma.HelpText.CommonWarnings.BrowserWarnings.generic;
            }
         }

         // panel for performing search, appears on load
         var searchForm = new Gemma.AnalysisResultsSearchForm({
               width : this.SEARCH_FORM_WIDTH,
               bodyStyle : 'text-align:left;',
               style : 'text-align:left'
            });

         var diffExResultsDiv = new Ext.Panel();
         // panel to hold all results of searches
         var coexResultsTabPanel = new Ext.TabPanel({
               border : true,
               // hidden: true,
               // deferredRender: true,
               bodyStyle : 'text-align:left;',
               style : 'text-align:left',
               hidden : true
            });

         this.searchPanel = new Ext.Panel({
               items : [searchForm],
               title : 'Search Form',
               collapsible : true,
               titleCollapse : true,
               border : false,
               bodyStyle : 'margin-left:auto;margin-right:auto;width:' + this.SEARCH_FORM_WIDTH + 'px;'
            });
         this.add(this.searchPanel);
         this.add(new Ext.Panel({
               items : [coexResultsTabPanel],
               title : 'Results',
               layout : 'fit'
            }));
         this.add(diffExResultsDiv);
         this.doLayout();

         // window that controls diff visualizer;
         // it's not part of the results panel so need to keep track separately to be able to delete it
         this.diffVisualizer = null;

         if (browserWarning !== "") {
            errorPanel.on('render', function() {
                  this.update('<img src="/Gemma/images/icons/warning.png">' + browserWarning);
                  this.show();
               });
         }

         // get ready to show results
         searchForm.on("beforesearch", function(panel) {

               // before every search, clear the results in preparation for new (possibly blank) results

               var flashPanel = coexResultsTabPanel.getItem('cytoscaperesults');
               if (flashPanel) {
                  flashPanel.stopRender = true;
               }
               coexResultsTabPanel.removeAll();
               panel.clearError();
               // clear errors
               errorPanel.update();
               errorPanel.hide();
               coexResultsTabPanel.doLayout();
               coexResultsTabPanel.hide();

               // for clearing diff ex
               this.remove(diffExResultsDiv.getId());
               diffExResultsDiv = new Ext.Panel();
               this.add(diffExResultsDiv);
               this.doLayout();

            }, this);

         searchForm.on("search_error", function(msg) {
               errorPanel.update({
                     msg : msg
                  });
               errorPanel.show();
            });         

         searchForm.on("showOptions", function(stringency, forceProbeLevelSearch, queryGenesOnly) {
               if (this.admin) {
                  coexResultsTabPanel.insert(1, {
                        border : false,
                        html : '<h4>Refinements Used:</h4>Stringency = ' + stringency + '<br>Probe-level search: ' + forceProbeLevelSearch + "<br>Results only among query genes: "
                           + queryGenesOnly + '<br>'
                     });
               } else {
                  coexResultsTabPanel.insert(1, {
                        border : false,
                        html : '<h4>Refinements Used:</h4>Stringency = ' + stringency + "<br>Results only among query genes: " + queryGenesOnly + '<br>'
                     });
               }

               coexResultsTabPanel.doLayout();
            }, this);

         searchForm.on("differential_expression_search_query_ready", function(formPanel, result, data) {
               // show metaheatmap viewer (but not control panel)
               // control panel is responsible for creating the visualisation view space
               Ext.apply(data, {
                     applyTo : diffExResultsDiv.getId()
                  });

               // override showing tutorial, for now only works with non-widget version
               Ext.apply(data, {
                     showTutorial : false
                  });

               this.diffVisualizer = new Gemma.MetaHeatmapDataSelection(data);
               this.diffVisualizer.on('visualizationLoaded', function() {
                     this.searchPanel.collapse();
                     formPanel.loadMask.hide();
                  }, this);
            }, this);
      }


   });
