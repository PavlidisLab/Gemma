/**
 * @version $Id$
 * @author Kelsey
 */

var Heatmap = function() {

	var MAX_LABEL_LENGTH_PIXELS = 190;
	var MIN_BOX_WIDTH = 2;
	var MAX_BOX_WIDTH = 20;
	var CLIP = 3;
	var NAN_COLOR = "grey";
	var SHOW_LABEL_MIN_SIZE = 8;
	var MIN_BOX_HEIGHT = 10; // so we can see the labels
	var MAX_BOX_HEIGHT = 18;
	var MAX_ROWS_BEFORE_SCROLL = 30;

	var MAX_SAMPLE_LABEL_HEIGHT_PIXELS = 120;

	var TRIM = 5;
	var DEFAULT_ROW_LABEL = "&nbsp;";

	// TODO put constants in config object so they can programtically changed on
	// the fly
	var DEFAULT_CONFIG = {
		label : false, // shows labels at end of row
		useFixedBoxHeight : true
	// Height of each row defaults to 12, setting to false will try calculate
	// row hight to fit in given container
	};

	var COLOR_4 = [ "black", "red", "orange", "yellow", "white" ];

	// black-red-orange-yellow-white
	var COLOR_16 = [ "rgb(0, 0, 0)", "rgb(32, 0, 0)", "rgb(64, 0, 0)", "rgb(96, 0, 0)", "rgb(128, 0, 0)",
			"rgb(159, 32, 0)", "rgb(191, 64, 0)", "rgb(223, 96, 0)", "rgb(255, 128, 0)", "rgb(255, 159, 32)",
			"rgb(255, 191, 64)", "rgb(255, 223, 96)", "rgb(255, 255, 128)", "rgb(255, 255, 159)", "rgb(255, 255, 191)",
			"rgb(255, 255, 223)", "rgb(255, 255, 255)" ];

	/**
	 * @param container
	 * @param data
	 * @param config
	 *            optional
	 * @param sampleLabels
	 *            optional.
	 */
	function HeatMap(container, data, config, sampleLabels) {

		if (!config) {
			config = DEFAULT_CONFIG;
		}

		drawMap(data, container, COLOR_16, config, sampleLabels);

		// Creates 1 canvas per row of the heat map
		function drawMap(vectorObjs, target, colors, config, sampleLabels) {

			// Get dimensions of target to determine box size in heat map
			var binSize = (2 * CLIP) / colors.length;
			var panelWidth = target.getWidth() - TRIM;
			// if no labels are to be shown don't use it in calculations for box
			// width
			var usablePanelWidth = config.label ? panelWidth - MAX_LABEL_LENGTH_PIXELS : panelWidth;

			var panelHeight = target.getHeight() - TRIM;

			if (sampleLabels) {
				panelHeight = panelHeight - MAX_SAMPLE_LABEL_HEIGHT_PIXELS; // might
				// be a
				// bad
				// guess.
			}

			var numberOfRowsToComputeSizeBy = Math.min(MAX_ROWS_BEFORE_SCROLL, vectorObjs.length);

			var calculatedBoxHeight = Math.floor(panelHeight / numberOfRowsToComputeSizeBy) - 2;

			if (calculatedBoxHeight > MAX_BOX_HEIGHT) {
				boxHeight = MAX_BOX_HEIGHT;
			} else if (calculatedBoxHeight < MIN_BOX_HEIGHT) {
				boxHeight = MIN_BOX_HEIGHT;
			} else {
				boxHeight = calculatedBoxHeight;
			}

			// resize containing div because possible scrollover over
			// elements below
			if (vectorObjs.length > numberOfRowsToComputeSizeBy) {

				// update height
				panelHeight = boxHeight * vectorObjs.length + TRIM;

				panelId = "heatmapScrollPanel-" + Ext.id();
				// Create a scroll panel to put in
				var scrollPanel = new Ext.Panel( {
					autoScroll : true,
					stateful : false,
					applyTo : target,
					html : {
						id : panelId,
						tag : 'div'
					}
				});
				// update target
				target = $(panelId);
			}

			var numberOfBoxesToDraw = vectorObjs[0].data.length;

			// avoid insanity
			if (numberOfBoxesToDraw == 0) {
				return;
			}

			var calculatedBoxWidth = Math.floor(usablePanelWidth / numberOfBoxesToDraw);

			var boxWidth = calculatedBoxWidth < MIN_BOX_WIDTH ? MIN_BOX_WIDTH : calculatedBoxWidth;

			boxWidth = boxWidth > MAX_BOX_WIDTH ? MAX_BOX_WIDTH : boxWidth;

			if (config.legend && config.legend.show && config.legend.container)
				insertVerticalLegend(config.legend.container);

			if (sampleLabels && boxWidth >= SHOW_LABEL_MIN_SIZE) {

				var id = 'sampleLabels-' + Ext.id();
				Ext.DomHelper.append(target, {
					id : id,
					tag : 'div',
					width : usablePanelWidth,
					height : MAX_SAMPLE_LABEL_HEIGHT_PIXELS
				});

				var labelDiv = Ext.get(id);

				var ctx = constructCanvas($(labelDiv), usablePanelWidth, MAX_SAMPLE_LABEL_HEIGHT_PIXELS);

				ctx.fillStyle = "#000000";
				ctx.font = (boxWidth - 1) + "px, sans-serif";
				ctx.textAlign = "left";
				ctx.translate(0, MAX_SAMPLE_LABEL_HEIGHT_PIXELS - 2);
				ctx.save();

				for ( var j = 0; j < sampleLabels.length; j++) {
					var lab = Ext.util.Format.ellipsis(sampleLabels[j], 25); // faster to draw small
					if (window.console) window.console.log(lab);
					ctx.translate(boxWidth, 0);
					ctx.save();
					ctx.rotate(-Math.PI / 2);
					ctx.fillText(lab, 0, 0)
					ctx.rotate(Math.PI / 2); // reset?
				}

			}

			for ( var i = 0; i < vectorObjs.length; i++) {

				var d = vectorObjs[i].data; // points.

				var vid = "heatmapCanvas-" + Ext.id();
				Ext.DomHelper.append(target, {
					id : vid,
					tag : 'div',
					width : panelWidth,
					height : boxHeight,
					style : "width:" + panelWidth + ";height:" + boxHeight
				});

				var canvasDiv = Ext.get(vid);
				var ctx = constructCanvas($(vid), usablePanelWidth, boxHeight);

				var offset = 0;
				for ( var j = 0; j < d.length; j++) {

					var a = d[j][1];

					if (isNaN(a)) {
						ctx.fillStyle = NAN_COLOR;
					} else {

						// Clip the data 1st
						if (a > CLIP) {
							a = CLIP;
						} else if (a < -CLIP) {
							a = -CLIP;
						}

						// Determine which color to use
						var v = Math.floor((a + CLIP) / binSize);

						if (v > colors.length - 1) {
							v = colors.length - 1;
						}

						ctx.fillStyle = colors[v];
					}
					ctx.fillRect(offset, 0, boxWidth, boxHeight);
					offset = offset + boxWidth;
				}

				// Add label or not
				if (config.label) {
					var rowLabel = DEFAULT_ROW_LABEL;
					if (vectorObjs[i].label) {
						rowLabel = vectorObjs[i].label;
					}
					var text = Ext.DomHelper.append(canvasDiv, {
						id : "heatmaplabel-" + Ext.id(),
						tag : 'div',
						html : "&nbsp;" + rowLabel,
						style : "white-space: nowrap"
					}, true);
					Ext.DomHelper.applyStyles(text, "position:absolute;top:0px;left:" + (offset + 5) + "px");
				}
			}
			// this.loadMask.hide();
		}

		/**
		 * Add a legend to the heatmap
		 */

		var MAX_LEGEND_HEIGHT = 10;
		var LEGEND_WIDTH = 64;

		function insertLegend(container) {

			if (!container)
				return;

			var legendDiv = $(container);
			var legendWidth = legendDiv.getWidth() - 10;
			var legendHeight = 10; // legendDiv.getHeight();
			var legendBoxWidth = Math.floor(legendWidth / COLOR_16.length);

			// TODO Get min/max labels for the legend. No luck adding a div to
			// show info or drawing the numbers.... nothing shows up....

			var extlegendDiv = Ext.get("zoomLegend");
			var posRangeLabel = Ext.DomHelper.append(extlegendDiv, {
				id : "legendLabel-" + Ext.id(),
				tag : 'div',
				html : "3"
			}, true);

			var negRangeLabel = Ext.DomHelper.append(extlegendDiv, {
				id : "legendlabel-" + Ext.id(),
				tag : 'div',
				html : "-3"
			}, true);

			Ext.DomHelper.applyStyles(posRangeLabel, "position:absolute;top:0px;left:" + legendWidth
					+ "px;font-size:8px");
			Ext.DomHelper.applyStyles(negRangeLabel, "position:absolute;top:0px;left:0px;font-size:8px");

			// ctx.fillText("-3",0,0);

			var offset = 5;
			for ( var j = 0; j < COLOR_16.length; j++) {

				var ctx = constructCanvas(legendDiv, legendWidth, legendHeight);

				ctx.fillStyle = COLOR_16[j];
				ctx.fillRect(offset, 0, legendBoxWidth, legendHeight);
				offset = offset + legendBoxWidth;
			}
			// ctx.fillText("3",offset,0);

		}

		function insertVerticalLegend(container) {

			if (!container)
				return;

			var legendDiv = $(container);
			legendDiv.innerHTML = '';

			var legendWidth = legendDiv.getWidth() - 10;
			var legendHeight = legendDiv.getHeight();
			var boxsize = 12;
			var binsize = 2 * CLIP / COLOR_16.length;
			var rangeMin = -CLIP;

			for ( var i = 0; i < COLOR_16.length; i++) {

				var rowLabel = "&nbsp;" + sprintf("%.4s", rangeMin) + " to " + sprintf("%.4s", rangeMin + binsize);
				rangeMin = rangeMin + binsize;

				var legendRowId = "heatmapLegendRow-" + Ext.id();
				Ext.DomHelper.append(legendDiv, {
					id : legendRowId,
					tag : 'div',
					width : legendWidth,
					height : boxsize,
					style : "width:" + legendWidth + ";height:" + boxsize
				});

				var ctx = constructCanvas($(legendRowId), boxsize, boxsize);
				ctx.fillStyle = COLOR_16[i];
				ctx.fillRect(0, 0, boxsize, boxsize);

				var legendRowDiv = Ext.get(legendRowId);
				var text = Ext.DomHelper.append(legendRowDiv, {
					id : "legendRowlabel-" + Ext.id(),
					tag : 'div',
					html : rowLabel
				}, true);
				Ext.DomHelper.applyStyles(text, "position:absolute;top:0px;left:" + boxsize + "px;font-size:10px");

			}

			// Add The NAN color to legend.

			legendRowId = "heatmapLegendRow-" + Ext.id();
			Ext.DomHelper.append(legendDiv, {
				id : legendRowId,
				tag : 'div',
				width : legendWidth,
				height : boxsize,
				style : "width:" + legendWidth + ";height:" + boxsize
			});

			var ctx = constructCanvas($(legendRowId), boxsize, boxsize);
			ctx.fillStyle = NAN_COLOR;
			ctx.fillRect(0, 0, boxsize, boxsize);

			var legendRowDiv = Ext.get(legendRowId);
			var text = Ext.DomHelper.append(legendRowDiv, {
				id : "legendRowlabel-" + Ext.id(),
				tag : 'div',
				html : "&nbsp; NaN"
			}, true);
			Ext.DomHelper.applyStyles(text, "position:absolute;top:0px;left:" + boxsize + "px;font-size:10px");

		}

		/**
		 * Function: (private) constructCanvas
		 * 
		 * Initializes a canvas. When the browser is IE, we make use of
		 * excanvas.
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
			div.setStyle( {
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

		draw : function(target, data, options, sampleLabels) {
			var map = new HeatMap(target, data, options, sampleLabels);
			return map;
		}
	};
}();