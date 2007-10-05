// modified from developer.apple.com - DynamicForms with DHTML
// @author : jsantos
function getStyleObject(objectId) {
  // checkW3C DOM, then MSIE 4, then NN 4.
  //
  if(document.getElementById && document.getElementById(objectId)) {
	return document.getElementById(objectId).style;
   }
   else if (document.all && document.all(objectId)) {  
	return document.all(objectId).style;
   } 
   else if (document.layers && document.layers[objectId]) { 
	return document.layers[objectId];
   } else {
	return false;
   }
}
function changeObjectVisibility(objectName, newVisibility) {
	// get the objects with the given name
	var elements = document.getElementsByName(objectName);
	for (var i = 0; i < elements.length; i++) {
    	// first get the object's stylesheet
    	var styleObject = elements[i].style;

    // then if we find a stylesheet, set its visibility
    // as requested
    //
        if (styleObject) {
			styleObject.display = newVisibility;
    	}
    }
	return false;
}

/*
* @deprecated in favor of Effects.toggle
*/
function toggleVisibility(objectName) {
	// get the objects with the given name
	var elements = document.getElementsByClassName(objectName);

	for (var i = 0; i < elements.length; i++) {
    	// first get the object's stylesheet
    	var styleObject = elements[i].style;

    	// then if we find a stylesheet, set its visibility
    	// as requested
    	//
        if (styleObject) {
  	   		if (styleObject.display == 'none') {
   				styleObject.display = 'inline'; 		
    		}
    		else if (styleObject.display == 'inline') {
   				styleObject.display = 'none'; 		
    		}
    		else {
    			styleObject.display = 'none';
    		}
    	}
    }
	return false;
}