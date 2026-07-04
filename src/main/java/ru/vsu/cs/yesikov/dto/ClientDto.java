package ru.vsu.cs.yesikov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ClientDto {
    private UUID id;
    private String name;
    private String phone;
    private OffsetDateTime createdAt;
}