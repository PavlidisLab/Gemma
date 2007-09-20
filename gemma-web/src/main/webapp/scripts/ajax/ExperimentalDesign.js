//===============================
//
//Tool bar for selecting available Factors and assigning Factor values to selected BioMaterials
//================================

//member variables
var deleteButton;
var saveButton;
var eeID, clazz;
var factorValueDS;
var bmDS;
var bioMaterialList;
var grid;

var createFactorComboBox = function(terms){				
				
			
	var     recordType = Ext.data.Record.create([
					   {name:"id", type:"int"},
                       {name:"factorValue", type:"string"},
                       {name:"description", type:"string"},
                       {name:"category", type:"string"}
               ]);
									
                    	
                var factorDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getExperimentalFactors),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'factorValue'}
               });
               
				factorDS.load({params:[{id:eeID, classDelegatingFor:"FactorValueObject"}]});
						
					    var combo = new Ext.form.ComboBox({	
					    	width: 200,				    	
					        store: factorDS,
					        fieldLabel: 'Factors',
					        displayField:'factorValue',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Available Factors',
					        selectOnFocus:true
					    });	
					    					   
	
		    			var comboHandler = function(field,record,index){
					    	
							//TODO: When a factor is selected refresh the bm table to display the selected factor
							//update the factor value combo box with the factor values associated with the selected factor
							
							factorValueDS.load({params:[{id:record.id, classDelegatingFor:"FactorValueObject"}]});
							bmDS.load({params:[{id:eeID, classDelegatingFor:"expressionExperimentID"},{id:record.id, classDelegatingFor: "FactorID"}]});
							grid.getView().refresh(true);
					    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);
					    
					    return combo;	
	
}

var createFactorValueComboBox = function(){
	
	
		var     recordType = Ext.data.Record.create([
				   		{name:"id", type:"int"},
                       	{name:"factorValue", type:"string"},
                       	{name:"description", type:"string"},
                       	{name:"category", type:"string"}
               ]);
									
                    	
                factorValueDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(ExpressionExperimentController.getFactorValues),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'factorValue'}
               });
               
				
						
					    var combo = new Ext.form.ComboBox({	
					    	width: 200,				    	
					        store: factorValueDS,
					        fieldLabel: 'Factor Values',
					        displayField:'factorValue',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Factor Values',
					        selectOnFocus:true
					    });	
					    					   
	    	                						    
					    return combo;
}

var saveHandler = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))

	//TODO: updating the selected biomaterials factor to have the value of the selected factor value
	
}

var deleteHandler = function(){
		
	//TODO: remove the current factor value and factor??? from the selected biomaterials. 
	
}



//=================================================
//
//	Biomateril table with a single experimental factor
//=================================================


var initBioMaterialGrid = function(div) {
       var     recordType = Ext.data.Record.create([
                       {name:"id", type:"int"},
                       {name:"name", type:"string"},
                       {name:"description", type:"string"},
                       {name:"factorValue", type:"string"}
                       
                       
               ]);


       bmDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(BioMaterialController.getBioMaterialsForEEWithFactor),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'name'}
               });

       var cm = new Ext.grid.ColumnModel([
                       {header: "Name", width: 50, dataIndex:"name"},
                       {header: "Description",  width: 100, dataIndex:"description"}, 
                       {header: "Factor Value",  width: 50, dataIndex:"factorValue"}
                       
                       ]);
       cm.defaultSortable = true;

       grid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:bmDS,
       							 cm:cm,
       							 loadMask: true });
       
       var gridClickHandler = function(grid, rowIndex, event){
       		//Get the ids of the selected biomaterials and put them in BiomatierialList
	       	var selected = grid.getSelectionModel().getSelections();	
	   
	    	bioMaterialList = [];
	    	for(var index=0; index<selected.length; index++) {	    		
	    		bioMaterialList.push(selected[index].id);
	    	}  	
       	
       }
       
       grid.on("rowclick", gridClickHandler);
       
       grid.render();
	
};




Ext.onReady(function() {

	eeID = dwr.util.getValue("expressionExperimentID");
	//the mged combo box 
	var expFactorsCB = createFactorComboBox();   
	
	//the lookup combobox
	var factorValueCB = createFactorValueComboBox();			

	//The tool bar. 
	var simpleTB = new Ext.Toolbar("eDesign");
	
	simpleTB.addField(expFactorsCB);
	simpleTB.addSpacer();
	simpleTB.addField(factorValueCB);
	simpleTB.addSpacer();
	saveButton = simpleTB.addButton({text: 'save',
						tooltip: 'updates the selected biomaterial with the chosen factor value',								  
						handler: saveHandler,
						disabled: true
					});
	simpleTB.addSeparator();
	deleteButton = simpleTB.addButton({text: 'delete',
						tooltip: 'Removes the desired factor from the selected biomaterial',								
						handler: deleteHandler,
						disabled: true						
					});
					
					
	initBioMaterialGrid("bmGrid");					
	
});