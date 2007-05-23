


//expose the main layout panels for use later. 
 var northPanel, southPanel, eastPanel, westPanel, centerPanel;
 
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
                   split: false, initialSize: 300
               },
               west: {
                   split: false, initialSize: 0
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
               fitToFrame: true, closable: false
           }));
           mainLayout.add('west', westPanel = new Ext.ContentPanel('west-div', {
               fitToFrame: true, closable: false
           }));
           mainLayout.add('center', centerPanel = new Ext.ContentPanel('center-div', {
               fitToFrame: true
           }));
           mainLayout.endUpdate();
                       mainLayout.getRegion('west').hide();
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

   var tree = new Ext.tree.TreePanel("center-div", {
       animate:true,
       loader: new Ext.tree.DwrTreeLoader({dataUrl:MgedOntologyService.getBioMaterialTerms})
   });


 	var read = function( data, attr ){
    	var node = attr[0];
        this.transId = false;
        displayRestrictionsPanel(node);
        //this.processResponse(data, node, "foo" ); // no callback.
        //this.fireEvent("load", this, node, data);
        //node.fireEvent("load", this, data);
       // return node; // need to complete the Reader interface but this isn't really used?
    }
    
   tree.on('click', function(node){
                       //MgedOntologyService.getTerm{params:[node.id]});
                       
                        var args = [];
            			args.push(node.id);
            			var cb = {
                				success: this.read,
				                failure: this.handleFailure,
                				scope: this,
        						argument: {callback: read, node: node}
            				 };
                       var proxy = new Ext.data.DWRProxy( MgedOntologyService.getTerm, cb);
           			   this.transId = proxy.load(args, this, "foo", this, args );
           			   
          });


   // set the root node
   var root = new Ext.tree.AsyncTreeNode({
       text: 'Top of the tree',
       draggable:false,
       allowChildre:true,
       id:'root'
   });
   tree.setRootNode(root);

	var read = 

   // render the tree
   tree.render();
   root.expand();


};

var showDetails = function(node) {
       dds.load({params:[{id: node}]});
};


var displayRestrictionsPanel = function(node) {

		var dh = Ext.DomHelper;
		dh.overwrite("east-div", {tag : 'h2', html : "Details for: " + node.id + " on " });
		//dh.append("east-div", {tag : 'ul', children : [
		//	{tag : 'li' , html: "Sequence: " + seqName}
		//]});



};



var individualDS;

var initIndividualsGrid = function(div) {
       var     recordIndividualType = Ext.data.Record.create([
                       {name:"id", type:"int"},
                       {name:"restrictedTo" },
                       {name:"restrictionOn"}
               ]);



       individualDS = new Ext.data.Store(
               {
                       proxy:new Ext.data.DWRProxy(MgedOntologyService.getTermIndividuals),
                       reader:new Ext.data.ListRangeReader({id:"id"}, recordIndividualType),
                       remoteSort:false
                       //sortInfo:{field:'uri'}
               });

       var individualCM = new Ext.grid.ColumnModel([
                       {header: "Label", width: 100, dataIndex:"label"},
                       {header: "Restricted To",  width: 130, dataIndex:"restrictedTo", renderer: restrictedToRenderer},
                       {header: "Restriction On",  width: 130, dataIndex:"restrictionOn", renderer: restritionOn}

                       ]);

       var restrictedToRenderer =  function(arg){
               return "im here";
       };

       var restritionOn =  function(arg){
               return arg.uri;
       };

       //individualCM.defaultSortable = true;

       individualGrid = new Ext.grid.Grid("west-div", {autoSizeColumns: true, ds:individualDS, cm:individualCM, loadMask: true });
       individualGrid.render();

};


Ext.EventManager.onDocumentReady(Simple.init, Simple, true);

