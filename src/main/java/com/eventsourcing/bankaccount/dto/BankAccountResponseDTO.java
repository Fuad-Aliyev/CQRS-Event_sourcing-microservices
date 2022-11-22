package com.eventsourcing.bankaccount.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BankAccountResponseDTO {
    private String aggregateId;
    private String email;
    private String address;
    private String userName;
    private BigDecimal balance;
}
