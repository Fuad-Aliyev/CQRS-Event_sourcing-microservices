package com.eventsourcing.bankaccount.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class DepositAmountRequestDTO {
    @Min(value = 300, message = "minimal amount is 300")
    @NotNull
    BigDecimal amount;
}
