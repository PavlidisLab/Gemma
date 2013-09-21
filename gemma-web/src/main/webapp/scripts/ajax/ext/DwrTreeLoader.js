Ext.tree.DwrTreeLoader = function(config) {
   // Ext.tree.DwrTreeLoader.superclass.constructor.call(this);
   this.baseParams = {};
   this.requestMethod = "POST";
   Ext.apply(this, config);

   this.addEvents({
         "beforeload" : true,
         "load" : true,
         "loadexception" : true
      });
};

Ext.extend(Ext.tree.DwrTreeLoader, Ext.tree.TreeLoader, {

      requestData : function(node, callback) {
         if (this.fireEvent("beforeload", this, node, callback) !== false) {
            var args = [];
            args.push(node);
            var cb = {
               success : this.read,
               failure : this.handleFailure,
               scope : this,
               argument : {
                  callback : callback,
                  node : node
               }
            };
            var proxy = new Ext.data.DWRProxy(this.dataUrl, cb);
            this.transId = proxy.load(null, this, "foo", this, args);

         } else {
            // if the load is cancelled, make sure we notify
            // the node that we are done
            if (typeof callback == "function") {
               callback();
            }
         }
      },

      read : function(data, attr) {
         var node = attr[0];
         this.transId = false;
         this.processResponse(data, node, "foo"); // no callback.
         this.fireEvent("load", this, node, data);
         node.fireEvent("load", this, data);
         // return node; // need to complete the Reader interface but this isn't really used?
      },

      abort : function() {
         if (this.isLoading()) {
            // FIXME do something
            // Ext.lib.Ajax.abort(this.transId);
         }
      },

      createNode : function(data) {
         if (this.applyLoader !== false) {
            data.loader = this;
         }
         if (data.uiProvider !== undefined && typeof data.uiProvider == 'string') {
            // I'm not sure what valid settings of this would be.
            data.uiProvider = this.uiProviders[data.uiProvider] || eval(data.uiProvider);
         }

         var n = (data.leaf ? new Ext.tree.TreeNode(data) : new Ext.tree.TreeNode(data));

         if (n.attributes.children !== undefined) {
            for (var i = 0, len = n.attributes.children.length; i < len; i++) {
               var newnode = this.createNode(n.attributes.children[i]);
               n.appendChild(newnode);
            }
         }

         return n;

      },

      processResponse : function(data, node, callback) {
         try {
            for (var i = 0, len = data.length; i < len; i++) {
               var newnode = this.createNode(data[i]);
               if (newnode) {
                  node.appendChild(newnode);
               }
            }
            node.loadComplete(true, true, "foo");
            if (typeof callback == "function") {
               callback(this, node);
            }
         } catch (e) {
            this.handleFailure(data);
         }
      },

      handleFailure : function(response) {
         this.transId = false;
         this.fireEvent("loadexception", this, this.node, response);
         // if(typeof a.callback == "function"){
         // a.callback(this, a.node);
         // }
      }

   });