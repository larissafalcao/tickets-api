package com.larissafalcao.tickets_api.application.port;

public interface TransactionRunner {
    void execute(Runnable work);
}
