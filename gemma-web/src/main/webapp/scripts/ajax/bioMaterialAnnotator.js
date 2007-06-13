


//expose the main layout panels for use later. 
 var northPanel, southPanel, eastPanel, westPanel, centerPanel;


onSubmit = function() {
	
}



//The simple function will be called when the document is finished loading.  See event at bottem of file 
Simple = function() {

   return {
       init : function() {
           var mainLayout = new Ext.BorderLayout("layout", {
               north: {
                   split: false, initialSize: 30
               },
               south: {
                   split: false, initialSize: 400
               },
               east: {
                   split: true, initialSize: 0
               },
               west: {
                   split: true, initialSize: 300
               },
               center: { split: true, initialSize: 80
               }
           });
           mainLayout.beginUpdate();
           mainLayout.add('north', northPanel = new Ext.ContentPanel('north-div', { 
              fitToFrame: true, closable: false, autoScroll: true
           }));
           mainLayout.add('south', southPanel = new Ext.ContentPanel('south-div', {
               fitToFrame: true, closable: false
           }));
           mainLayout.add('east', eastPanel = new Ext.ContentPanel('east-div', {
               fitToFrame: true, closable: false, autoScroll: true
           }));
           mainLayout.add('west', westPanel = new Ext.ContentPanel('west-div', {
               fitToFrame: true, closable: false, autoScroll: true
           }));
           mainLayout.add('center', centerPanel = new Ext.ContentPanel('center-div', {
               fitToFrame: true , autoScroll: true
           }));
           mainLayout.endUpdate();
                       mainLayout.getRegion('east').hide();
                       mainLayout.getRegion('north').hide();
                       initBioMaterialGrid( );
                       initTree( );                     
               }
   };
}();

var showbms = function( id ) {
       var ids =  id.split(",");
       // note how we pass the new array in directly, without wraping it in an object first.
       ds.load({params:[ids]});
};

var ds;

var initBioMaterialGrid = function(div) {
       var     recordType = Ext.data.Record.create([
                       {name:"id", type:"int"},
                       {name:"name", type:"string"},
                       {name:"description", type:"string"}
               ]);


       ds = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(BioMaterialController.getBioMaterials),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordType),
                       remoteSort:false,
                       sortInfo:{field:'name'}
               });

       var cm = new Ext.grid.ColumnModel([
                       {header: "Name", width: 50, dataIndex:"name"},
                       {header: "Description",  width: 80, dataIndex:"description"}
                       ]);
       cm.defaultSortable = true;

       grid = new Ext.grid.Grid("south-div", {autoSizeColumns: true, ds:ds,cm:cm, loadMask: true });
       grid.render();


       var id = dwr.util.getValue("cslist");
       showbms(id);
};

var initTree = function(div){

   var tree = new Ext.tree.TreePanel("west-div", {
       animate:true,
       loader: new Ext.tree.DwrTreeLoader({dataUrl:MgedOntologyService.getBioMaterialTerms})
   });


    
   tree.on('click', function(node){                       
                   MgedOntologyService.getTerm(node.id,displayRestrictionsPanel);                                  			   
          });


   // set the root node
   var root = new Ext.tree.AsyncTreeNode({
       text: 'Top of the tree',
       draggable:false,
       allowChildre:true,
       id:'root'
   });
   tree.setRootNode(root);


   // render the tree
   tree.render();
   root.expand();


};

var vocabC; 

//The call back method for the dwr call
var displayRestrictionsPanel = function(node){
	console.log(dwr.util.toDescriptiveString(node, 10));

	vocabC = { termUri : node.uri,properties : [] };		
	createRestrictionGui(node, vocabC);
	
	var saveButton = new Ext.Button("center-div", {text : 'save'});
	saveButton.on("click", saveHandler);

};

var saveHandler = function(event){
	console.log(dwr.util.toDescriptiveString(vocabC,10))
	//MgedOntologyService.saveTerm(vocabC);
		
}

//Recursive function that walks the node given to it and creates a coresponding form to fill in
var createRestrictionGui = function(node, vc, indent, parentDivId) {

		var dh = Ext.DomHelper;  //allows html output

		if (indent === undefined){
			dh.overwrite("center-div", {html : ""});
			indent = 0;			
		}
		if (parentDivId === undefined) {
			parentDivId = "center-div";
			
		}
	
		

		dh.append(parentDivId, {html : node.term+ ": " + node.comment});


      
        var res = node.restrictions;
        if ( (res !== undefined) && (res !== null) && (res.size() > 0) ) {
        	
            for ( var i = 0, len = res.length; i < len; i++ ) {
            	var restrictedOn = res[i].restrictionOn;
            	var restrictedTo = res[i].restrictedTo;
            	//make a nested div for adding to.
            	var divId = (Math.random() * 100000).toFixed();   
            	dh.append(parentDivId, {tag: 'div', id: divId, style : "border-width:thin;border-style:dotted;padding:5px;margin:5px;"});
                dh.append(divId, {html : restrictedOn.label});
 
            	
            	if (restrictedOn.type !== undefined ) {	//Primitive Type
 					
                    	var simple = new Ext.form.Form({
					        labelWidth: 75, // label settings here cascade unless overridden
					        url:'save-form.php'
					    });
					    
					    var handler = createPrimitiveTypeHandler(restrictedOn, vc);
//					     var handler = function(field) {
//					     	console.log(dwr.util.toDescriptiveString(vc,10));
//					     	var newVc = {termUri : primitiveRestrictedOn.uri, data : field.getValue(), type : primitiveRestrictedTo};
//					     	
//					     	if (vc.properties === undefined){
//    	                	 	vc.properties = [];    	                	 	
//    	                	}
//					     	
//					   		vc.properties.push(newVc); 
//					   };
					    var valueField = new Ext.form.TextField({
					            fieldLabel: restrictedOn.label ,
					            name: 'hasValue',
					            width:175,
					            allowBlank:false,					           
					        });
					     
					  //valueField.on.createDelegate(this,['valid', handler],true);					       
					  valueField.on('valid', handler);
					        
					   simple.add(valueField);
                       dh.append(divId, {tag: 'h3', html : 'Value:' });
 
					   simple.render(divId);
					   
					   
					   
            	} else if ( (restrictedTo !== undefined) && (restrictedTo !== null)) {	//Class restriction
                                       
                    if (restrictedTo.restrictions === undefined || restrictedTo.restrictions === null || restrictedTo.restrictions.size() === 0){	// ie) we are at a leaf node so display gui
                        dh.append(divId, {tag: 'h3', html : "Create an instance of: " +restrictedTo.term });
 					 
	                    var simple = createForm(restrictedOn,vc);	
					                       
    	                if (restrictedTo.individuals !== undefined && restrictedTo.individuals !== null && restrictedTo.individuals.size() > 0){  //are there examples?
    	                		var combo = createComboBox(restrictedOn, restrictedTo.individuals, vc);    	                
        		            	simple.column({width:285},combo);                                 	                    
                	    }
                    
	                    simple.render(divId);
                    } else{        //Not a leaf node. recurse down another level
                        var vcChild = { termUri : restrictedOn.uri, object : { termUri : restrictedTo.uri }, properties : []};
                        vc.properties.push(vcChild);
    	            	createRestrictionGui( restrictedTo, vcChild.object, indent + 3, divId );
                   }                
                    
                } else if ( res[i].cardinality !== undefined  ) { //Cardinality Type
                    // this will be rare.                  
                    var cardinality = res[i].cardinality;
                    var cardinalityType = res[i].cardinalityType;
                    dh.append(parentDivId,{ html: " Cardinality Slot to fill in: " + restrictedOn.label + " with " + cardinalityType.term + " "
                            + cardinality + " things" });
                    // todo check range of the property (what 'things' should be) if specified.
                } else{
                	  dh.append(parentDivId,{ html: " Error: Not a typical type.  " + restrictedOn.label});                	
                }
            }
        }
        else{//No restrictions must be a leaf node. 
			
			//Create new div
        	var divId = (Math.random() * 100000).toFixed();   
           	dh.append(parentDivId, {tag: 'div', id: divId, style : "border-width:thin;border-style:dotted;padding:5px;margin:5px;"});
           	dh.append(divId, {tag: 'h3', html :  "Create an instance of: " + node.term });      
           	var simple = createForm(restrictedOn,vc);	
           	
           	//If there already exisit individuals display them in a drop down box so the user can select one.
            if ((node.individuals !== undefined) && (node.individuals !== null) && (node.individuals.size() > 0))
            	simple.column({width:285},createComboBox(restrictedOn, node.individuals)); 	   	    
    		
        	simple.render(divId);
        }
	};

var createComboBox = function(subject, individuals, vc){
	     	var recordType = Ext.data.Record.create([
							{name:"label", type : "string" }, 
							{name:"uri", type: "string" } 
						]);
						
						//convert the arry of objects into an array of records
						var records = [];
						for(var i = 0, len = individuals.length; i < len; i++) {
							records.push(new recordType(individuals[i]));
						}
                    	
	                    var store = new Ext.data.Store({recordType : recordType} );
						store.add(records);
						
					    var combo = new Ext.form.ComboBox({
					    	fieldLabel: 'pick one',
					        store: store,
					        displayField:'label',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Select an individual',
					        selectOnFocus:true
					    });
					    
					    var comboHandler = function(field,record,index){
					    	console.log(dwr.util.toDescriptiveString(vc,10));
					    	var vcChild = { termUri : subject.uri, object : record.data};
					    	
					    	if (vc.properties === undefined){
    	                	 	vc.properties = [];    	                	 	
    	                	}
    	                	vc.properties.push(vcChild); 
                        
    	                };
    	                		
    	                combo.on('select', comboHandler);
					    
					    return combo;				
					    
	
};

var createForm = function(subject, vc){

	var lookUp = 	new Ext.form.TextField({
            fieldLabel: 'Lookup',
            name: 'lookup',
            width:150,
            allowBlank:true	});
	
	var custom =   new Ext.form.TextField({
            fieldLabel: 'custom',
            name: 'custom',
            width:150 });

	var customHandler = function(field){
							console.log(dwr.util.toDescriptiveString(vc,10));
							var newVC = {termUri : subject, object : {value: field.getValue()}};
    	                	if (vc.properties === undefined){
    	                	 	vc.properties = [];    	                	 	
    	                	}
    	                	//todo:  need to do something about clicking back and forth and creating more objects than necessary
    	                	vc.properties.push(newVC); 
    	                };
    	                		
    	       custom.on('valid', customHandler);

	var simple = new Ext.form.Form({
		        labelWidth: 50, // label settings here cascade unless overridden
    });
					    
    simple.column(
    	{width:250},
        lookUp
        );

	simple.column(
		{width:250},
		custom								
    );
    
    

    //simple.addButton('Save');
    //simple.addButton('Cancel');		
   
   return simple;		
}

//Do this for scope reasons. 
//By creating a function on the fly the scope of the restrictions will be fixed for the handler.
var createPrimitiveTypeHandler = function(restrictedOn, vc){

	return 	function(field) {
					     	
					     	var newVc = {termUri : restrictedOn.uri, data : field.getValue(), type : restrictedOn.type};
					     	
					     	if (vc.properties === undefined){
    	                	 	vc.properties = [];    	                	 	
    	                	}
					     	
					   		vc.properties.push(newVc); 
					   		console.log(dwr.util.toDescriptiveString(vc,10));
			}
};


Ext.EventManager.onDocumentReady(Simple.init, Simple, true);

