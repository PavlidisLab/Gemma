<%@ include file="include.jsp" %>
<%@ taglib prefix="Gemma" uri="/Gemma" %>
<%@ page contentType="text/html; charset=iso-8859-1" language="java" import="java.sql.*" errorPage="" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head><title><fmt:message key="title"/></title><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"></head>
<body bgcolor="#ffffff">
<%--<img src="bigicon.gif" width="88" height="97"> <br>--%>
<h1><fmt:message key="bulkLoad.heading"/></h1>
<SCRIPT TYPE="text/javascript">
function dropdown(mySel)
{
var myWin, myVal;
myVal = mySel.options[mySel.selectedIndex].value;
if(myVal)
 {
 if(mySel.form.target)myWin = parent[mySel.form.target];
 else myWin = window;
 if (! myWin) return true;
 myWin.location = myVal;
 }
return false;
}
</SCRIPT>

<form method="post">
<form name="form1" method="post" action="">
<SELECT NAME="typeOfLoader">
<OPTION VALUE="">Choose objects to load ...
<OPTION VALUE="geneLoaderService">Genes
<OPTION VALUE="taxonLoaderService">Taxons
<OPTION VALUE="chromosomeLoaderService">Chromosomes
<OPTION VALUE="arrayDesignLoaderService">Array Designs
</SELECT>

<input type=checkbox name="hasHeader" value=true>File Has Header<br>

<%--
      <td alignment="right" width="31%"><strong>Filename:</strong></td>
      <spring:bind path="fileName.fileName">
        <td width="38%">
          <input name="fileName" type="text" value="<c:out value="${status.value}"/>">
        </td>
        <td width="60%">
          <font color="red"><c:out value="${status.errorMessage}"/></font>
        </td>
      </spring:bind>
--%>      
<br></br>
<%--
<spring:hasBindErrors name="fileName">
    <b>Please fix all errors!</b>
</spring:hasBindErrors>
--%>
<br>
<input type="submit" alignment="center" value="Submit">       
</form>

<a href="<c:url value="home.jsp"/>">Home</a>
</body>
</html>


