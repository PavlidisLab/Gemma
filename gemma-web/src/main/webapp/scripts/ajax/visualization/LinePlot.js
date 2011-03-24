/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */

/**
 * Draw line plots with (optional) sample labels above the graph.
 * 
 * @version $Id$
 * @author Paul
 */
var LinePlot = function() {

	var MAX_SAMPLE_LABEL_HEIGHT_PIXELS = 120;
	var TRIM = 5;
	var EXPANDED_BOX_WIDTH = 10; // maximum value to use when expanded.
	var SAMPLE_LABEL_MAX_CHAR = 125;

	function LinePlot(container, data, config, sampleLabels) {

		draw(container, data, config, sampleLabels);

		/**
		 * 
		 */
		function draw(container, series, config, sampleLabels) {

			// maybe not ready yet.
			if (series.length == 0)
				return;

			var numberOfColumns = series[0].data.length; // assumed to be the same always.

			// avoid insanity
			if (numberOfColumns == 0) {
				return;
			}

			/*
			 * Smooth or unsmooth the data, but only if the previous state has changed.
			 */

			if (config.smoothLineGraphs) {
				if (!series.smoothed) {
					smooth(series);
					series.smoothed = true;
				}
			} else {
				if (series.smoothed) {
					// put back the original data.
					for (var i = 0; i < data.length; i++) {
						var d = series[i].data;
						if (series[i].backup) { // better be!
							for (var j = 0; j < d.length; j++) {
								series[i].data[j][1] = series[i].backup[j][1];
							}
						}
					}
					series.smoothed = false;
				}
			}

			/*
			 * Deal with plotting.
			 */

			// TOTAL area available.
			var width = container.getWidth();
			var height = container.getHeight();

			// space used for the line plot itself.
			var plotHeight = height; // to be modified to make room for the sample labels.

			// initial guess..
			var plotWidth = width - (2 * TRIM);

			var spacePerPoint;

			if (config.forceFit) {
				spacePerPoint = plotWidth / numberOfColumns;
			} else {
				spacePerPoint = Math.max(EXPANDED_BOX_WIDTH, plotWidth / numberOfColumns);
				plotWidth = spacePerPoint * numberOfColumns;
			}

			// put the heatmap in a scroll panel if it is too big to display.
			if (!config.forceFit && plotWidth > width) {
				Ext.DomHelper.applyStyles(container, "overflow:auto");
			}

			if (sampleLabels) {

				if (spacePerPoint >= EXPANDED_BOX_WIDTH) {
					var fontSize = Math.min(12, spacePerPoint - 1);
					// longest label...
					var maxLabelLength = 0;
					for (var j = 0; j < sampleLabels.length; j++) {
						if (sampleLabels[j].length > maxLabelLength) {
							maxLabelLength = sampleLabels[j].length;
						}
					} 
					// compute approximate pixel size of that label...not so easy without context.
					var labelHeight = Math.round(Math.min(MAX_SAMPLE_LABEL_HEIGHT_PIXELS, Math.min(maxLabelLength,
									SAMPLE_LABEL_MAX_CHAR)
									* fontSize * 0.8)); // 0.8 is about right (pixel width)/(pixel of height).

					var id = 'sampleLabels-' + Ext.id();

					var sampleLabelsWidth = plotWidth + (2 * TRIM);

					Ext.DomHelper.append(container, {
								id : id,
								tag : 'div',
								width : sampleLabelsWidth,
								height : labelHeight
							});

					var ctx = constructCanvas($(id), sampleLabelsWidth, labelHeight);

					ctx.fillStyle = "#000000";
					ctx.font = fontSize + "px sans-serif";
					ctx.textAlign = "left";
					ctx.translate(TRIM + 5, labelHeight - 2); // +extra to make sure we don't chop off the top.

					// vertical text.
					for (var j = 0; j < sampleLabels.length; j++) {
						// the shorter the better for performance.
						var lab = Ext.util.Format.ellipsis(sampleLabels[j], SAMPLE_LABEL_MAX_CHAR);

						ctx.rotate(-Math.PI / 2);
						ctx.fillText(lab, 0, 0);
						ctx.rotate(Math.PI / 2);
						ctx.translate(spacePerPoint, 0);
					}

					plotHeight = plotHeight - labelHeight;

				} else if (config.forceFit) {
					var message;
					if (numberOfColumns.length > 80) { // basically, no matter how wide they make it, there won't be
						// room.
						message = "Click 'expand' to see the sample labels";
					} else {
						message = "Click 'expand' or try widening the window to see the sample labels";
					}

					var mid = "message-" + Ext.id();

					Ext.DomHelper.append(container, {
								id : mid,
								tag : 'div',
								width : plotWidth,
								height : 20
							});

					var ctx = constructCanvas($(mid), plotWidth, 20);
					ctx.translate(10, 10);
					ctx.fillText(message, 0, 0);
					plotHeight = plotHeight - 20;
				}

			}

			plotHeight = plotHeight - 4 * TRIM;

			if (plotHeight < 10 || plotWidth < 10) {
				return; // don't try ...
			}

			// todo: vary colours

			var vid = "plotCanvas-" + Ext.id();
			Ext.DomHelper.append(container, {
						id : vid,
						tag : 'div',
						style : "margin:" + TRIM + "px;width:" + plotWidth + ";height:" + plotHeight
					});
			Flotr.draw($(vid), series, config);
		}

	}

	function smooth(series) {
		for (var i = 0; i < series.length; i++) {

			var d = series[i].data;

			if (!series[i].backup) {
				series[i].backup = [];
				for (var m = 0; m < d.length; m++) {
					series[i].backup.push([d[m][0], d[m][1]]);
				}
			}

			// window size from 2-10.
			var w = Math.min(10, Math.max(Math.floor(d.length / 25.0), 2));

			var lv = 0;
			var ave = [];
			for (var j = 0; j < d.length; j++) {

				var u = 0, v = 0;
				if (j < d.length - w) {
					for (var k = j; k < j + w; k++) {
						v += d[k][1];
						u++;
					}
				} else {
					// at the end, we just average what we can. This causes some problems, but not too bad.
					for (var k = j; k < d.length; k++) {
						v += d[k][1];
						u++;
					}
				}

				ave.push(v / u);

			}

			for (j = 0; j < d.length; j++) {
				series[i].data[j][1] = ave[j];
			}

		}

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

	return {
		clean : function(element) {
			element.innerHTML = '';
		},

		draw : function(target, data, options, sampleLabels) {
			return new LinePlot(target, data, options, sampleLabels);
		}
	};
}();