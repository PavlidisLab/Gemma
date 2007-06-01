


//expose the main layout panels for use later. 
 var northPanel, southPanel, eastPanel, westPanel, centerPanel;
 
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

//The call back method for the dwr call
var displayRestrictionsPanel = function(node){
	console.log(dwr.util.toDescriptiveString(node, 10));
	createRestrictionGui(node);
};

//Recursive function that walks the node given to it and creates a coresponding form to fill in
var createRestrictionGui = function(node, indent, parentDivId) {

		var dh = Ext.DomHelper;  //allows html output

		if (indent === undefined){
			dh.overwrite("center-div", {html : ""});
			indent = "";
		}
		if (parentDivId === undefined) {
			parentDivId = "center-div";
		}
	
		

		//dh.append(parentDivId, {html : indent + "Details for: " + node.uri });


      
        var res = node.restrictions;
        if ( (res !== undefined) && (res !== null) && (res.size() > 0) ) {
        	
            for ( var i = 0, len = res.length; i < len; i++ ) {
            	var restrictedOn = res[i].restrictionOn;
            	var restrictedTo = res[i].restrictedTo;
            	//make a nested div for adding to.
            	var divId = (Math.random() * 100000).toFixed();   
            	dh.append(parentDivId, {tag: 'div', id: divId});
            	
            	if (restrictedOn.type !== undefined ) {	//Primitive Type
                    var primitiveRestrictedTo = restrictedOn.type
                    dh.append(parentDivId, {html : indent + " Primitive Type Slot to fill in: " + restrictedOn.label + " with a " + primitiveRestrictedTo.value });
            	} else if ( (restrictedTo !== undefined) && (restrictedTo !== null)) {	//is it a class restriction?
                                       
                    if (restrictedTo.restrictions === undefined || restrictedTo.restrictions === null || restrictedTo.restrictions.size() === 0){	// ie) we are at a leaf node so display gui
                        dh.append(divId, {html : indent + " Restricted To Slot to fill in: " + restrictedOn.label + " with a " + restrictedTo.term });
 
	                    var simple = createForm();	
					                       
    	                if (restrictedTo.individuals !== undefined && restrictedTo.individuals !== null && restrictedTo.individuals.size() > 0){  //are there examples?
        		            	simple.add(createComboBox(restrictedTo.individuals));                                 	                    
                	    }
                    
	                    simple.render(divId);
                    } else{        //Not a leaf node. recurse down another level
                        dh.append(divId, {html : indent + " The " + restrictedOn.label + " restriction has " + restrictedTo.restrictions.size()+ " slots to fill in"});
    	            	createRestrictionGui( restrictedTo, "&nbsp;&nbsp;&nbsp;&nbsp;" + indent, divId );
                   }                
                    
                } else if ( res[i].cardinality !== undefined  ) { //Cardinality Type
                    // this will be rare.                  
                    var cardinality = res[i].cardinality;
                    var cardinalityType = res[i].cardinalityType;
                    dh.append(parentDivId,{ html: indent + " Cardinality Slot to fill in: " + restrictedOn.label + " with " + cardinalityType.term + " "
                            + cardinality + " things" });
                    // todo check range of the property (what 'things' should be) if specified.
                } else{
                	  dh.append(parentDivId,{ html: indent + " Error: This should not happen" });                	
                }
            }
        }
	    //dh.append(parentDivId, {html : indent + "End of details for " + node.uri}) ;
    };

var createComboBox = function(individuals){
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
					    
					    return combo;				
					    
	
};

var createForm = function(){
	var simple = new Ext.form.Form({
					        labelWidth: 75, // label settings here cascade unless overridden
					        url:'save-form.php'
					    });
					    simple.add(
					        new Ext.form.TextField({
					            fieldLabel: 'Lookup',
					            name: 'lookup',
					            width:175,
					            allowBlank:true
					        }),
					
					        new Ext.form.TextField({
					            fieldLabel: 'custom',
					            name: 'custom',
					            width:175
					        })										
					    );

					    //simple.addButton('Save');
					    //simple.addButton('Cancel');		
					   
					   return simple;		
}

Ext.EventManager.onDocumentReady(Simple.init, Simple, true);

