//===============================
//
//Tool bar for selecting available Factors and assigning Factor values to selected BioMaterials
//================================

//member variables
var saveButton, expFactorsCB, factorValueCB, bmGrid;	//gui components
var eeID, clazz;
var factorValueDS, bmDS;							//Datastores behind gui components
var bioMaterialList, selectedFactorId, selectedFactorValueId;	//what is selected by user
var bmGridRefresh;									//methods

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
					        selectOnFocus:true,
					        editable: false
					    });	
					    					   
	
		    			var comboHandler = function(field,record,index){
					    	
							//TODO: When a factor is selected refresh the bm table to display the selected factor
							//update the factor value combo box with the factor values associated with the selected factor

							selectedFactorId = record.id;
							factorValueDS.reload({params:[{id:selectedFactorId, classDelegatingFor:"FactorValueObject"}]});
							factorValueCB.reset();
							bmGridRefresh(selectedFactorId);
							saveButton.disable();					    								      						 					    	                        
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
					        selectOnFocus:true,
					        editable: false
					    });	
					    			
					    var comboHandler = function(field,record,index){
					    	
							//TODO: When a factor is selected refresh the bm table to display the selected factor
							//update the factor value combo box with the factor values associated with the selected factor

							selectedFactorValueId = record.id;
							saveButton.enable();									    								      						 					    	                        
    	                };
    	                	
	
				  	 	combo.on('select', comboHandler);		   
	    	                						    
					    return combo;
}

var saveHandler = function(){

	//Use for debugging. 
	//console.log(dwr.util.toDescriptiveString(vocabC,10))

	if ((bioMaterialList === undefined) || (bioMaterialList.length === 0)){
		alert("Please select a biomaterial");
		return;
	}
	
	BioMaterialController.addFactorValueTo(bioMaterialList, {id:selectedFactorValueId, classDelegatingFor:"FactorValueObject"}, bmGridRefresh );
	
	
}


//=================================================
//
//	Biomateril table with a single experimental factor
//=================================================

bmGridRefresh = function(){
	
	bmDS.reload({params:[{id:eeID, classDelegatingFor:"expressionExperimentID"},{id:selectedFactorId, classDelegatingFor: "FactorID"}]});	
	bmGrid.getView().refresh(true);	
	
}

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

       bmGrid = new Ext.grid.Grid(div, {autoSizeColumns: true,
       							 ds:bmDS,
       							 cm:cm,
       							 loadMask: true });
       
       var gridClickHandler = function(bmGrid, rowIndex, event){
       		//Get the ids of the selected biomaterials and put them in BiomatierialList
	       	var selected = bmGrid.getSelectionModel().getSelections();	
	   
	    	bioMaterialList = [];
	    	for(var index=0; index<selected.length; index++) {	    		
	    		bioMaterialList.push(selected[index].id);
	    	}  	
       	
       }
       
       bmGrid.on("rowclick", gridClickHandler);
       
       bmGrid.render();
	
};




Ext.onReady(function() {

	eeID = dwr.util.getValue("expressionExperimentID");
	//the mged combo box 
	expFactorsCB = createFactorComboBox();   
	
	//the lookup combobox
	factorValueCB = createFactorValueComboBox();		

	//The tool bar. 
	var simpleTB = new Ext.Toolbar("eDesign");
	
	simpleTB.addField(expFactorsCB);
	simpleTB.addSpacer();
	simpleTB.addField(factorValueCB);
	simpleTB.addSpacer();
	saveButton = simpleTB.addButton({text: 'assign',
						tooltip: 'assigns the selected Factor Value to the selected BioMaterials',								  
						handler: saveHandler,
						disabled: true
					});
					
					
	initBioMaterialGrid("bmGrid");					
	
});