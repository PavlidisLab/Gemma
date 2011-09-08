Ext.namespace('Gemma');

Gemma.CytoscapePanel = Ext.extend(Ext.Panel,
{
	
	title:'Cytoscape',
	
	closeAction : 'hide',
	
	layoutArray: ["ForceDirected", "Circle", "Radial", "Tree", "Preset"],
    layoutIndex :0,
        	
    newNodeSuffix: 1,
    newNodeX:10,
    newNodeY:10,
        
    
	
	
    initComponent: function(){ 
			
			Ext.apply(this, {
				
								
				_offset: this.textOffset,
				
				items: [{
				
					xtype: 'box',
					height: 600,
					width: 900,
					
					id: 'cytoscapeweb',
					listeners: {
						afterrender: {
							scope: this,
							fn: function(){
					
								var command = this.coexCommand;
								
								var queryGeneIdsComplete = [];
								
								var qgenes = this.queryGenes;
								var knowngenes =  this.knownGeneResults;
    		  
    		  					var qlength=qgenes.length;
    		  
    		  					//populate geneid array for complete graph
    		  					for (var i =0; i< qlength; i++){    			  
    			  					
    			  					queryGeneIdsComplete.push(qgenes[i].id);
    			  
    		  					}
    		  
    		  					var kglength = knowngenes.length; 
    		  
    		  					
    		  					for (var i = 0; i < kglength; i++ ){
    			  
    			  					if (queryGeneIdsComplete.indexOf(knowngenes[i].foundGene.id)===-1){
    				  					queryGeneIdsComplete.push(knowngenes[i].foundGene.id);
    			  					} 
    		  					}
					
								Ext.apply(command, {
									geneIds : queryGeneIdsComplete,									
									queryGenesOnly : true
								});
								
								
								ExtCoexpressionSearchController.doSearch(command, {
									callback : this.returnFromCompleteCoexSearch.createDelegate(this)//,
									//errorHandler : errorHandler
								});
								
					
							}
						}
					
					
					}
				
				
				}]
			});
			
		
		
		
	
		
		
        Ext.apply(this, {
        	
        	closable: false,
        	
			margins : {
				top : 0,
				right : 0,
				bottom : 0,
				left : 0
			}
        	
         }); 
        
        Gemma.CytoscapePanel.superclass.initComponent.apply(this, arguments);
        
        //this.addEvents('drawGraphEvent');
    
        
    },

    
    		returnFromCompleteCoexSearch : function(result) { 
    	
    			this.queryGenes = result.queryGenes;
    
    			this.knownGeneResults = result.knownGeneResults;
    	
    			this.drawGraph();
    	
    	
    	
    		},
    		
			constructDataJSON : function (qgenes, knowngenes){
    	
    		  data = {nodes: [{id: "MAP1B", label:"MAP1B", geneid:"88685"},{id: "TUSC3", label:"TUSC3", geneid:"174428"}],edges: [{id: "MAP1BtoTUSC3", target: "TUSC3", source:"MAP1B"}]}
    	
    	
    		  var nodesArray = [];
    		  
    		  var edgesArray = [];
    		  
    		  //array for keeping track of what nodes have already been added
    		  var nodeGeneIds = [];
    		  //array to keep track of edges
    		  var edgesStrings =[];
    		  
    		  var qlength=qgenes.length;
    		  
    		  //start by adding query genes to node data
    		  for (var i =0; i< qlength; i++){
    			  
    			  nodesArray.push({id:qgenes[i].officialSymbol, label:qgenes[i].officialSymbol, geneid: qgenes[i].id});
    			  nodeGeneIds.push(qgenes[i].id);
    			  
    		  }
    		  
    		  var kglength = knowngenes.length; 
    		  
    		  //add found genes to node data plus populate edge data
    		  for (var i = 0; i < kglength; i++ ){
    			  
    			  if (nodeGeneIds.indexOf(knowngenes[i].foundGene.id)===-1){
    				  nodeGeneIds.push(knowngenes[i].foundGene.id);
    				  
    				  nodesArray.push({id:knowngenes[i].foundGene.officialSymbol, label:knowngenes[i].foundGene.officialSymbol, geneid: knowngenes[i].foundGene.id});    				  
    				  
    			  }
    			  
    			  if (edgesStrings.indexOf(knowngenes[i].foundGene.officialSymbol+"to"+knowngenes[i].queryGene.officialSymbol)===-1 &&
    					  edgesStrings.indexOf(knowngenes[i].queryGene.officialSymbol+"to"+knowngenes[i].foundGene.officialSymbol===-1)){
    			  
    			  	edgesArray.push({id:knowngenes[i].foundGene.officialSymbol+"to"+knowngenes[i].queryGene.officialSymbol  , target:knowngenes[i].foundGene.officialSymbol, source:knowngenes[i].queryGene.officialSymbol });
    			  	edgesStrings.push(knowngenes[i].foundGene.officialSymbol+"to"+knowngenes[i].queryGene.officialSymbol);
    			  	edgesStrings.push(knowngenes[i].queryGene.officialSymbol+"to"+knowngenes[i].foundGene.officialSymbol);
    			  }
    		  }
    		  
    		  data.nodes = nodesArray;
    		  data.edges = edgesArray;
    		  
    			return data;
			},
	
        
        	drawGraph : function(){
        	
        	/*
    	
    		Ext.Ajax.request({
         	url : '/Gemma/expressionExperiment/getCoexpressionNetwork.html',
         	params: {geneid:geneIdParam, stringency:'2'},
            method: 'GET',                  
            success: function ( response, options ) {			
					
                    var dataMsg = Ext.util.JSON.decode(response.responseText);
                    // id of Cytoscape Web container div
                    var div_id = "cytoscapeweb";
                
                // you could also use other formats (e.g. GraphML) or grab the network data via AJAX
                
                
                
               
                
                    
                var networ_json = []    
                    
                if (networ_json.length == 0){
                	networ_json = dataMsg;
                }
                else{
                	//change this after testing to merge existing networ_json with new dataMsg network
                	networ_json = dataMsg;
                
                
                } 
                
                // initialization options
                var options = {
                    // where you have the Cytoscape Web SWF
                    swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
                    // where you have the Flash installer SWF
                    flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
                };
                
                
                var visual_style = {
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
                         labelHorizontalAnchor: "center"
                     },
                     edges: {
                         width: 1,
                         color: "#0B94B1"
                     }
                 };
                
                // init and draw
                var vis = new org.cytoscapeweb.Visualization(div_id, options);
                
                
                
              
              vis.ready(function(){
              
              
              	// Add the Context menu item for radial layout
                vis.addContextMenuItem("Radial layout", "none",
                function () {
                vis.layout("Radial");
                }).addContextMenuItem("Circle layout", "none",
                function () {
                  vis.layout("Circle");
                }).addContextMenuItem("Tree layout", "none",
                function () {
                  vis.layout("Tree");
                }); 
              
              
             	vis.addContextMenuItem("Some Option", "nodes", function(evt){
              	
              	var rootNode = evt.target;
         
             	// Get the first neighbors of that node:
             	var fNeighbors = vis.firstNeighbors([rootNode]);
             	var neighborNodes = fNeighbors.neighbors;
         
             	// Select the root node and its neighbors:
             	vis.select([rootNode]).select(neighborNodes);
              	
              	
              	});
              
              	//doubleclick
              	vis.addListener("dblclick", "nodes", function(event){
              	
              		getCoexpressionData(event.target.data.geneid);
              	
              		var target = event.target;
              		
              		var nodeId = "newNode"+newNodeSuffix+event.target.data.geneid;
              		newNodeSuffix = newNodeSuffix+1;
              		
              		var data = { id: nodeId,
                 	label: nodeId
                 	};
     
   					var node = vis.addNode(newNodeX, newNodeY, data, true);
   					newNodeX = newNodeX +50;
   					newNodeY = newNodeY +50;
              	
              	
              	});
              
              	document.getElementById("changeLayout").onclick = function(){
              	
              		layoutIndex = layoutIndex+1;
              		
              		if (layoutIndex > layoutArray.length-1){
              		
              			layoutIndex=0;
              		}
              		
              		var index = layoutIndex;
              		
              		vis.layout(layoutArray[index]);
              	
              		
              	
              	};
              	
              	document.getElementById("changeThumbsup").onclick = function(){
              	
              		if (visual_style.nodes.image=="/Gemma/images/icons/thumbsup.png"){
              			visual_style.nodes.image="";
              		}
              		else{
              		visual_style.nodes.image="/Gemma/images/icons/thumbsup.png";
              		}
              		vis.visualStyle(visual_style);
              	
              		
              	
              	};
              
              
              });  
               //end vis.ready()  
   				 
   				     vis.draw({ network: networ_json, visualStyle: visual_style, layout: "Tree" }); 
                    
            },
            failure: function ( response, options ) {   
				alert(failed);
            },
            
            disableCaching: true
       });
        	
        	*/
        	
        	var dataSchemaJSON={
        		
        		nodes : [
        			{
        			name: 'label',
        			type: 'string'       			
        			},
        			{
        			name:'geneid',
        			type:'number'	
        			}
        		]
        		
        	};
        	
        	var dataJSON = this.constructDataJSON(this.queryGenes, this.knownGeneResults);
        	
        	var dataMsg = {
        		
        		dataSchema: dataSchemaJSON,
        		
        		data: dataJSON
        		
        		
        	}
        	
        	
        	var div_id = "cytoscapeweb";
                
                // you could also use other formats (e.g. GraphML) or grab the network data via AJAX
                    
                var networ_json = []    
                    
                if (networ_json.length == 0){
                	networ_json = dataMsg;
                }
                else{
                	//change this after testing to merge existing networ_json with new dataMsg network
                	networ_json = dataMsg;
                
                
                } 
                
                // initialization options
                var options = {
                    // where you have the Cytoscape Web SWF
                    swfPath: "/Gemma/scripts/cytoscape/swf/CytoscapeWeb",
                    // where you have the Flash installer SWF
                    flashInstallerPath: "/Gemma/scripts/cytoscape/swf/playerProductInstall"
                };
                
                
                var visual_style = {
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
                         labelHorizontalAnchor: "center"
                     },
                     edges: {
                         width: 1,
                         color: "#0B94B1"
                     }
                 };
                
                // init and draw
                var vis = new org.cytoscapeweb.Visualization(div_id, options);
                
                
                
              
              vis.ready(function(){
              
              
              	// Add the Context menu item for radial layout
                vis.addContextMenuItem("Radial layout", "none",
                function () {
                vis.layout("Radial");
                }).addContextMenuItem("Circle layout", "none",
                function () {
                  vis.layout("Circle");
                }).addContextMenuItem("Tree layout", "none",
                function () {
                  vis.layout("Tree");
                }); 
              
              
             	vis.addContextMenuItem("Some Option", "nodes", function(evt){
              	
              	var rootNode = evt.target;
         
             	// Get the first neighbors of that node:
             	var fNeighbors = vis.firstNeighbors([rootNode]);
             	var neighborNodes = fNeighbors.neighbors;
         
             	// Select the root node and its neighbors:
             	vis.select([rootNode]).select(neighborNodes);
              	
              	
              	});
              
              	//doubleclick
              	vis.addListener("dblclick", "nodes", function(event){
              	
              		getCoexpressionData(event.target.data.geneid);
              	
              		var target = event.target;
              		
              		var nodeId = "newNode"+newNodeSuffix+event.target.data.geneid;
              		newNodeSuffix = newNodeSuffix+1;
              		
              		var data = { id: nodeId,
                 	label: nodeId
                 	};
     
   					var node = vis.addNode(newNodeX, newNodeY, data, true);
   					newNodeX = newNodeX +50;
   					newNodeY = newNodeY +50;
              	
              	
              	});
              
              	document.getElementById("changeLayout").onclick = function(){
              	
              		layoutIndex = layoutIndex+1;
              		
              		if (layoutIndex > layoutArray.length-1){
              		
              			layoutIndex=0;
              		}
              		
              		var index = layoutIndex;
              		
              		vis.layout(layoutArray[index]);
              	
              		
              	
              	};
              	
              	document.getElementById("changeThumbsup").onclick = function(){
              	
              		if (visual_style.nodes.image=="/Gemma/images/icons/thumbsup.png"){
              			visual_style.nodes.image="";
              		}
              		else{
              		visual_style.nodes.image="/Gemma/images/icons/thumbsup.png";
              		}
              		vis.visualStyle(visual_style);
              	
              		
              	
              	};
              
              
              });  
               //end vis.ready()  
   				 
   				     vis.draw({ network: networ_json, visualStyle: visual_style, layout: "Tree" }); 
        	
        	
        	
        	
        	}

});


