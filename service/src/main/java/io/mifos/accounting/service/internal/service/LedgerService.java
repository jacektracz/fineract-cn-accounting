/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.accounting.service.internal.service;

import io.mifos.accounting.service.internal.mapper.AccountMapper;
import io.mifos.accounting.service.internal.mapper.LedgerMapper;
import io.mifos.accounting.service.internal.repository.AccountEntity;
import io.mifos.accounting.service.internal.repository.AccountRepository;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.repository.LedgerEntity;
import io.mifos.accounting.service.internal.repository.LedgerRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LedgerService {

  private final Logger logger;
  private final LedgerRepository ledgerRepository;
  private final AccountRepository accountRepository;

  @Autowired
  public LedgerService(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                       final LedgerRepository ledgerRepository,
                       final AccountRepository accountRepository) {
    super();
    this.logger = logger;
    this.ledgerRepository = ledgerRepository;
    this.accountRepository = accountRepository;
  }

  public List<Ledger> fetchLedgerHierarchy() {
    final List<LedgerEntity> ledgerEntities = this.ledgerRepository.findByParentLedgerIsNull();
    if (ledgerEntities != null) {
      return ledgerEntities.stream().map(ledgerEntity -> {
        final Ledger ledger = LedgerMapper.map(ledgerEntity);
        final List<LedgerEntity> subLedgerEntities =
            this.ledgerRepository.findByParentLedger(ledgerEntity);
        addSubLedgers(ledger, subLedgerEntities);
        return ledger;
      }).collect(Collectors.toList());

    } else {
      return Collections.emptyList();
    }
  }

  public Optional<Ledger> findLedger(final String identifier) {
    final LedgerEntity ledgerEntity = this.ledgerRepository.findByIdentifier(identifier);
    if (ledgerEntity != null) {
      final Ledger ledger = LedgerMapper.map(ledgerEntity);
      this.addSubLedgers(ledger, this.ledgerRepository.findByParentLedger(ledgerEntity));
      return Optional.of(ledger);
    } else {
      return Optional.empty();
    }
  }

  public AccountPage fetchAccounts(final String ledgerIdentifier, final Pageable pageable) {
    final LedgerEntity ledgerEntity = this.ledgerRepository.findByIdentifier(ledgerIdentifier);
    final Page<AccountEntity> accountEntities = this.accountRepository.findByLedger(ledgerEntity, pageable);

    final AccountPage accountPage = new AccountPage();
    accountPage.setTotalPages(accountEntities.getTotalPages());
    accountPage.setTotalElements(accountEntities.getTotalElements());

    if(accountEntities.getSize() > 0){
      final List<Account> accounts = new ArrayList<>(accountEntities.getSize());
      accountEntities.forEach(accountEntity -> accounts.add(AccountMapper.map(accountEntity)));
      accountPage.setAccounts(accounts);
    }

    return accountPage;
  }

  public boolean hasAccounts(final String ledgerIdentifier) {
    final LedgerEntity ledgerEntity = this.ledgerRepository.findByIdentifier(ledgerIdentifier);
    final List<AccountEntity> ledgerAccounts = this.accountRepository.findByLedger(ledgerEntity);
    return ledgerAccounts.size() > 0;
  }

  private void addSubLedgers(final Ledger parentLedger,
                             final List<LedgerEntity> subLedgerEntities) {
    if (subLedgerEntities != null) {
      final List<Ledger> subLedgers = new ArrayList<>(subLedgerEntities.size());
      subLedgerEntities.forEach(subLedgerEntity -> {
        final Ledger subLedger = LedgerMapper.map(subLedgerEntity);
        final List<LedgerEntity> subSubLedgerEntities =
            this.ledgerRepository.findByParentLedger(subLedgerEntity);
        this.addSubLedgers(subLedger, subSubLedgerEntities);
        subLedgers.add(subLedger);
      });
      parentLedger.setSubLedgers(subLedgers);
    }
  }
}