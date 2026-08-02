package com.portfolio.releasegovernance.policy;

import static com.portfolio.releasegovernance.domain.DomainEnums.*;

import com.portfolio.releasegovernance.domain.AvailableRelease;
import com.portfolio.releasegovernance.domain.ReleaseEnvironment;
import com.portfolio.releasegovernance.domain.ReleasePolicy;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReleasePolicyEvaluator {

    public PolicyDecision evaluate(ReleasePolicy policy, ReleaseEnvironment environment,
                                   AvailableRelease release, String installedVersion,
                                   boolean approved, boolean emergency, Instant now) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> bypasses = new ArrayList<>();
        SemanticVersion target = SemanticVersion.parse(release.getVersion());

        if (release.getStatus() == ReleaseStatus.BLOCKED || isExplicitlyBlocked(policy, target.canonical())) {
            violations.add("VERSION_BLOCKED");
        }
        if (environment.getType() == EnvironmentType.PRODUCTION
                && policy.isProhibitPrereleaseInProduction() && target.isPrerelease()) {
            violations.add("PRERELEASE_FORBIDDEN_IN_PRODUCTION");
        }
        if (policy.getMinimumSupportedVersion() != null && !policy.getMinimumSupportedVersion().isBlank()
                && target.compareTo(SemanticVersion.parse(policy.getMinimumSupportedVersion())) < 0) {
            violations.add("BELOW_MINIMUM_SUPPORTED_VERSION");
        }

        if (environment.getType() == EnvironmentType.PRODUCTION
                && policy.isRequireProductionApproval() && !approved) {
            if (emergency && policy.isEmergencyBypassAllowed()) bypasses.add("PRODUCTION_APPROVAL_BYPASSED");
            else violations.add("PRODUCTION_APPROVAL_REQUIRED");
        }

        if (policy.isEnforceMaintenanceWindow() && !insideWindow(environment, now)) {
            if (emergency && policy.isEmergencyBypassAllowed()) bypasses.add("MAINTENANCE_WINDOW_BYPASSED");
            else violations.add("OUTSIDE_MAINTENANCE_WINDOW");
        }

        if (installedVersion != null && !installedVersion.isBlank()) {
            int comparison = target.compareTo(SemanticVersion.parse(installedVersion));
            if (comparison == 0) warnings.add("VERSION_ALREADY_INSTALLED");
            if (comparison < 0 && !emergency) violations.add("DOWNGRADE_REQUIRES_EMERGENCY");
            if (comparison < 0 && emergency) bypasses.add("EMERGENCY_DOWNGRADE");
        }

        if (emergency && !policy.isEmergencyBypassAllowed()) {
            violations.add("EMERGENCY_ROLLOUT_DISABLED");
        }
        return PolicyDecision.of(violations, warnings, bypasses);
    }

    private boolean isExplicitlyBlocked(ReleasePolicy policy, String version) {
        if (policy.getBlockedVersions() == null || policy.getBlockedVersions().isBlank()) return false;
        return Arrays.stream(policy.getBlockedVersions().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .anyMatch(pattern -> pattern.endsWith(".*")
                        ? version.startsWith(pattern.substring(0, pattern.length() - 1))
                        : version.equalsIgnoreCase(pattern));
    }

    private boolean insideWindow(ReleaseEnvironment environment, Instant now) {
        LocalTime start = environment.getMaintenanceStart();
        LocalTime end = environment.getMaintenanceEnd();
        if (start == null || end == null || environment.getMaintenanceDays() == null
                || environment.getMaintenanceDays().isBlank()) return true;
        ZonedDateTime local = now.atZone(ZoneId.of(environment.getZoneId()));
        boolean allowedDay = Arrays.stream(environment.getMaintenanceDays().split(","))
                .map(String::trim)
                .anyMatch(day -> day.equalsIgnoreCase(local.getDayOfWeek().name()));
        if (!allowedDay) return false;
        LocalTime time = local.toLocalTime();
        return start.equals(end) || (start.isBefore(end)
                ? !time.isBefore(start) && time.isBefore(end)
                : !time.isBefore(start) || time.isBefore(end));
    }
}
