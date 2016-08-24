/**
 * Shows the summary of the coexpression search results.
 * 
 * @class Gemma.CoexpressionSummaryGrid
 * @extends Gemma.GemmaGridPanel
 */
Gemma.CoexpressionSummaryGrid = Ext
   .extend(
      Gemma.GemmaGridPanel,
      {

         editable : false,
         title : 'Search Summary',
         width : 350,
         height : 300,
         stateful : false,

         /**
          * @memberOf Gemma.CoexpressionSummaryGrid
          */
         initComponent : function() {

            var columns = [ {
               header : 'Group',
               dataIndex : 'group',
               width : 100
            }, {
               id : 'key',
               header : '',
               dataIndex : 'key',
               align : 'right',
               width : 200
            } ];

            for (var i = 0; i < this.genes.length; ++i) {
               columns.push( {
                  header : this.genes[i].officialSymbol,
                  dataIndex : this.genes[i].officialSymbol,
                  align : 'right',
                  width : 100
               } );
            }

            var fields = [ {
               name : 'sort',
               mapping : 0,
               type : 'int'
            }, {
               name : 'group',
               mapping : 1,
               type : 'string'
            }, {
               name : 'key',
               mapping : 2,
               type : 'string'
            } ];
            for (var i = 0; i < this.genes.length; ++i) {
               fields.push( {
                  name : this.genes[i].officialSymbol,
                  mapping : i + 3,
                  type : 'string'
               } );
            }

            Ext.apply( this, {
               columns : columns,
               store : new Ext.data.GroupingStore( {
                  reader : new Ext.data.ArrayReader( {}, fields ),
                  groupField : 'group',
                  data : this.transformData( this.genes, this.summary ),
                  sortInfo : {
                     field : 'sort',
                     direction : 'ASC'
                  }
               } ),
               view : new Ext.grid.GroupingView( {
                  enableGroupingMenu : false,
                  enableNoGroups : false,
                  hideGroupedColumn : true,
                  showGroupName : false
               } )
            } );

            Gemma.CoexpressionSummaryGrid.superclass.initComponent.call( this );

         },

         transformData : function( genes, summary ) {

            var datasetsAvailable = [ 0, "Datasets",
                                     "<span ext:qtip='How many data sets met your criteria'>Available</span>" ];
            var datasetsTested = [ 1, "Datasets",
                                  "<span ext:qtip='How many data sets had the query gene available for analysis'>Query gene testable</span>" ];
            var linksFound = [
                              2,
                              "Links",
                              "<span ext:qtip='Total number of links (may show the number including those not meeting your stringency threshold)'>Found</span>" ];
            var linksPositive = [ 3, "Links",
                                 "<span ext:qtip='How many genes were considered positively correlated with the query'>Met stringency (+)</span>" ];
            var linksNegative = [ 4, "Links",
                                 "<span ext:qtip='How many genes were considered negatively correlated with the query'>Met stringency (-)</span>" ];

            var geneDetails = [ 5, "Query Gene", "Details" ];

            for (var i = 0; i < genes.length; ++i) {
               var thisSummary = summary[genes[i].id] || {};
               datasetsAvailable.push( thisSummary.datasetsAvailable );
               datasetsTested.push( thisSummary.datasetsTested );
               linksFound.push( thisSummary.linksFound );
               linksPositive.push( thisSummary.linksMetPositiveStringency );
               linksNegative.push( thisSummary.linksMetNegativeStringency );
               geneDetails.push( String.format(
                  "<a target=\"_blank\" href='/Gemma/gene/showGene.html?id={0}' ext:qtip='{1}'> {2} </a> ",
                  genes[i].id, genes[i].officialName, genes[i].officialSymbol ) );
            }

            return [ datasetsAvailable, datasetsTested, linksFound, linksPositive, linksNegative, geneDetails ];
         }

      } );
