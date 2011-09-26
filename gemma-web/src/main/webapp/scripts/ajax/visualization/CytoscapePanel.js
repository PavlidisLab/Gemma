Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(
Ext.Panel, {

	title: 'Cytoscape',

	closeAction: 'hide',

	layoutArray: ["ForceDirected", "Circle", "Radial", "Tree", "Preset"],
	layoutIndex: 0,

	currentNodeGeneIds: [],

	currentEdgesStrings: [],

	//used to store query genes for "complete the graph" search
	currentQueryGeneIdsComplete: [],

	//used to store initial Search query gene ids, plus extended gene ids
	currentQueryGeneIds: [],

	dataJSON: [],

	dataSchemaJSON: {

		nodes: [{
			name: 'label',
			type: 'string'
		}, {
			name: 'geneid',
			type: 'number'
		}, {
			name: 'queryflag',
			type: 'boolean'
		}],
		edges: [{
			name: 'positivesupport',
			type: 'number'
		}]

	},

	options: {
		// where you have the Cytoscape Web SWF
		swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
		// where you have the Flash installer SWF
		flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
	},

	visual_style: {
		global: {
			backgroundColor: "#ABCFD6"
		},
		nodes: {
			shape: "ELLIPSE",
			borderWidth: 3,
			borderColor: "#ffffff",
			size: {
				defaultValue: 30

			},
			labelHorizontalAnchor: "center",
			color: {
				discreteMapper: {
					attrName: "queryflag",
					entries: [{
						attrValue: true,
						value: "#FF8C69"
					}, {
						attrValue: false,
						value: "#E0FFFF"
					}]
				}
			}
		},
		edges: {
			width: {
				defaultValue: 1,
				continuousMapper: {
					attrName: "positivesupport",
					minValue: 1,
					maxValue: 6 /*, minAttrValue: 0.1, maxAttrValue: 1.0*/
				}

			},

			color: "#0B94B1"
		}
	},

	initComponent: function () {

		Ext.apply(
		this, {

			_offset: this.textOffset,

			items: [{

				xtype: 'box',
				height: 550,
				width: 850,

				id: 'cytoscapeweb',
				listeners: {
					afterrender: {
						scope: this,
						fn: function () {

							var command = this.coexCommand;

							var queryGeneIdsComplete = [];

							var qgenes = this.queryGenes;
							var knowngenes = this.knownGeneResults;

							var qlength = qgenes.length;

							//populate geneid array for complete graph
							for (var i = 0; i < qlength; i++) {

								queryGeneIdsComplete.push(qgenes[i].id);

								this.currentQueryGeneIds.push(qgenes[i].id);

							}

							var kglength = knowngenes.length;

							for (var i = 0; i < kglength; i++) {

								if (queryGeneIdsComplete.indexOf(knowngenes[i].foundGene.id) === -1) {
									queryGeneIdsComplete.push(knowngenes[i].foundGene.id);
								}
							}

							Ext.apply(
							command, {
								geneIds: queryGeneIdsComplete,
								queryGenesOnly: true
							});

							this.currentQueryGeneIdsComplete = queryGeneIdsComplete;

							ExtCoexpressionSearchController.doSearchQuick2(
							command, {
								callback: this.returnFromCompleteCoexSearch.createDelegate(this)
								//,
								//errorHandler : errorHandler
							});

						}
					}

				}

			}]
		});

		var div_id = "cytoscapeweb";

		var vis = new org.cytoscapeweb.Visualization(div_id, this.options);

		vis.panelRef = this;

		vis.ready(function () {

			// Add the Context menu item for radial layout
			vis.addContextMenuItem("Radial layout", "none", function () {
				vis.layout("Radial");
			}).addContextMenuItem("Circle layout", "none", function () {
				vis.layout("Circle");
			}).addContextMenuItem("ForceDirected layout", "none", function () {
				vis.layout("ForceDirected");
			});

			vis.addContextMenuItem("Extend this node", "nodes", function (evt) {

				var rootNode = evt.target;

				// Get the first neighbors of that node:
				//var fNeighbors = vis.firstNeighbors([rootNode]);
				//var neighborNodes = fNeighbors.neighbors;
				// Select the root node and its neighbors:
				//vis.select([rootNode]).select(neighborNodes);
				rootNode.data.geneid

				var command = vis.panelRef.coexCommand;

				Ext.apply(
				command, {
					geneIds: [rootNode.data.geneid],
					queryGenesOnly: false
				});


				if (vis.panelRef.currentQueryGeneIdsComplete.indexOf(rootNode.data.geneid) === -1) {

					vis.panelRef.currentQueryGeneIds.push(rootNode.data.geneid);
				}
				//this gene is now a query gene so go into dataJSON and change it
				dataNodes = vis.panelRef.dataJSON.nodes;

				var dlength = dataNodes.length;

				for (var i = 0; i < dlength; i++) {
					var breakflag = false
					for (var j in dataNodes[i]) {

						if (j == "geneid") {

							var gid = dataNodes[i][j];

							if (gid == rootNode.data.geneid) {


								dataNodes[i]["queryflag"] = true;

								var test = dataNodes[i]["queryflag"];
								breakflag = true;
							}


						}

						if (breakflag) {
							break;
						}

					}

					if (breakflag) {
						break;
					}

				}


				ExtCoexpressionSearchController.doSearchQuick2(
				command, {
					callback: vis.panelRef.returnFromExtendThisNodeInitialCoexSearch.createDelegate(vis.panelRef)
					//,
					//errorHandler : errorHandler
				});

			});
			vis.addContextMenuItem("Extend Selected nodes", "none", function (evt) {




				var selectedNodes = vis.selected("nodes");

				// Get the first neighbors of that node:
				//var fNeighbors = vis.firstNeighbors([rootNode]);
				//var neighborNodes = fNeighbors.neighbors;
				// Select the root node and its neighbors:
				//vis.select([rootNode]).select(neighborNodes);
				var extendedNodesGeneIdArray = [];

				var sNodesLength = selectedNodes.length;
				for (var i = 0; i < sNodesLength; i++) {

					extendedNodesGeneIdArray[i] = selectedNodes[i].data.geneid;

					//store ids of query genes for "complete" search to come later in process
					if (vis.panelRef.currentQueryGeneIdsComplete.indexOf(selectedNodes[i].data.geneid) === -1) {
						vis.panelRef.currentQueryGeneIds.push(selectedNodes[i].data.geneid);
					}

				}

				var command = vis.panelRef.coexCommand;

				Ext.apply(
				command, {
					geneIds: extendedNodesGeneIdArray,
					queryGenesOnly: false
				});



				//these genes are now query genes so go into dataJSON and change it
				dataNodes = vis.panelRef.dataJSON.nodes;

				var dlength = dataNodes.length;

				for (var i = 0; i < dlength; i++) {
					var breakflag = false
					for (var j in dataNodes[i]) {

						if (j == "geneid") {

							var gid = dataNodes[i][j];

							for (var k = 0; k < extendedNodesGeneIdArray.length; k++) {

								if (gid == extendedNodesGeneIdArray[k]) {

									dataNodes[i]["queryflag"] = true;

									var test = dataNodes[i]["queryflag"];

								}
							}


						}



					}


				}


				ExtCoexpressionSearchController.doSearchQuick2(
				command, {
					callback: vis.panelRef.returnFromExtendThisNodeInitialCoexSearch.createDelegate(vis.panelRef)
					//,
					//errorHandler : errorHandler
				});

			});

			document.getElementById("changeLayout").onclick = function () {

				layoutIndex = layoutIndex + 1;

				if (layoutIndex > layoutArray.length - 1) {

					layoutIndex = 0;
				}

				var index = layoutIndex;

				vis.layout(layoutArray[index]);

			};


		});
		//end vis.ready()  
		Ext.apply(this, {

			visualization: vis,

			closable: false,

			margins: {
				top: 0,
				right: 0,
				bottom: 0,
				left: 0
			}

		});

		Gemma.CytoscapePanel.superclass.initComponent.apply(
		this, arguments);


	},

	returnFromCompleteCoexSearch: function (result) {

		this.queryGenes = result.queryGenes;

		this.knownGeneResults = result.knownGeneResults;

		this.dataJSON = this.constructDataJSON(this.queryGenes, this.knownGeneResults);

		this.drawGraph();

	},

	returnFromExtendThisNodeInitialCoexSearch: function (result) {

		//result has one query gene and possible some found genes, run my genes only search with these genes as query genes
		var qgenes = result.queryGenes;
		var knowngenes = result.knownGeneResults;
		this.mergeDataJSON(knowngenes);

		var qlength = qgenes.length;

		//populate geneid array for complete graph
		for (var i = 0; i < qlength; i++) {

			if (this.currentQueryGeneIdsComplete.indexOf(qgenes[i].id) === -1) {
				this.currentQueryGeneIdsComplete.push(qgenes[i].id);
			}

		}

		var kglength = knowngenes.length;

		var completeSearchFlag = false;

		for (var i = 0; i < kglength; i++) {

			if (this.currentQueryGeneIdsComplete.indexOf(knowngenes[i].foundGene.id) === -1) {
				this.currentQueryGeneIdsComplete.push(knowngenes[i].foundGene.id);
				completeSearchFlag = true;
			}
		}


		//only do completeSearch if new genes have shown up
		if (completeSearchFlag === true) {

			var command = this.coexCommand;

			Ext.apply(command, {
				geneIds: this.currentQueryGeneIdsComplete,
				queryGenesOnly: true
			});

			ExtCoexpressionSearchController.doSearchQuick2(command, {
				callback: this.returnFromExtendThisNodeCompleteCoexSearch.createDelegate(this)
				//,
				//errorHandler : errorHandler
			});
		}


	},

	returnFromExtendThisNodeCompleteCoexSearch: function (result) {
		this.mergeDataJSON(result.knownGeneResults);
		this.drawGraph();
	},

	constructDataJSON: function (qgenes, knowngenes) {

		data = {
			nodes: [],
			edges: []
		}

		var nodesArray = [];

		var edgesArray = [];

		//array for keeping track of what nodes have already been added
		var nodeGeneIds = [];
		//array to keep track of edges
		var edgesStrings = [];

		var queryFlag = false;
		var qlength = qgenes.length;

		for (var i = 0; i < qlength; i++) {

			if (this.currentQueryGeneIds.indexOf(qgenes[i].id) !== -1) {
				queryFlag = true;
			}

			nodesArray.push({
				id: qgenes[i].officialSymbol,
				label: qgenes[i].officialSymbol,
				geneid: qgenes[i].id,
				queryflag: queryFlag
			});
			nodeGeneIds.push(qgenes[i].id);
			queryFlag = false;

		}

		var kglength = knowngenes.length;
		//var queryFlag=false;
		//add found genes to node data plus populate edge data
		for (var i = 0; i < kglength; i++) {

			if (nodeGeneIds.indexOf(knowngenes[i].foundGene.id) === -1) {

				//if (this.currentQueryGeneIds.indexOf(knowngenes[i].foundGene.id) !==-1){
				//	queryFlag = true;								
				//}
				nodeGeneIds.push(knowngenes[i].foundGene.id);

				nodesArray.push({
					id: knowngenes[i].foundGene.officialSymbol,
					label: knowngenes[i].foundGene.officialSymbol,
					geneid: knowngenes[i].foundGene.id,
					queryflag: false
				});

			}


			if (edgesStrings.indexOf(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol) === -1 && edgesStrings.indexOf(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol === -1)) {

				edgesArray.push({
					id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
					target: knowngenes[i].foundGene.officialSymbol,
					source: knowngenes[i].queryGene.officialSymbol,
					positivesupport: knowngenes[i].posSupp
				});
				edgesStrings.push(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol);
				edgesStrings.push(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol);
			}
		}

		data.nodes = nodesArray;
		data.edges = edgesArray;

		this.currentNodeGeneIds = nodeGeneIds;
		this.currentEdgesStrings = edgesStrings;

		return data;
	},

	mergeDataJSON: function (knowngenes) {

		var kglength = knowngenes.length;

		var queryFlag = false;

		//add found genes to node data plus populate edge data
		for (var i = 0; i < kglength; i++) {

			if (this.currentNodeGeneIds.indexOf(knowngenes[i].foundGene.id) === -1) {

				this.currentNodeGeneIds.push(knowngenes[i].foundGene.id);

				this.dataJSON.nodes.push({
					id: knowngenes[i].foundGene.officialSymbol,
					label: knowngenes[i].foundGene.officialSymbol,
					geneid: knowngenes[i].foundGene.id,
					queryflag: queryFlag
				});

			}

			if (this.currentEdgesStrings.indexOf(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol) === -1 && this.currentEdgesStrings.indexOf(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol === -1)) {

				this.dataJSON.edges.push({
					id: knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol,
					target: knowngenes[i].foundGene.officialSymbol,
					source: knowngenes[i].queryGene.officialSymbol,
					positivesupport: knowngenes[i].posSupp
				});
				this.currentEdgesStrings.push(knowngenes[i].foundGene.officialSymbol + "to" + knowngenes[i].queryGene.officialSymbol);
				this.currentEdgesStrings.push(knowngenes[i].queryGene.officialSymbol + "to" + knowngenes[i].foundGene.officialSymbol);
			}
		}

	},

	drawGraph: function () {

		var dataMsg = {

			dataSchema: this.dataSchemaJSON,

			data: this.dataJSON

		}
		// you could also use other formats (e.g. GraphML) or grab the network data via AJAX
		var networ_json = []

		if (networ_json.length == 0) {
			networ_json = dataMsg;
		} else {
			//change this after testing to merge existing networ_json with new dataMsg network
			networ_json = dataMsg;

		}

		// init and draw
		this.visualization.draw({
			network: networ_json,
			visualStyle: this.visual_style,
			layout: "ForceDirected"
		});

	}

});
