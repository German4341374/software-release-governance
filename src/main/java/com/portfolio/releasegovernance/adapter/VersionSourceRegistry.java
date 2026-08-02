package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.SourceType;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class VersionSourceRegistry {
    private final Map<SourceType, VersionSourceAdapter> adapters;

    public VersionSourceRegistry(List<VersionSourceAdapter> adapters) {
        EnumMap<SourceType, VersionSourceAdapter> values = new EnumMap<>(SourceType.class);
        adapters.forEach(adapter -> values.put(adapter.sourceType(), adapter));
        this.adapters = Map.copyOf(values);
    }

    public VersionSourceAdapter adapterFor(SourceType sourceType) {
        VersionSourceAdapter adapter = adapters.get(sourceType);
        if (adapter == null) throw new VersionSourceException("No adapter is registered for " + sourceType + ".", null);
        return adapter;
    }
}
