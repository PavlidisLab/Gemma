var Heatmap = function() {

	var MAX_LABEL_LENGTH_PIXELS = 80;
	var MAX_LABEL_LENGTH_CHAR = 12;
	var MIN_BOX_WIDTH = 2;
	var CLIP = 3;
	var NAN_COLOR = "grey";
	var SHOW_LABEL_MIN_SIZE = 5;
	var MIN_BOX_HEIGHT = 2;
	var MAX_BOX_HEIGHT = 16;
	var TRIM = 10;
	
	//TODO put constants in config object so they can programtically changed on the fly
	var DEFAULT_CONFIG = {label : false, //shows labels at end of row
						  useFixedBoxHeight : true	//Height of each row defaults to 12, setting to false will try calculate row hight to fit in given container
						  };

	var COLOR_4 = ["black", "red", "orange", "yellow", "white"];

	//black-red-orange-yellow-white	
	var COLOR_16 = ["rgb(0, 0, 0)","rgb(32, 0, 0)", "rgb(64, 0, 0)", "rgb(96, 0, 0)", 
	"rgb(128, 0, 0)","rgb(159, 32, 0)", "rgb(191, 64, 0)", "rgb(223, 96, 0)", 
	"rgb(255, 128, 0)", "rgb(255, 159, 32)", "rgb(255, 191, 64)", "rgb(255, 223, 96)",
	"rgb(255, 255, 128)", "rgb(255, 255, 159)", "rgb(255, 255, 191)" , "rgb(255, 255, 223)", "rgb(255, 255, 255)"];
	
	
	function HeatMap(container, data, config) {
				
		if (!config){
			config = DEFAULT_CONFIG;	 
		}
		drawMap(data, container, COLOR_16, config);
	
	
		//Creates 1 canvas per row of the heat map
	function drawMap(vectorObjs,target, colors, config) {

//Attempt to put in load mask failed.  worked but never saw spinner		
//		Ext.apply(this, {
//			loadMask : new Ext.LoadMask(target, {
//				msg : "Constructing HeatMap..."
//			})
//		});

		//this.loadMask.show();
		
		//Get dimensions of target to determine box size in heat map
			var binSize = (2*CLIP)/colors.length;
			var panelWidth = target.getWidth() - TRIM;
			//if no lables are to be show don't use it in calculations for box width			
			var usablePanelWidth = config.label ? panelWidth -MAX_LABEL_LENGTH_PIXELS : panelWidth;  
			var panelHeight = target.getHeight() - TRIM;
			
			var calculatedBoxHeight = Math.ceil(panelHeight/vectorObjs.length);
			var boxHeight = calculatedBoxHeight > MIN_BOX_HEIGHT ? calculatedBoxHeight : MIN_BOX_HEIGHT;
				
			var calculatedBoxWidth = Math.ceil( usablePanelWidth / vectorObjs[0].data.length);
			var boxWidth = calculatedBoxWidth > MIN_BOX_WIDTH ? calculatedBoxWidth: MIN_BOX_WIDTH;
			
			if (config.legend && config.legend.show && config.legend.container)
				insertLegend(config.legend.container);
			
			for (var i = 0; i < vectorObjs.length; i++) {
				
			
				
				var d = vectorObjs[i].data; // points.

				var vid = "heatmapCanvas" + Ext.id();
				Ext.DomHelper.append(target, {
							id : vid,
							tag : 'div',
							width : panelWidth,
							height : boxHeight,
							style : "width:" + panelWidth  + ";height:" + boxHeight
						});

				var canvasDiv = Ext.get(vid);
				var ctx = constructCanvas($(vid), usablePanelWidth, boxHeight);

				var offset = 0;
				for (var j = 0; j < d.length; j++) {

					var a = d[j][1]; // yvalue
					//Missing value
					if (isNaN(a)) {
						ctx.fillStyle = NAN_COLOR;
					} else {
						//Clip the data 1st
						if (a > CLIP) {
							a = CLIP;
						} else if (a < -CLIP) {
							a = -CLIP;
						}
						
						var v = Math.floor((a + CLIP)/binSize);  //Determine which color to use
						
						if (!colors[v]){ // v's max should be 16 but sometimes 17 cause of rounding....						
						//	console.log("color index out or range: " + v);
							v = colors.length - 1;
						//	console.log("Changed v to: " + v);
						}
						ctx.fillStyle = colors[v];
					}
					ctx.fillRect(offset, 0, boxWidth, boxHeight);
					offset = offset + boxWidth;
				}
				
				//Add label or not
				if (config.label){
					
					var text = Ext.DomHelper.append(canvasDiv, {
								tag : 'div',
								html : (vectorObjs[i].label) ?" <a  href='/Gemma/compositeSequence/show.html?id="+vectorObjs[i].labelID +"' target='_blank' ext:qtip= '" + vectorObjs[i].label + "'> " + Ext.util.Format.ellipsis( vectorObjs[i].label, MAX_LABEL_LENGTH_CHAR) + "</a>" : "n/a" 
							}, true);
					Ext.DomHelper.applyStyles(text, "position:absolute;top:0px;left:" + usablePanelWidth + "px;font-size:8px");
				}
			}
			//this.loadMask.hide();
		}

		
		/**
		 * Add a legend to the heatmap
		 */
		
		var MAX_LEGEND_HEIGHT = 10;
		var LEGEND_WIDTH = 64;
		
		function insertLegend(container){

				if(container == null)
							return;
				
							
				var legendDiv = $(container);	
//				var extlegendDiv = Ext.get("zoomLegend");
				var legendWidth = legendDiv.getWidth() - 10;
				var legendHeight = 10; //legendDiv.getHeight();
				var legendBoxWidth = Math.floor( legendWidth/COLOR_16.length);
								

				//TODO Get min/max labels for the legend. No luck adding a div to show info or drawing the numbers.... nothing shows up....
//				var posRangeLabel = Ext.DomHelper.append(extlegendDiv, {
//								tag : 'div',
//								html :"3"
//							}, true);
//							
//				var negRangeLabel = Ext.DomHelper.append(extlegendDiv, {
//								tag : 'div',
//								html : "-3"
//							}, true);

//				Ext.DomHelper.applyStyles(posRangeLabel, "position:absolute;top:0px;left:" + legendWidth + "px;font-size:8px");
//				Ext.DomHelper.applyStyles(negRangeLabel, "position:absolute;top:0px;left:0px;font-size:8px");
				
				
				var ctx = constructCanvas(legendDiv, legendWidth, legendHeight);

				//ctx.fillText("-3",0,0);
				var offset = 5;
				for (var j = 0; j < COLOR_16.length; j++) {
					ctx.fillStyle = COLOR_16[j];
					ctx.fillRect(offset, 0, legendBoxWidth, legendHeight);
					offset = offset + legendBoxWidth;
				}
				//ctx.fillText("3",offset,0);					
				
				
		}
		/**
		 * Function: (private) constructCanvas
		 * 
		 * Initializes a canvas. When the browser is IE, we make use of excanvas.
		 * 
		 * Parameters: none
		 * 
		 * Returns: ctx
		 */
		function constructCanvas(div, canvasWidth, canvasHeight) {

			div.innerHTML = '';

			/**
			 * For positioning labels and overlay.
			 */
			div.setStyle({
						'position' : 'relative'
					});

			if (canvasWidth <= 0 || canvasHeight <= 0) {
				throw 'Invalid dimensions for plot, width = ' + canvasWidth + ', height = ' + canvasHeight;
			}

				var canvas = Ext.DomHelper.append(div, {
					tag : 'canvas',
					width : canvasWidth,
					height : canvasHeight
				});
					
			
			if (Prototype.Browser.IE) {
				canvas = $(window.G_vmlCanvasManager.initElement(canvas));
			}
			
			return canvas.getContext('2d');
		}
	}

	return {
		clean : function(element) {
			element.innerHTML = '';
		},

		draw : function(target, data, options) {
			var map = new HeatMap(target, data, options);
			return map;
		}
	};
}();