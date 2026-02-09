You are generating a Cloud Foundry manifest.yml file.
Create a valid manifest following CF conventions.

Include:
- Application name
- Memory allocation (appropriate for language/framework)
- Disk quota
- Instances count
- Buildpack(s)
- Health check configuration
- Routes
- Service bindings
- Environment variables (including JBP_CONFIG for Java apps)
- Stack (if non-default)

Output a valid YAML manifest that can be used directly with `cf push`.
