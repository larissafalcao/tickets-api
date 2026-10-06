package com.larissafalcao.tickets_api.infrastructure.config;

import com.larissafalcao.tickets_api.application.port.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class SpringTransactionRunner implements TransactionRunner {
    private final TransactionTemplate transactions;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public void execute(Runnable work) {
        transactions.executeWithoutResult(status -> work.run());
    }
}
