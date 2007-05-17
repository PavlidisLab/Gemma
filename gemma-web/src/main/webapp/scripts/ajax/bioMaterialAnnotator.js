Simple = function() {
  var northPanel, southPanel, eastPanel, westPanel, centerPanel;
    return {
        init : function() {
            var mainLayout = new Ext.BorderLayout("layout", {
                north: { 
                    split: true, initialSize: 300 
                }, 
                south: { 
                    split: true, initialSize: 50 
                }, 
                east: { 
                    split: true, initialSize: 10 
                }, 
                west: { 
                    split: true, initialSize: 10 
                }, 
                center: { 
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
			
			initGrid( );
		}
		
		
		
    };
}();

var showbms = function( id ) {
	var ids =  id.split(",");
	// note how we pass the new array in directly, without wraping it in an object first.
	ds.load({params:[ids]});
};

var ds;

var initGrid = function(div) {
	var	recordType = Ext.data.Record.create([
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
			{header: "Name", width: 100, dataIndex:"name"},
			{header: "Description",  width: 130, dataIndex:"description"} 
			]);
	cm.defaultSortable = true;

	grid = new Ext.grid.Grid("center-div", {autoSizeColumns: true, ds:ds, cm:cm, loadMask: true });
	grid.render();
	
	
	var id = dwr.util.getValue("cslist");
	showbms(id);
};



Ext.EventManager.onDocumentReady(Simple.init, Simple, true);