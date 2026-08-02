package com.portfolio.releasegovernance.api;

import static com.portfolio.releasegovernance.api.ApiContracts.*;

import com.portfolio.releasegovernance.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GovernanceApiController {
    private final ProductService products;
    private final ReleaseImportService imports;
    private final ApprovalService approvals;
    private final DeploymentService deployments;
    private final ReleasePolicyService policies;
    private final DashboardService dashboard;
    private final AuditQueryService audit;

    public GovernanceApiController(ProductService products, ReleaseImportService imports,
                                   ApprovalService approvals, DeploymentService deployments,
                                   ReleasePolicyService policies, DashboardService dashboard,
                                   AuditQueryService audit) {
        this.products = products;
        this.imports = imports;
        this.approvals = approvals;
        this.deployments = deployments;
        this.policies = policies;
        this.dashboard = dashboard;
        this.audit = audit;
    }

    @GetMapping("/products")
    public List<ProductResponse> products() { return products.products(); }

    @GetMapping("/products/{id}")
    public ProductResponse product(@PathVariable UUID id) { return products.product(id); }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader(name = "X-Actor", defaultValue = "api-client") String actor,
            HttpServletRequest servletRequest) {
        ProductResponse response = products.createProduct(request, actor(actor), correlation(servletRequest));
        return ResponseEntity.created(java.net.URI.create("/api/products/" + response.id())).body(response);
    }

    @PostMapping("/products/{id}/refresh")
    public ImportSummary refresh(@PathVariable UUID id,
                                 @RequestHeader(name = "X-Actor", defaultValue = "api-client") String actor,
                                 HttpServletRequest request) {
        return imports.refresh(id, actor(actor), correlation(request));
    }

    @GetMapping("/products/{id}/releases")
    public List<ReleaseResponse> releases(@PathVariable UUID id) { return imports.releases(id); }

    @PostMapping("/products/{id}/releases")
    public ResponseEntity<ImportSummary> manualRelease(
            @PathVariable UUID id,
            @Valid @RequestBody ManualReleaseRequest release,
            @RequestHeader(name = "X-Actor", defaultValue = "api-client") String actor,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imports.addManual(id, release, actor(actor), correlation(request)));
    }

    @GetMapping("/environments")
    public List<EnvironmentResponse> environments() { return products.environments(); }

    @PostMapping("/environments")
    public ResponseEntity<EnvironmentResponse> createEnvironment(@Valid @RequestBody CreateEnvironmentRequest request) {
        EnvironmentResponse response = products.createEnvironment(request);
        return ResponseEntity.created(java.net.URI.create("/api/environments/" + response.id())).body(response);
    }

    @GetMapping("/products/{id}/policy")
    public PolicyResponse policy(@PathVariable UUID id) { return policies.get(id); }

    @PutMapping("/products/{id}/policy")
    public PolicyResponse updatePolicy(@PathVariable UUID id, @Valid @RequestBody UpdatePolicyRequest request,
                                       HttpServletRequest servletRequest) {
        return policies.update(id, request, correlation(servletRequest));
    }

    @GetMapping("/approvals")
    public List<ApprovalResponse> approvals() { return approvals.list(); }

    @PostMapping("/approvals")
    public ResponseEntity<ApprovalResponse> requestApproval(@Valid @RequestBody RequestApprovalRequest request,
                                                            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(approvals.request(request, correlation(servletRequest)));
    }

    @PostMapping("/approvals/{id}/decision")
    public ApprovalResponse decideApproval(@PathVariable UUID id, @Valid @RequestBody DecideApprovalRequest request,
                                           HttpServletRequest servletRequest) {
        return approvals.decide(id, request, correlation(servletRequest));
    }

    @GetMapping("/deployments")
    public List<DeploymentResponse> deployments() { return deployments.history(); }

    @PostMapping("/deployments")
    public ResponseEntity<DeploymentResponse> schedule(@Valid @RequestBody ScheduleDeploymentRequest request,
                                                       HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deployments.schedule(request, correlation(servletRequest)));
    }

    @PostMapping("/deployments/{id}/complete")
    public DeploymentResponse complete(@PathVariable UUID id, @Valid @RequestBody CompleteDeploymentRequest request,
                                       HttpServletRequest servletRequest) {
        return deployments.complete(id, request, correlation(servletRequest));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() { return dashboard.dashboard(); }

    @GetMapping("/audit/{aggregateType}/{aggregateId}")
    public List<AuditResponse> audit(@PathVariable String aggregateType, @PathVariable UUID aggregateId,
                                    @RequestParam(defaultValue = "100") int limit) {
        return audit.events(aggregateType, aggregateId, limit);
    }

    private static String actor(String value) {
        String cleaned = value == null || value.isBlank() ? "api-client" : value.trim();
        return cleaned.substring(0, Math.min(cleaned.length(), 120));
    }

    private static String correlation(HttpServletRequest request) {
        String supplied = request.getHeader("X-Correlation-ID");
        return supplied == null || supplied.isBlank() ? UUID.randomUUID().toString()
                : supplied.substring(0, Math.min(supplied.length(), 100));
    }
}
