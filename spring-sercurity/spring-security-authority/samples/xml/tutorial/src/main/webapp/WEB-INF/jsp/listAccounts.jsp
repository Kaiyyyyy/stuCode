<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
	<meta http-equiv="content-type" content="text/html; charset=UTF-8">
	<link rel="stylesheet" href="<c:url value='/static/css/tutorial.css'/>" type="text/css" />
	<title>Accounts</title>
</head>
<body>
<div id="content">

<h1>Accounts</h1>
<p>
Anyone can view this page, but posting to an Account requires login and must be authorized. Below are some users to try posting to Accounts with.
</p>
<ul>
<li>rod/koala - can post to any Account</li>
<li>dianne/emu - can post to Accounts as long as the balance remains above the overdraft amount</li>
<li>scott/wombat - cannot post to any Accounts</li>
</ul>

<a href="index.jsp">Home</a><br><br>

<table>
<tr>
<td><b>ID</b></td>
<td><b>Holder</b></td>
<td><b>Balance</b></td>
<td><b>Overdraft</b></td>
<td><b>Operations</b></td>
</tr>
<c:forEach var="account" items="${accounts}">
<tr>
<td>${account.id}</td>
<td>${account.holder}</td>
<td>${account.balance}</td>
<td>${account.overdraft}</td>
<td>
	<a href="post.html?id=${account.id}&amp;amount=-20.00">-$20</a>
	<a href="post.html?id=${account.id}&amp;amount=-5.00">-$5</a>
	<a href="post.html?id=${account.id}&amp;amount=5.00">+$5</a>
	<a href="post.html?id=${account.id}&amp;amount=20.00">+$20</a>
</td>
</tr>
</c:forEach>
</table>

<p>
<form action="logout" method="post">
	<sec:csrfInput />
	<input type="submit" value="Logout"/>
</form>
</div>
</body>
</html>
