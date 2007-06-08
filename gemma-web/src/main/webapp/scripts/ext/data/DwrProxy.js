
/**
 * 
 * @param {Object} dwrCall The method that will be called
 * @param {Object} config consisting of:
 * - {boolean} pagingAndSort
 * - {Array} baseParams, which are sent in dwr method calls as the first parameters. 
 */
Ext.data.DWRProxy = function (dwrCall, config) {
	Ext.data.DWRProxy.superclass.constructor.call(this);
	this.dwrCall = dwrCall;
	if (config !== undefined) {
		this.pagingAndSort = (config.pagingAndSort !== undefined ? config.pagingAndSort : false);
		this.baseParams = config.baseParams;
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
			var errorHandler = this.handleFailure.createDelegate(this, [], true);
			var callParams = [];

   		 	if ( params instanceof Array) {
				callParams = params;
			} else if ( params && params.push === undefined && this.pagingAndSort ) {
				callParams.push(params.start);
				callParams.push(params.limit);
				callParams.push(params.sort);
				callParams.push(params.dir);
			}
			
		 	// add baseparams to start of array
			if (this.baseParams !== undefined ) {
				for(var k = this.baseParams.length - 1; k >= 0; k--) {
					callParams.unshift(this.baseParams[k]);
				}
			}
			
			callParams.push({callback : delegate, errorHandler : errorHandler  });
			return this.dwrCall.apply(this, callParams);
		} else {
			callback.call(scope || this, null, arg, false);
		}
	}, 
	


	handleFailure : function(data, e) {
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
			handleFailure(data, e);
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

