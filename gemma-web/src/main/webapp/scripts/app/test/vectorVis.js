/**
 * 
 * show heatmap of data.
 * 
 * retrieve data using the VisualizationController.
 * 
 * 
 */
var colors = ["black", "red", "orange", "yellow", "white"];
var container;
Ext.onReady(function() {
	container = Ext.get("vis");
});

function draw() {
	VisualizationController.getVectorData([1231409, 1231413, 1231415, 1252263],
			drawMap);
}

/**
 * Convert an array into colors.
 */
function colorMap(data) {

}

var boxSize = 16;

/**
 * Given an array of color specifications,draw boxes on a Canvas.
 */
function drawMap(vectorObjs) {

	// clear
	Ext.DomHelper.overwrite(container, "");

	for (i = 0; i < vectorObjs.length; i++) {

		var d = vectorObjs[i].data;
		var vid = "vec-" + vectorObjs[i].dedvId;
		Ext.DomHelper.append(container, {
			id : vid,
			tag : 'div',
			width : d.length * boxSize + 100,
			height : boxSize
		});

		var target = Ext.get(vid);
		var ctx = constructCanvas(target, d.length * boxSize, boxSize);

		var offset = 0;
		for (j = 0; j < d.length; j++) {

			a = d[j];
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
			html : vectorObjs[i].probeName
		}, true);
		Ext.DomHelper.applyStyles(text, "position:absolute;top:0px;left:" + boxSize*d.length + "px;font-size:8px");
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
function constructCanvas(target, width, height) {
	canvasWidth = width;
	canvasHeight = height;
	target.innerHTML = '';

	/**
	 * For positioning labels and overlay.
	 */
	target.setStyle({
		'position' : 'relative'
	});

	if (canvasWidth <= 0 || canvasHeight <= 0) {
		throw 'Invalid dimensions for plot, width = ' + canvasWidth
				+ ', height = ' + canvasHeight;
	}

	var canvas = Ext.DomHelper.append(target, {
		tag : 'canvas',
		width : canvasWidth,
		height : canvasHeight
	});
	if (Prototype.Browser.IE) {
		canvas = $(window.G_vmlCanvasManager.initElement(canvas));
	}
	ctx = canvas.getContext('2d');
	return ctx;
}