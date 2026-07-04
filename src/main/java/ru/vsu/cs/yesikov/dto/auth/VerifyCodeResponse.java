package ru.vsu.cs.yesikov.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.vsu.cs.yesikov.dto.ClientDto;

@Data
@AllArgsConstructor
public class VerifyCodeResponse {
    private TokenPairResponse tokens;
    private ClientDto client;
    private Boolean isNew;
}