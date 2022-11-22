package com.eventsourcing.bankaccount.queries;

import com.eventsourcing.bankaccount.dto.BankAccountResponseDTO;

public interface BankAccountQueryService {
    BankAccountResponseDTO handle(GetBankAccountByIDQuery query);
}
