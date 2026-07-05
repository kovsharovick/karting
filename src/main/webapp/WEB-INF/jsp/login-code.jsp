<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Ввод кода</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 min-h-screen flex items-center justify-center p-4">
    <div class="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 class="text-2xl font-bold text-blue-600 text-center mb-4">Подтверждение</h1>
        <p class="text-center text-gray-600 mb-4">Код отправлен на <span class="font-medium">${phone}</span></p>

        <form action="${pageContext.request.contextPath}/verify-code" method="post">
            <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700">Код из SMS</label>
                <input type="text" name="code" placeholder="____" maxlength="6"
                       class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
                <c:if test="${not empty error}">
                    <p class="text-red-600 text-sm mt-1">${error}</p>
                </c:if>
            </div>
            <button type="submit"
                    class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition">
                Подтвердить
            </button>
        </form>
    </div>
</body>
</html>