


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
	console.log(dwr.util.toDescriptiveString(node, 7));
	createRestrictionGui(node);
};

//Recursive function that walks the node given to it and creates a coresponding form to fill in
var createRestrictionGui = function(node, indent) {

		var dh = Ext.DomHelper;  //allows html output

		if (indent === undefined){
			dh.overwrite("center-div", {html : ""});
			indent = "";
		}
	
		

		dh.append("center-div", {html : indent + "Details for: " + node.uri });


      
        var res = node.restrictions;
        if ( (res !== undefined) && (res !== null) && (res.size() > 0) ) {
        	
            for ( var id in res ) {
            	var restrictedOn = res[id].restrictionOn;
            	              
                if ( (res[id].restrictedTo !== undefined) && (res[id].restrictedTo !== null)) {	//is it a class restriction?
                    var restrictedTo = res[id].restrictedTo;
                    dh.append( "center-div", {html : indent + " Restricted To Slot to fill in: " + restrictedOn.label + " with a " + restrictedTo.term });
                    
                    if (restrictedTo.individuals !== undefined && restrictedTo.individuals !== null && restrictedTo.individuals.size() > 0){
                    	
	                    var store = new Ext.data.SimpleStore({
	        				fields: ['label','uri'],
	        				data : [restrictedTo.individuals.slice(0)]	//make a copy of the array
	    				});
	
					    var combo = new Ext.form.ComboBox({
					        store: store,
					        displayField:'label',
					        typeAhead: true,
					        mode: 'local',
					        triggerAction: 'all',
					        emptyText:'Select an individual',
					        selectOnFocus:true
					    });
					    
					    //Need to make a div to apply the combo box to. 
					 	dh.append("center-div", {tag: 'div', tag: 'input', type: 'text', id: 'individual', size:'20'});  
					    combo.applyTo('individual');
	                    
	                    createRestrictionGui( restrictedTo, "===>" + indent );
                    }
                                        
                } else if ( res[id].type !== undefined ) {
                    var restrictedTo = res[id].type
                    dh.append("center-div", {html : indent + " Primitive Type Slot to fill in: " + restrictedOn.label + " with a " + restrictedTo.term });
                    
                } else if ( res[id].cardinality !== undefined  ) {
                    // this will be rare.                  
                    var cardinality = res[id].cardinality;
                    var cardinalityType = res[id].cardinalityType;
                    dh.append("center-div",{ html: indent + " Cardinality Slot to fill in: " + restrictedOn.label + " with " + cardinalityType.term + " "
                            + cardinality + " things" });
                    // todo check range of the property (what 'things' should be) if specified.
                }
            }
        }
	    dh.append("center-div", {html : indent + "End of details for " + node.uri}) ;
    };


Ext.EventManager.onDocumentReady(Simple.init, Simple, true);

