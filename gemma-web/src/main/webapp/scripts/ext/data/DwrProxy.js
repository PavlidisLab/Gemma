
Ext.data.DWRProxy = function (dwrCall, config) {
	Ext.data.DWRProxy.superclass.constructor.call(this);
	this.dwrCall = dwrCall;
	if (config !== undefined) {
		this.pagingAndSort = (config.pagingAndSort !== undefined ? config.pagingAndSort : false);
		this.success = config.success;
		this.failure = config.failure;
	}
};


Ext.extend(Ext.data.DWRProxy, Ext.data.DataProxy, {
	
   /**
 	* @param params Array of parameters
 	* @param reader Reader implmentation that will be used to create the callback for DWR
 	* @param callback optional
 	* @param scope optional
 	* @param arg array of arguments that will be passed to read method of the Reader.
 	*/
	load:function (params, reader, callback, scope, arg) {
		if (this.fireEvent("beforeload", this, params) !== false) {
			var delegate = this.loadResponse.createDelegate(this, [reader, callback, scope, arg], true);
			if (params === undefined || params === null) {
				params = [];
			}
			params.push({callback : delegate, errorHandler : this.handleFailure });

			return this.dwrCall.apply(this, params);
		} else {
			callback.call(scope || this, null, arg, false);
		}
	}, 
	
	handleFailure: function(data, e) {
    	if (typeof this.failure == "function" ) {
    		failure();
    	}
		this.fireEvent("loadexception", this, null, data, e);
		if (typeof callback == "function") {
			callback.call(scope, null, arg, false);
		}
	},


	loadResponse:function (data, reader, callback, scope, arg) {
		var result;
		try {
			console.log(dwr.util.toDescriptiveString(data, 5));
			result = reader.read(data, arg);
		}
		catch (e) {
			handleFailure(data);
			return;
		}
		if (typeof callback == "function") {
			callback.call(scope, result, arg, true);
		}
	}, 
	
	update:function (dataSet) {
	}, 

	updateResponse:function (dataSet) {
	}
});

