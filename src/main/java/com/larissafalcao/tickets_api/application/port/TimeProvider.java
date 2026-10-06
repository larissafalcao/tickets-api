package com.larissafalcao.tickets_api.application.port;

import java.time.Instant;

public interface TimeProvider {
    Instant now();
}
