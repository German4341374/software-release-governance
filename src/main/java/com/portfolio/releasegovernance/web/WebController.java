package com.portfolio.releasegovernance.web;

import static com.portfolio.releasegovernance.api.ApiContracts.*;
import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.adapter.VersionSourceException;
import com.portfolio.releasegovernance.service.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
    private final ProductService products;
    private final ReleaseImportService imports;
    private final DashboardService dashboard;
    private final ApprovalService approvals;
    private final DeploymentService deployments;
    private final ReleasePolicyService policies;

    public WebController(ProductService products, ReleaseImportService imports, DashboardService dashboard,
                         ApprovalService approvals, DeploymentService deployments, ReleasePolicyService policies) {
        this.products = products;
        this.imports = imports;
        this.dashboard = dashboard;
        this.approvals = approvals;
        this.deployments = deployments;
        this.policies = policies;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboard.dashboard());
        model.addAttribute("products", products.products());
        model.addAttribute("environments", products.environments());
        model.addAttribute("sourceTypes", SourceType.values());
        model.addAttribute("environmentTypes", EnvironmentType.values());
        return "index";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam String name, @RequestParam String vendor,
                                @RequestParam(required = false) String description,
                                @RequestParam SourceType sourceType,
                                @RequestParam(required = false) String sourceReference,
                                RedirectAttributes redirect) {
        ProductResponse created = products.createProduct(new CreateProductRequest(
                name, vendor, description, sourceType, sourceReference, ReleaseChannel.STABLE),
                "web-ui", UUID.randomUUID().toString());
        redirect.addFlashAttribute("message", "Product registered.");
        return "redirect:/products/" + created.id();
    }

    @PostMapping("/environments")
    public String createEnvironment(@RequestParam String name, @RequestParam EnvironmentType type,
                                    @RequestParam(defaultValue = "UTC") String zoneId,
                                    @RequestParam(required = false) LocalTime maintenanceStart,
                                    @RequestParam(required = false) LocalTime maintenanceEnd,
                                    @RequestParam(required = false) String maintenanceDays,
                                    RedirectAttributes redirect) {
        products.createEnvironment(new CreateEnvironmentRequest(
                name, type, zoneId, maintenanceStart, maintenanceEnd, maintenanceDays));
        redirect.addFlashAttribute("message", "Environment registered.");
        return "redirect:/";
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable UUID id, Model model) {
        model.addAttribute("product", products.product(id));
        model.addAttribute("releases", imports.releases(id));
        model.addAttribute("policy", policies.get(id));
        model.addAttribute("environments", products.environments());
        model.addAttribute("channels", ReleaseChannel.values());
        return "product";
    }

    @PostMapping("/products/{id}/refresh")
    public String refresh(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            ImportSummary result = imports.refresh(id, "web-ui", UUID.randomUUID().toString());
            redirect.addFlashAttribute("message", result.imported() + " new release(s) imported.");
        } catch (VersionSourceException exception) {
            redirect.addFlashAttribute("error", exception.getMessage() + " Last known releases remain available.");
        }
        return "redirect:/products/" + id;
    }

    @PostMapping("/products/{id}/releases")
    public String manualRelease(@PathVariable UUID id, @RequestParam String version,
                                @RequestParam ReleaseChannel channel,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirect) {
        ImportSummary result = imports.addManual(id, new ManualReleaseRequest(
                version, channel, null, null, notes, Instant.now()), "web-ui", UUID.randomUUID().toString());
        redirect.addFlashAttribute("message", result.imported() == 0 ? "Release already exists." : "Release imported.");
        return "redirect:/products/" + id;
    }

    @PostMapping("/approvals")
    public String requestApproval(@RequestParam UUID productId, @RequestParam UUID releaseId,
                                  @RequestParam UUID environmentId, RedirectAttributes redirect) {
        approvals.request(new RequestApprovalRequest(releaseId, environmentId, "web-ui", "Requested from web interface"),
                UUID.randomUUID().toString());
        redirect.addFlashAttribute("message", "Approval requested.");
        return "redirect:/products/" + productId;
    }

    @GetMapping("/approvals")
    public String approvals(Model model) {
        model.addAttribute("approvals", approvals.list());
        return "approvals";
    }

    @PostMapping("/approvals/{id}/decision")
    public String decide(@PathVariable UUID id, @RequestParam ApprovalStatus decision,
                         RedirectAttributes redirect) {
        approvals.decide(id, new DecideApprovalRequest(decision, "web-ui", "Decision from web interface"),
                UUID.randomUUID().toString());
        redirect.addFlashAttribute("message", "Approval updated.");
        return "redirect:/approvals";
    }

    @PostMapping("/deployments")
    public String schedule(@RequestParam UUID productId, @RequestParam UUID releaseId,
                           @RequestParam UUID environmentId,
                           @RequestParam(defaultValue = "false") boolean emergency,
                           @RequestParam(required = false) String reason,
                           RedirectAttributes redirect) {
        deployments.schedule(new ScheduleDeploymentRequest(releaseId, environmentId, emergency, "web-ui", reason),
                UUID.randomUUID().toString());
        redirect.addFlashAttribute("message", "Deployment recorded as scheduled.");
        return "redirect:/products/" + productId;
    }

    @GetMapping("/deployments")
    public String deployments(Model model) {
        model.addAttribute("deployments", deployments.history());
        return "deployments";
    }

    @PostMapping("/deployments/{id}/complete")
    public String complete(@PathVariable UUID id, @RequestParam boolean successful,
                           @RequestParam(required = false) String failureReason,
                           RedirectAttributes redirect) {
        deployments.complete(id, new CompleteDeploymentRequest(successful, "web-ui", failureReason),
                UUID.randomUUID().toString());
        redirect.addFlashAttribute("message", "Deployment result recorded.");
        return "redirect:/deployments";
    }
}
