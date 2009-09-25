Ext.namespace('Gemma');

Gemma.DIFF_THRESHOLD = 0.05;

var goTermGrid = function() {
	var ds;
	var grid; // component
	var columnModel; // definition of the columns

	var golink = function(d) {
		var g = d.replace("_", ":");
		return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
				+ g + "'>" + g + "</a>";
	};

	function initDataSource() {
		var recordType = Ext.data.Record.create([{
			name : "id",
			type : "int"
		}, {
			name : "termUri",
			type : "string"
		}, {
			name : "termName",
			type : "string"
		}, {
			name : "evidenceCode",
			type : "string"
		}]);
		ds = new Ext.data.Store({
			proxy : new Ext.data.DWRProxy(GeneController.findGOTerms),
			reader : new Ext.data.ListRangeReader({
				id : "id"
			}, recordType),
			remoteSort : false
		});
		ds.on("load", function() {
		});
	}

	function getColumnModel() {
		if (!columnModel) {
			columnModel = new Ext.grid.ColumnModel([{
				header : "ID",
				dataIndex : "termUri",
				renderer : golink,
				width : 75
			}, {
				header : "Term",
				dataIndex : "termName",
				width : 375
			}, {
				header : "Evidence Code",
				dataIndex : "evidenceCode",
				width : 100
			}

			]);
			columnModel.defaultSortable = true;
		}
		return columnModel;
	}
	function buildGrid() {
		grid = new Ext.grid.GridPanel({
			renderTo : "go-grid",
			height : Ext.get("go-grid").getHeight(),
			width : Ext.get("go-grid").getWidth(),
			ds : ds,
			cm : getColumnModel(),
			loadMask : true
		});
		grid.render();
	}
	return {
		init : function() {
			var geneid = dwr.util.getValue("gene");
			var g = {
				id : geneid
			};
			initDataSource();
			buildGrid();
			ds.load({
				params : [g]
			});
		},
		getStore : function() {
			return ds;
		}
	};
}();

Ext.onReady(goTermGrid.init);

Ext.onReady(function() {
	
	Ext.QuickTips.init();
	
	var geneid = dwr.util.getValue("gene");
	var g = {
		id : geneid
	};
	var converttype = function(d) {
		return d.value;
	};
	var recordType = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "name",
		type : "string"
	}, {
		name : "description",
		type : "string"
	}, {
		name : "type",
		convert : converttype
	}]);

	var ds = new Ext.data.Store({
				proxy : new Ext.data.DWRProxy(GeneController.getProducts),
				reader : new Ext.data.ListRangeReader({
					id : "id"
				}, recordType),
				remoteSort : false
			});
	ds.setDefaultSort('type', 'name');

	var cm = new Ext.grid.ColumnModel([{
		header : "Name",
		width : 80,
		dataIndex : "name"
	}, {
		header : "Type",
		width : 80,
		dataIndex : "type"
	}, {
		header : "Description",
		width : 270,
		dataIndex : "description"
	}]);
	cm.defaultSortable = true;

	var grid = new Ext.grid.GridPanel({
		renderTo : "geneproduct-grid",
		height : Ext.get("geneproduct-grid").getHeight(),
		width : Ext.get("geneproduct-grid").getWidth(),
		ds : ds,
		cm : cm,
		loadMask : true
	});
	grid.render();
	ds.load({
		params : [g]
	});
	
	
	
	//Coexpression grid. 

	var coexpressedGeneGrid = new Gemma.CoexpressionGridLite({
		width : 400,
		colspan : 2,
		//user : false, 
		renderTo : "coexpression-grid"
	});
		

	var geneid = dwr.util.getValue("gene");
			
	
	coexpressedGeneGrid.doSearch({geneIds : [geneid],
				quick: true,
			stringency : 2,
			forceProbeLevelSearch : false});
	
// diff expression grid
				
			var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
							width : 650,
							height : 200, 
							renderTo : "diff-grid"
						});
		
					var eeNameColumnIndex = diffExGrid.getColumnModel().getIndexById('expressionExperimentName');
					diffExGrid.getColumnModel().setHidden(eeNameColumnIndex, true);
					var visColumnIndex = diffExGrid.getColumnModel().getIndexById('visualize');
					diffExGrid.getColumnModel().setHidden(visColumnIndex, false);

			
						
		diffExGrid.getStore().load({params : [ geneid, Gemma.DIFF_THRESHOLD, 50] });

		var visDifWindow = null;
		
	diffExGrid.geneDiffRowClickHandler = function(grid, rowIndex, columnIndex, e) {
		if (diffExGrid.getSelectionModel().hasSelection()) {

			var record = diffExGrid.getStore().getAt(rowIndex);
			var fieldName = diffExGrid.getColumnModel().getDataIndex(columnIndex);
			var gene = record.data.gene;

			if (fieldName == 'visualize') {
				
				var ee = record.data.expressionExperiment;

				if (visDifWindow != null)
					visDifWindow.close();
					
				visDifWindow = new Gemma.VisualizationDifferentialWindow();
				visDifWindow.dv.store = new Gemma.VisualizationStore({
					readMethod :  DEDVController.getDEDVForDiffExVisualizationByExperiment
					});
	
				
				visDifWindow.displayWindow =  function(ee, geneId) {

							this.setTitle("Visualization of probes in:  "
									+ ee.shortName);
					
							var downloadDedvLink = String
									.format(
											"<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1}' > <img src='/Gemma/images/download.gif'/></a>",
											ee.id, geneId);
					
							this.thumbnailPanel.setTitle("Thumbnails &nbsp; " + downloadDedvLink);
														
							var params = [];
							params.push(ee.id);
							params.push(geneId);	//Gene would be nicer but don't have access to the full object... hmmmm
							params.push(Gemma.DIFF_THRESHOLD);
					
							this.show();
							this.dv.store.load({
								params : params
							});
					
					}.createDelegate(visDifWindow);		//Without the createDelegate IE has fails to re-render the zoom panel
				
				var geneId = dwr.util.getValue("gene");

				visDifWindow.displayWindow(ee, geneId);

			
			}
		}
	};

	diffExGrid.on("cellclick", diffExGrid.geneDiffRowClickHandler.createDelegate(visDifWindow), diffExGrid);
		
	

});


Gemma.geneLinkOutPopUp = function(abaImageUrl){
	  
	//TODO put a null, empty string check in. 
		  win = new Ext.Window({							             							  							            						             				          
                html: "<img src='"+ abaImageUrl +"'>",    
                autoScroll : true 
		    });
		    win.setTitle("<img height=15  src='/Gemma/images/abaExpressionLegend.gif'>");
    		win.show(this);
   	

};

