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

	function LinePlot(container, data, config, sampleLabels) {

		drawMap(container, data, config, sampleLabels);

		/**
		 * s
		 */
		function drawMap(container, series, config, sampleLabels) {

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

			// todo: add sample names. Need a separate div, which leaves less room for the graph.

			// todo: add some white space around the graph.

			// todo: vary colours

			Flotr.draw(container, series, config);

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
			var map = new LinePlot(target, data, options, sampleLabels);
			return map;
		}
	};
}();