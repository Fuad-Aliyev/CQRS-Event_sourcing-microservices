package com.eventsourcing.bankaccount.controller;

import com.eventsourcing.bankaccount.command.BankAccountCommandService;
import com.eventsourcing.bankaccount.command.CreateBankAccountCommand;
import com.eventsourcing.bankaccount.command.DepositAmountCommand;
import com.eventsourcing.bankaccount.dto.BankAccountResponseDTO;
import com.eventsourcing.bankaccount.dto.CreateBankAccountRequestDTO;
import com.eventsourcing.bankaccount.dto.DepositAmountRequestDTO;
import com.eventsourcing.bankaccount.queries.BankAccountQueryService;
import com.eventsourcing.bankaccount.queries.GetBankAccountByIDQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/bank")
@Slf4j
@RequiredArgsConstructor
public class BankAccountController {
    private final BankAccountCommandService commandService;
    private final BankAccountQueryService queryService;

    @PostMapping
    public ResponseEntity<String> createBankAccount(@Valid @RequestBody CreateBankAccountRequestDTO dto) {
        final String aggregateID = UUID.randomUUID().toString();
        final String id = commandService.handle(new CreateBankAccountCommand(aggregateID, dto.getEmail(), dto.getUsername(), dto.getAddress()));
        log.info("Created bank account id: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PostMapping(path = "/deposit/{aggregateId}")
    public ResponseEntity<Void> depositAmount(@Valid @RequestBody DepositAmountRequestDTO dto, @PathVariable String aggregateId) {
        commandService.handle(new DepositAmountCommand(aggregateId, dto.getAmount()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("{aggregateId}")
    public ResponseEntity<BankAccountResponseDTO> getBankAccount(@PathVariable String aggregateId) {
        final BankAccountResponseDTO result = queryService.handle(new GetBankAccountByIDQuery(aggregateId));
        log.info("Get bank account result: {}", result);
        return ResponseEntity.ok(result);
    }
}
