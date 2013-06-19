Ext.namespace('Gemma');

/**
 * Show the data sets that occur in a set of coexpression results.
 * 
 * @author luke, paul
 * @version $id$
 */
Gemma.CoexpressionDatasetGrid = Ext.extend(Gemma.GemmaGridPanel, {

      collapsible : true,
      collapsed : true,
      hidden : true,
      title : 'Dataset information',
      style : "margin-top: 1em; margin-bottom: .5em;",
      autoScroll : true,
      height : 250,
      stateful : false,

      record : Ext.data.Record.create([{
            name : "id",
            type : "int"
         }, {
            name : "shortName",
            type : "string"
         }, {
            name : "name",
            type : "string"
         }, {
            name : "coexpressionLinkCount",
            type : "int"
         }, {
            name : "probeSpecificForQueryGene"
         }, {
            name : "arrayDesignCount",
            type : "int"
         }, {
            name : "bioAssayCount",
            type : "int"
         }, {
            name : "queryGene",
            type : "string"
         }]),

      loadData : function(d) {
         var datasets = {}, numDatasets = 0;
         for (var i = 0; i < d.length; ++i) {
            if (!datasets[d[i].id]) {
               datasets[d[i].id] = 1;
               ++numDatasets;
            }
         }
         var title = String.format("{0} dataset{1} relevant{2} coexpression data", numDatasets, numDatasets == 1 ? " has" : "s have", this.adjective ? " " + this.adjective : "");

         this.setTitle(title);
         this.show();
         this.getStore().proxy.data = d;
         this.getStore().reload();
      },

      initComponent : function() {

         Ext.apply(this, {
               store : new Ext.data.GroupingStore({
                     proxy : new Ext.data.MemoryProxy([]),
                     reader : new Ext.data.ListRangeReader({}, this.record),
                     groupField : 'queryGene',
                     sortInfo : {
                        field : 'coexpressionLinkCount',
                        direction : 'DESC'
                     }
                  }),

               view : new Ext.grid.GroupingView({
                     hideGroupedColumn : true
                  }),

               columns : [{
                  id : ' shortName ',
                  header : "Dataset",
                  dataIndex : "shortName",
                  sortable : true,
                  tooltip : "Dataset short name"
                  // renderer : this.eeStyler.createDelegate(this)
               }, {
                  id : 'name',
                  header : "Name",
                  dataIndex : "name",
                  tooltip : "Dataset long name",
                  sortable : true,
                  width : 230

               }, {
                  id : ' queryGene ',
                  header : "Query Gene",
                  dataIndex : "queryGene",
                  hidden : true,
                  sortable : true

               }, {
                  header : " Contributing Links ",
                  dataIndex : "coexpressionLinkCount",
                  tooltip : "# contributions to confirmed links",
                  align : 'center',
                  sortable : true

               }
                  // see bug 1564
                  // , {
                  // header : " Specific Probe ",
                  // dataIndex : "probeSpecificForQueryGene",
                  // type : "boolean",
                  // tooltip : "Does the dataset have a probe that is specific for the query gene?",
                  // renderer : this.booleanStyler.createDelegate(this),
                  // align: 'center',
                  // sortable : true
                  //
                  // }
                  , {
                     id : 'assays',
                     header : " Assays ",
                     dataIndex : "bioAssayCount",
                     tooltip : "# of samples in the study",
                     align : 'center',
                     sortable : true
                     // renderer : this.assayCountStyler.createDelegate(this)
               }]
            });

         Gemma.CoexpressionDatasetGrid.superclass.initComponent.call(this);

      },

      assayCountStyler : function(value, metadata, record, row, col, ds) {
         return String.format("{0}&nbsp;<a href='/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id={1}'>"
               + "<img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a>", record.data.bioAssayCount, record.data.id);
      },

      booleanStyler : function(value, metadata, record, row, col, ds) {
         if (value) {
            return "<img src='/Gemma/images/icons/ok.png' height='10' width='10' />";
         }
         return "";
      },

      eeTemplate : new Ext.Template("<a target='_blank' " + "href='/Gemma/expressionExperiment/showExpressionExperiment.html?id={id}' ext:qtip='{name}'>{shortName}</a>"),

      eeStyler : function(value, metadata, record, row, col, ds) {
         this.eeTemplate.apply(record.data);
      }

   });

Gemma.CoexpressionDatasetGrid.updateDatasetInfo = function(datasets, eeMap) {
   for (var i = 0; i < datasets.length; ++i) {
      var ee = eeMap[datasets[i].id];
      if (ee) {
         datasets[i].shortName = ee.shortName;
         datasets[i].name = ee.name;
      }
   }
};
