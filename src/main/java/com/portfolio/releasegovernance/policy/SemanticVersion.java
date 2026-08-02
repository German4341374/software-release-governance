package com.portfolio.releasegovernance.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern PATTERN = Pattern.compile(
            "^[vV]?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
                    "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?" +
                    "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> prerelease;
    private final String canonical;

    private SemanticVersion(int major, int minor, int patch, List<String> prerelease, String build) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = List.copyOf(prerelease);
        this.canonical = major + "." + minor + "." + patch
                + (prerelease.isEmpty() ? "" : "-" + String.join(".", prerelease))
                + (build == null ? "" : "+" + build);
    }

    public static SemanticVersion parse(String value) {
        if (value == null) throw new IllegalArgumentException("Version is required.");
        Matcher matcher = PATTERN.matcher(value.trim());
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid semantic version: " + value);
        List<String> identifiers = new ArrayList<>();
        if (matcher.group(4) != null) {
            for (String identifier : matcher.group(4).split("\\.")) {
                if (identifier.matches("\\d+") && identifier.length() > 1 && identifier.startsWith("0")) {
                    throw new IllegalArgumentException("Numeric prerelease identifiers cannot contain leading zeroes.");
                }
                identifiers.add(identifier);
            }
        }
        return new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                identifiers,
                matcher.group(5));
    }

    public boolean isPrerelease() { return !prerelease.isEmpty(); }
    public String canonical() { return canonical; }

    @Override
    public int compareTo(SemanticVersion other) {
        int core = Integer.compare(major, other.major);
        if (core == 0) core = Integer.compare(minor, other.minor);
        if (core == 0) core = Integer.compare(patch, other.patch);
        if (core != 0) return core;
        if (prerelease.isEmpty() && other.prerelease.isEmpty()) return 0;
        if (prerelease.isEmpty()) return 1;
        if (other.prerelease.isEmpty()) return -1;
        int length = Math.min(prerelease.size(), other.prerelease.size());
        for (int i = 0; i < length; i++) {
            String left = prerelease.get(i);
            String right = other.prerelease.get(i);
            boolean leftNumeric = left.matches("\\d+");
            boolean rightNumeric = right.matches("\\d+");
            int result;
            if (leftNumeric && rightNumeric) result = Long.compare(Long.parseLong(left), Long.parseLong(right));
            else if (leftNumeric) result = -1;
            else if (rightNumeric) result = 1;
            else result = left.compareTo(right);
            if (result != 0) return result;
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    @Override public String toString() { return canonical; }
    @Override public boolean equals(Object value) { return value instanceof SemanticVersion other && compareTo(other) == 0; }
    @Override public int hashCode() { return Objects.hash(major, minor, patch, prerelease); }
}
