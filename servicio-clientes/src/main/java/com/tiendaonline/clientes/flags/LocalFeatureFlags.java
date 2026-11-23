package com.tiendaonline.clientes.flags;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!statsig")
public class LocalFeatureFlags implements FeatureFlags {

    private final boolean maskPii;

    public LocalFeatureFlags(@Value("${feature-flags.mask-pii:true}") boolean maskPii) {
        this.maskPii = maskPii;
    }

    @Override
    public boolean isMaskPiiEnabled() {
        return maskPii;
    }
}