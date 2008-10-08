Heatmap = (function() {

	var colors = ["black", "red", "orange", "yellow", "white"];

	var boxSize = 10;

	function HeatMap(container, data) {

		// constructCanvas(container);
		drawMap(data);

		function drawMap(vectorObjs) {
			for (i = 0; i < vectorObjs.length; i++) {

				var d = vectorObjs[i].data; // points.

				var vid = "vec-" + Ext.id();
				Ext.DomHelper.append(container, {
							id : vid,
							tag : 'div',
							width : d.length * boxSize + 100,
							height : boxSize,
							style : "width:" + (d.length * boxSize) + ";height:" + boxSize
						});

				var target = Ext.get(vid);
				var ctx = constructCanvas($(vid));

				var offset = 0;
				for (j = 0; j < d.length; j++) {

					a = d[j][1]; // yvalue
					if (isNaN(a)) {
						ctx.fillStyle = "grey";
					} else {
						if (a > 2) {
							a = 2;
						} else if (a < -2) {
							a = -2;
						}
						v = Math.round(a - (-2));
						ctx.fillStyle = colors[v];
					}
					ctx.fillRect(offset, 0, boxSize, boxSize);
					offset = offset + boxSize;
				}
				var text = Ext.DomHelper.append(target, {
							tag : 'div',
							html : 'test'// vectorObjs[i].probeName
						}, true);
				Ext.DomHelper.applyStyles(text, "position:absolute;top:0px;left:" + boxSize * d.length
								+ "px;font-size:8px");
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
		function constructCanvas(target) {
			canvasWidth = target.getWidth();
			canvasHeight = target.getHeight();
			target.innerHTML = '';

			/**
			 * For positioning labels and overlay.
			 */
			target.setStyle({
						'position' : 'relative'
					});

			if (canvasWidth <= 0 || canvasHeight <= 0) {
				throw 'Invalid dimensions for plot, width = ' + canvasWidth + ', height = ' + canvasHeight;
			}

			var canvas = Ext.DomHelper.append(target, {
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
			var plot = new HeatMap(target, data, options);
			return plot;
		}
	}
})();