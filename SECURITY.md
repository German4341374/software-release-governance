# Security policy

## Reporting

Do not open a public issue for a suspected vulnerability. Use GitHub's private vulnerability reporting feature for this repository and include a minimal reproduction, impact, and affected revision. Do not include real tokens, customer data, or proprietary release metadata.

## Supported version

Security fixes target the latest `main` revision until formal releases are published.

## Deployment guidance

This project is an educational governance service, not an internet-ready identity boundary. Before production use:

- place it behind TLS and an identity-aware proxy;
- authorize policy, approval, import, and deployment operations separately;
- replace actor headers with authenticated identity claims;
- use a managed secret store and a least-privilege PostgreSQL role;
- restrict egress to approved release origins;
- export audit events to immutable external storage;
- configure backups, restore tests, monitoring, and retention;
- pin container images by digest in the deployment platform.

The optional GitHub token needs read access only to public metadata unless private repositories are explicitly in scope. Never grant package write or administration permissions.
