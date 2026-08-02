package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.SourceType;

import com.portfolio.releasegovernance.domain.Product;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ManualVersionSourceAdapter implements VersionSourceAdapter {
    @Override public SourceType sourceType() { return SourceType.MANUAL; }
    @Override public List<ReleaseCandidate> fetch(Product product) { return List.of(); }
}
