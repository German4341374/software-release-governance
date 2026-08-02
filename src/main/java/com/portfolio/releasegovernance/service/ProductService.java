package com.portfolio.releasegovernance.service;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.domain.*;
import com.portfolio.releasegovernance.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    private final EnvironmentRepository environments;
    private final ReleasePolicyRepository policies;
    private final AuditService audit;
    private final Clock clock;

    public ProductService(ProductRepository products, EnvironmentRepository environments,
                          ReleasePolicyRepository policies, AuditService audit, Clock clock) {
        this.products = products;
        this.environments = environments;
        this.policies = policies;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, String actor, String correlationId) {
        validateSource(request.sourceType(), request.sourceReference());
        Product product = new Product(request.name().trim(), request.vendor().trim(), trim(request.description()),
                request.sourceType(), trim(request.sourceReference()), request.defaultChannel());
        try {
            products.save(product);
            policies.save(new ReleasePolicy(product));
        } catch (DataIntegrityViolationException exception) {
            throw new GovernanceRuleException("PRODUCT_ALREADY_EXISTS", "A product with this name already exists.");
        }
        audit.record(AuditAction.PRODUCT_REGISTERED, "Product", product.getId(), actor,
                Map.of("name", product.getName(), "sourceType", product.getSourceType()), correlationId);
        return toProduct(product);
    }

    @Transactional
    public EnvironmentResponse createEnvironment(CreateEnvironmentRequest request) {
        try { ZoneId.of(request.zoneId()); }
        catch (DateTimeException exception) { throw new IllegalArgumentException("Unknown time zone: " + request.zoneId()); }
        if ((request.maintenanceStart() == null) != (request.maintenanceEnd() == null)) {
            throw new IllegalArgumentException("Both maintenance window boundaries must be supplied together.");
        }
        ReleaseEnvironment environment = new ReleaseEnvironment(
                request.name().trim(), request.type(), request.zoneId(), request.maintenanceStart(),
                request.maintenanceEnd(), trim(request.maintenanceDays()));
        try { environments.save(environment); }
        catch (DataIntegrityViolationException exception) {
            throw new GovernanceRuleException("ENVIRONMENT_ALREADY_EXISTS", "An environment with this name already exists.");
        }
        return toEnvironment(environment);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> products() { return products.findAll().stream().map(ProductService::toProduct).toList(); }

    @Transactional(readOnly = true)
    public ProductResponse product(UUID id) { return toProduct(findProduct(id)); }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> environments() {
        return environments.findAll().stream().map(ProductService::toEnvironment).toList();
    }

    Product findProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private void validateSource(SourceType sourceType, String reference) {
        if (sourceType != SourceType.MANUAL && (reference == null || reference.isBlank())) {
            throw new IllegalArgumentException("sourceReference is required for remote sources.");
        }
        if (sourceType == SourceType.GITHUB_RELEASES && !reference.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("GitHub sourceReference must use owner/repository.");
        }
    }

    static ProductResponse toProduct(Product value) {
        return new ProductResponse(value.getId(), value.getName(), value.getVendor(), value.getDescription(),
                value.getSourceType(), value.getSourceReference(), value.getDefaultChannel(),
                value.getLastCheckStatus(), value.getLastCheckedAt(), value.getLastCheckError(),
                value.getNextCheckAfter(), value.isActive(), value.getLockVersion());
    }

    static EnvironmentResponse toEnvironment(ReleaseEnvironment value) {
        return new EnvironmentResponse(value.getId(), value.getName(), value.getType(), value.getZoneId(),
                value.getMaintenanceStart(), value.getMaintenanceEnd(), value.getMaintenanceDays());
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
}
