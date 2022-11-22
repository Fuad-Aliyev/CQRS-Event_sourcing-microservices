package com.eventsourcing.bankaccount.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateBankAccountRequestDTO {
    @Email
    @NotBlank
    @Size(min = 10, max = 250)
    private String email;
    @NotBlank
    @Size(min = 10, max = 250)
    private String address;
    @NotBlank
    @Size(min = 10, max = 250)
    private String username;
}
