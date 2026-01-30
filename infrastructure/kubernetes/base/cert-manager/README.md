# cert-manager Configuration for QAWave

This directory contains the cert-manager configuration for automatic TLS certificate management.

## Overview

cert-manager automates the management and issuance of TLS certificates from various sources, including Let's Encrypt.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            cert-manager                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────┐     ┌────────────────┐     ┌────────────────┐          │
│  │ ClusterIssuer  │     │  Certificate   │     │    Secret      │          │
│  │ letsencrypt-   │────▶│  qawave-tls    │────▶│  qawave-tls    │          │
│  │ prod           │     │                │     │  (TLS cert)    │          │
│  └────────────────┘     └────────────────┘     └────────────────┘          │
│         │                       │                      │                    │
│         │                       │                      │                    │
│         ▼                       ▼                      ▼                    │
│  ┌────────────────┐     ┌────────────────┐     ┌────────────────┐          │
│  │  Let's Encrypt │     │    Ingress     │     │    Pod TLS     │          │
│  │  ACME Server   │     │  (terminates)  │     │ (if mounted)   │          │
│  └────────────────┘     └────────────────┘     └────────────────┘          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Components

### ClusterIssuers

| Name | Purpose | Server |
|------|---------|--------|
| `letsencrypt-prod` | Production certificates | Let's Encrypt Production |
| `letsencrypt-staging` | Testing (untrusted) | Let's Encrypt Staging |
| `selfsigned-issuer` | Self-signed certs | N/A |
| `qawave-ca-issuer` | Internal CA | Internal PKI |

### Certificates

| Name | Namespace | Domains | Issuer |
|------|-----------|---------|--------|
| `qawave-tls` | qawave | *.qawave.io | letsencrypt-prod |
| `qawave-staging-tls` | qawave-staging | *.staging.qawave.io | letsencrypt-prod |
| `qawave-ca` | cert-manager | Internal CA | selfsigned-issuer |
| `backend-internal-tls` | qawave | Internal services | qawave-ca-issuer |

## Usage

### Adding TLS to Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - myapp.qawave.io
      secretName: myapp-tls
  rules:
    - host: myapp.qawave.io
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: myapp
                port:
                  number: 80
```

### Creating a New Certificate

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: myapp-tls
  namespace: qawave
spec:
  secretName: myapp-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - myapp.qawave.io
```

## Troubleshooting

### Check Certificate Status

```bash
# List all certificates
kubectl get certificates -A

# Describe certificate
kubectl describe certificate qawave-tls -n qawave

# Check certificate secret
kubectl get secret qawave-tls -n qawave -o yaml
```

### Check Certificate Requests

```bash
# List certificate requests
kubectl get certificaterequests -A

# Describe pending request
kubectl describe certificaterequest <name> -n <namespace>
```

### Check Orders and Challenges

```bash
# List ACME orders
kubectl get orders -A

# List challenges
kubectl get challenges -A

# Debug challenge
kubectl describe challenge <name> -n <namespace>
```

### Common Issues

1. **Challenge failing**: Check DNS resolution and ingress configuration
2. **Rate limited**: Wait or use staging issuer
3. **Certificate not renewing**: Check cert-manager logs

### Logs

```bash
# cert-manager controller logs
kubectl logs -n cert-manager -l app=cert-manager -f

# Webhook logs
kubectl logs -n cert-manager -l app=webhook -f
```

## Rate Limits

### Let's Encrypt Production

- 50 certificates per registered domain per week
- 5 duplicate certificates per week
- 300 pending authorizations per account per week

### Let's Encrypt Staging

- No rate limits (use for testing)
- Certificates are not trusted by browsers

## Security Considerations

1. **Private Keys**: Stored as Kubernetes secrets
2. **ACME Account**: Private key in secret
3. **HTTP-01 Challenge**: Requires port 80 open
4. **Renewal**: Automatic, 30 days before expiry

## Related Documentation

- [cert-manager Documentation](https://cert-manager.io/docs/)
- [Let's Encrypt](https://letsencrypt.org/docs/)
- [ACME Protocol](https://datatracker.ietf.org/doc/html/rfc8555)
