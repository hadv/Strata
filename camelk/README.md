# Apache Camel K — Country-Based Fee Router

This directory contains a standalone Camel K YAML DSL route that can be deployed
directly on a Kubernetes cluster with the **Camel K operator** installed.

The route calculates transaction fees by routing requests to country-specific
microservices (US, EU, VN) based on the country field in the request.

## Prerequisites

1. A running Kubernetes cluster (Minikube, Kind, GKE, EKS, etc.)
2. [Camel K operator](https://camel.apache.org/camel-k/latest/installation/installation.html) installed:

```bash
# Install the Camel K CLI
brew install kamel          # macOS

# Install the operator into your cluster
kamel install --namespace strata
```

3. Fee services (US, EU, VN) deployed and accessible within the cluster.

## Deploy the Route

```bash
# Deploy the transfer-fee-router with service URL configurations
kamel run transfer-fee-router.camel.yaml \
         --namespace strata \
         -p router.fee-service-urls.US=http://fee-service-us:8090 \
         -p router.fee-service-urls.EU=http://fee-service-eu:8090 \
         -p router.fee-service-urls.VN=http://fee-service-vn:8090 \
         -p router.default-fee-service-url=http://fee-service-default:8090
```

## Verify Deployment

```bash
# Check integration status
kamel get --namespace strata

# Stream logs
kamel log transfer-fee-router --namespace strata
```

## Test the Route (via Port Forward)

```bash
# Port-forward the Camel K integration pod to localhost:8082
kubectl port-forward <pod-name> 8082:8080 --namespace strata

# Test US fee (0.5%)
curl -s -X POST http://localhost:8082/transfer/calculate-fee \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000, "country": "US"}' | jq
```
