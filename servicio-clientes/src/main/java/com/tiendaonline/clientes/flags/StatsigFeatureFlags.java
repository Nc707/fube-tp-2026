package com.tiendaonline.clientes.flags;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.statsig.sdk.Statsig;
import com.statsig.sdk.StatsigUser;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Profile("statsig")
public class StatsigFeatureFlags implements FeatureFlags {

    private static final String GATE_MASK_PII = "mask-pii";

    private final String key;

    public StatsigFeatureFlags(@Value("${statsig.key}") String key) {
        this.key = key;
    }

    @PostConstruct
    void init() throws Exception {
        Statsig.initializeAsync(key).get();
    }

    @Override
    public boolean isMaskPiiEnabled(Long clientId) {
        try {
            return Statsig.checkGateSync(new StatsigUser(String.valueOf(clientId)), GATE_MASK_PII);
        } catch (Exception e) {
            return true;
        }
    }

    @PreDestroy
    void close() {
        Statsig.shutdown();
    }
}