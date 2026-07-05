<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Запись оформлена</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 min-h-screen flex items-center justify-center p-4">
    <div class="bg-white p-8 rounded-lg shadow-md w-full max-w-md text-center">
        <div class="text-green-500 text-5xl mb-4">✓</div>
        <h1 class="text-2xl font-bold mb-2">Вы записаны!</h1>
        <p class="text-gray-600 mb-4">
            <fmt:formatDate value="${booking.slot.startAt.toGregorianCalendar().time}" pattern="dd MMM, HH:mm"/> ·
            ${booking.slot.trackConfig.name}
        </p>
        <p class="text-sm">
            Маршал: ${booking.slot.marshal.name} ·
            ${booking.seatsCount} места
            <c:if test="${booking.rentalGearCount > 0}">, прокат: ${booking.rentalGearCount}</c:if>
        </p>
        <p class="text-xl font-bold mt-2">${booking.priceTotal} ₽</p>
        <p class="text-sm text-gray-500">Оплата на месте: наличные или перевод на карту.</p>

        <div class="mt-6 flex gap-2">
            <a href="${pageContext.request.contextPath}/bookings" class="flex-1 bg-gray-200 text-gray-800 py-2 rounded hover:bg-gray-300 transition">Мои брони</a>
            <a href="${pageContext.request.contextPath}/slots" class="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700 transition">Готово</a>
        </div>
    </div>
</body>
</html>