Ext.tree.DwrTreeLoader = function(config){
    this.baseParams = {};
    this.requestMethod = "POST";
    Ext.apply(this, config);
    
    this.events = {
        "beforeload" : true,
        "load" : true,
        "loadexception" : true
    };
};

Ext.extend(Ext.tree.DwrTreeLoader, Ext.tree.TreeLoader, {

    requestData : function(node, callback){
        if(this.fireEvent("beforeload", this, node, callback) !== false){
            var params = this.getParams(node);
            var cb = {
                success: this.handleResponse,
                failure: this.handleFailure,
                scope: this,
        		argument: {callback: callback, node: node}
            };
           var proxy = new Ext.data.DWRProxy( this.dataUrl, cb);
           proxy.load(params,this.handleResponse, this.handleResponse, this, [1,1,true]);
            
         ds = new Ext.data.Store({
		    proxy: new Ext.data.DWRProxy(this.dataUrl),
		    reader: new Ext.data.ListRangeReader( 
					{id:'id', totalProperty:'totalSize'}, recordType),
		    remoteSort: true
		  });		
		 ds.on("load", function () {});		
         ds.load({params:{start:0, limit:22}});
            
        }else{
            // if the load is cancelled, make sure we notify 
            // the node that we are done
            if(typeof callback == "function"){
                callback();
            }
        }
    },
    
     createNode : function(attr){
        if(this.applyLoader !== false){
            attr.loader = this;
        }
        if(typeof attr.uiProvider == 'string'){
           attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
        }
        return(attr.leaf ?
                        new Ext.tree.TreeNode(attr) : 
                        new Ext.tree.AsyncTreeNode(attr));  
    },
    
    processResponse : function(response, node, callback){
        var json = response.responseText;
        try {
            var o = eval("("+json+")");
	        for(var i = 0, len = o.length; i < len; i++){
                var n = this.createNode(o[i]);
                if(n){
                    node.appendChild(n); 
                }
	        }
	        if(typeof callback == "function"){
                callback(this, node);
            }
        }catch(e){
            this.handleFailure(response);
        }
    }
});