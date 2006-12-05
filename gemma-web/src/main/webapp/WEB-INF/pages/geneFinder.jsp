<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="ubic.gemma.model.genome.Gene" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Collection" %>
<title> Search Gemme for Genes </title>

From here you can search the Gemma database for existing Genes.
<BR />
The search will be by:
<BR />
<BR />
- official symbol 
<BR />
- synonym 
<BR /> 
- genes that have gene products with that name or GI number
<BR />
- genes that have bioSequences with that name or GI number
<BR /><BR />

<form name="geneSearch" action="geneFinder.html" method="POST">
<input type="text" name="searchString" />
<input type="submit" value="Search">
</form>