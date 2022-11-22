package com.eventsourcing.bankaccount.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InternalServerErrorResponse {
    private int status;
    private String message;
    private String timestamp;
}
