package com.portfolio.releasegovernance.adapter;

import static com.portfolio.releasegovernance.domain.DomainEnums.SourceType;

import com.portfolio.releasegovernance.domain.Product;
import java.util.List;

public interface VersionSourceAdapter {
    SourceType sourceType();
    List<ReleaseCandidate> fetch(Product product);
}
