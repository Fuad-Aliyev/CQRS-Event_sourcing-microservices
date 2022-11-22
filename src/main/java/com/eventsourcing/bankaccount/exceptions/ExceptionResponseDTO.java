package com.eventsourcing.bankaccount.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ExceptionResponseDTO {
    private int Status;
    private String message;
    private LocalDateTime timestamp;
}
