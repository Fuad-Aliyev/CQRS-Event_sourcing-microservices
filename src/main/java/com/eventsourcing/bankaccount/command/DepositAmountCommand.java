package com.eventsourcing.bankaccount.command;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DepositAmountCommand {
    private String aggregateId;
    private BigDecimal amount;
}
