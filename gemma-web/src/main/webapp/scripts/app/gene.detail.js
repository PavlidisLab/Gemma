Ext.namespace('Gemma');



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
		title : "Coexpressed genes",
		autoHeight : true,
		pageSize : 15,
		colspan : 2,
		//user : false, 
		renderTo : "coexpression-grid"
	});
	
	
	
	
	var returnFromCoexpressionSearch = function(result){
				coexpressedGeneGrid.loadData(result.isCannedAnalysis, result.queryGenes.length, result.knownGeneResults,
				result.knownGeneDatasets);
				
				//this.loadMask.hide();
	};
	
	var errorHandler = function(error){
		console.log(error);
	};
	 
	 
//	 this.loadMask = new Ext.LoadMask( {
//		msg : "Searching for coexpressions ..."
//	});
//	this.loadMask.show();
//	
	var geneid = dwr.util.getValue("gene");

	var csc = {geneIds : [geneid],
				quick: true,
			stringency : 2,
			forceProbeLevelSearch : false};
			
		ExtCoexpressionSearchController.doSearch(csc, {
					callback : returnFromCoexpressionSearch.createDelegate(this),
					errorHandler : errorHandler.createDelegate(this)
				});

// diff expression grid
				
			var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
							width : 650,
							height : 200, 
							renderTo : "diff-grid"
						});
		
					var eeNameColumnIndex = diffExGrid.getColumnModel().getIndexById('expressionExperimentName');
					diffExGrid.getColumnModel().setHidden(eeNameColumnIndex, true);
			
						
		diffExGrid.getStore().load({params : [ geneid, 0.05] });

	

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

