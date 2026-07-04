package ru.vsu.cs.yesikov.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.model.enums.TrackConfigType;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TrackConfigurationDto {
    private UUID id;
    private String name;
    private TrackConfigType type;
    private String description;
}