// ru.vsu.cs.yesikov.web.dto.BookingForm.java
package ru.vsu.cs.yesikov.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingForm {
    @NotNull(message = "Укажите количество мест")
    @Min(value = 1, message = "Количество мест должно быть не меньше 1")
    private Integer seatsCount;

    @NotNull(message = "Укажите количество проката")
    @Min(value = 0, message = "Количество проката не может быть отрицательным")
    private Integer rentalGearCount;
}