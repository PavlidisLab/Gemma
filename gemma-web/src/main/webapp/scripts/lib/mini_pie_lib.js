var MiniPieLib = {};

	/**
	 * Method to draw a two-piece pie chart in a canvas
	 * @param {Object} ctx the canvas component to draw in (here, the canvas tag)
	 * @param {int} x centre of pie on x axis relative to top right of ctx
	 * @param {int} y centre of pie on y axis relative to top right of ctx
	 * @param {int} size size of the pie chart
	 * @param {String} color colour for the slice associsted with value param 
	 * @param {int} value size of the slice in degrees

	 */
MiniPieLib.drawMiniPie = function(ctx, x, y, size, color, value)
{
    ctx.save();
    ctx.fillStyle = color;
    ctx.strokeStyle = color;
    ctx.moveTo(x, y);        
    // draw circle
    ctx.beginPath();
    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI*2),false);
    ctx.stroke();
    // draw slice
    ctx.beginPath();
    ctx.moveTo(x, y);        
    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI/180)*value,false);
    ctx.lineTo(x,y);
    ctx.fill();
    
    ctx.restore();			
}

	/**
	 * Method to draw a two-colour two-piece pie chart in a canvas
	 * @param {Object} ctx the canvas component to draw in (here, the canvas tag)
	 * @param {int} x centre of pie on x axis relative to top right of ctx
	 * @param {int} y centre of pie on y axis relative to top right of ctx
	 * @param {int} size size of the pie chart
	 * @param {String} colour one colour for the pie
	 * @param {String} sliceColour the other colour for the slice, the one associsted with value param 
	 * @param {int} value corresponds to the size of the slice
	 * @param {String} outlineColour the colour for the outline of the circle
	 */
function drawTwoColourMiniPie(ctx, x, y, size, colour, sliceColour, value, outlineColour){
	    ctx.save();
	    ctx.fillStyle = colour;
	    ctx.strokeStyle = colour;
	    ctx.moveTo(x, y);
	    // draw circle
	    ctx.beginPath();
	    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI*2),false);
	    ctx.fill();
	    // draw slice
	    ctx.fillStyle = sliceColour;
	    ctx.beginPath();
	    ctx.moveTo(x, y);        
	    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI/180)*value,false);
	    ctx.lineTo(x,y);
	    ctx.fill();
		// draw circle outline
	    ctx.beginPath();
	    ctx.arc(x,y,size/2, Math.PI*3/2 , Math.PI*3/2+(Math.PI*2),false);
	    ctx.strokeStyle = outlineColour;
	    ctx.stroke();
	    
	    ctx.restore();			
	}