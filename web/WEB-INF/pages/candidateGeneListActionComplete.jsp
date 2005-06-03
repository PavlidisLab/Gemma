<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.Map" %>
<%-- 
The purpose of this page is to prevent reloads from re-running a completed action.
--%>

<c:if test="${applicationScope.secureLogin == 'true'}">
    <Gemma:secure/>
</c:if>
<%
Map m = (Map)request.getAttribute("model");
if( m==null) {
	response.sendRedirect("candidateGeneList.htm");
}
else{
	String target = (String) m.get("target");
	String listID = (String) m.get("listID");
	if( target != null && target.compareTo("candidateGeneListDetail")==0){
		response.sendRedirect("candidateGeneListDetail.htm?listID=" + listID);
	}
	else{	
		response.sendRedirect("candidateGeneList.htm");
	}
}
%>