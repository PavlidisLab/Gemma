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
    	this.node = node;
        if(this.fireEvent("beforeload", this, node, callback) !== false){
            var params = this.getParams(node);
            var cb = {
                success: this.read,
                failure: this.handleFailure,
                scope: this,
        		argument: {callback: callback, node: node}
            };
           var proxy = new Ext.data.DWRProxy( this.dataUrl, cb);
           this.transId = proxy.load(params, this, "foo", this );
            
        } else {
            // if the load is cancelled, make sure we notify 
            // the node that we are done
            if(typeof callback == "function"){
                callback();
            }
        }
    },
    
    read : function( data ){
        this.transId = false;
        this.processResponse(data, this.node, "foo" ); // no callback.
        this.fireEvent("load", this, this.node, data);
        return this.node; // need to complete the Reader interface but this isn't really used?
    },
    
    abort : function(){
        if(this.isLoading()){
        // FIXME do something 
        //    Ext.lib.Ajax.abort(this.transId);
        }
    },
    
    createNode : function(attr){
        if(this.applyLoader !== false){
            attr.loader = this;
        }
        if(typeof attr.uiProvider == 'string'){
        	// I'm not sure what valid settings of this would be.
           attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
        }
        return(attr.leaf ?
                        new Ext.tree.TreeNode(attr) : 
                        new Ext.tree.AsyncTreeNode(attr));  
    },
 
    processResponse : function(data, node, callback){
        try {
        	node.attributes.children = [];
	        for(var i = 0, len = data.length; i < len; i++){
	        	newnode =  this.createNode(data[i]);
               node.appendChild( newnode );
               node.attributes.children[i] = newnode; // ??
	        }
	        if(typeof callback == "function"){
                callback(this, node);
            }
        }catch(e){
            this.handleFailure(data);
        }
    },
    
    
     handleFailure : function(response){
        this.transId = false;
        this.fireEvent("loadexception", this, this.node, response);
//        if(typeof a.callback == "function"){
//            a.callback(this, a.node);
//        }
    }
    
});