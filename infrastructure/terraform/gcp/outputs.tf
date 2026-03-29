# ============================================================
# GKE Cluster Outputs / Output GKE Cluster
# ============================================================

output "gke_cluster_name" {
  description = "Tên GKE cluster / GKE cluster name"
  value       = google_container_cluster.main.name
}

output "gke_cluster_endpoint" {
  description = "Endpoint của GKE cluster / GKE cluster endpoint"
  value       = google_container_cluster.main.endpoint
  sensitive   = true
}

output "gke_cluster_ca_certificate" {
  description = "CA certificate của GKE cluster / GKE cluster CA certificate"
  value       = google_container_cluster.main.master_auth.0.cluster_ca_certificate
  sensitive   = true
}

output "gke_region" {
  description = "Region của GKE cluster / GKE cluster region"
  value       = var.region
}

# ============================================================
# Cloud SQL Outputs / Output Cloud SQL
# ============================================================

output "cloudsql_instance_name" {
  description = "Tên Cloud SQL instance / Cloud SQL instance name"
  value       = google_sql_database_instance.main.name
}

output "cloudsql_instance_connection_name" {
  description = "Connection name cho Cloud SQL / Cloud SQL connection name"
  value       = google_sql_database_instance.main.connection_name
}

output "cloudsql_private_ip_address" {
  description = "Private IP address của Cloud SQL / Cloud SQL private IP address"
  value       = google_sql_database_instance.main.private_ip_address
}

output "cloudsql_database_version" {
  description = "PostgreSQL version / PostgreSQL version"
  value       = google_sql_database_instance.main.database_version
}

output "cloudsql_public_ip_address" {
  description = "Public IP address của Cloud SQL (nếu enabled) / Cloud SQL public IP address"
  value       = try(google_sql_database_instance.main.public_ip_address, null)
}

# ============================================================
# Memorystore Redis Outputs / Output Memorystore Redis
# ============================================================

output "memorystore_redis_host" {
  description = "Host của Memorystore Redis / Memorystore Redis host"
  value       = google_redis_instance.main.host
}

output "memorystore_redis_port" {
  description = "Port của Memorystore Redis / Memorystore Redis port"
  value       = google_redis_instance.main.port
}

output "memorystore_redis_auth_string" {
  description = "Auth string cho Memorystore Redis / Memorystore Redis auth string"
  value       = try(google_redis_instance.main.auth_string, "")
  sensitive   = true
}

output "memorystore_redis_connection_string" {
  description = "Redis connection string / Redis connection string"
  value       = "redis://${google_redis_instance.main.host}:${google_redis_instance.main.port}"
  sensitive   = true
}

# ============================================================
# VPC Network Outputs / Output VPC Network
# ============================================================

output "vpc_name" {
  description = "Tên VPC network / VPC network name"
  value       = google_compute_network.main.name
}

output "vpc_self_link" {
  description = "Self link của VPC network / VPC network self link"
  value       = google_compute_network.main.self_link
}

output "primary_subnet_name" {
  description = "Tên primary subnet / Primary subnet name"
  value       = google_compute_subnetwork.primary.name
}

output "primary_subnet_self_link" {
  description = "Self link của primary subnet / Primary subnet self link"
  value       = google_compute_subnetwork.primary.self_link
}

# ============================================================
# Cloud Storage Outputs / Output Cloud Storage
# ============================================================

output "gcs_uploads_bucket_name" {
  description = "Tên GCS uploads bucket / GCS uploads bucket name"
  value       = google_storage_bucket.uploads.name
}

output "gcs_uploads_bucket_url" {
  description = "URL của GCS uploads bucket / GCS uploads bucket URL"
  value       = google_storage_bucket.uploads.url
}

output "gcs_backups_bucket_name" {
  description = "Tên GCS backups bucket / GCS backups bucket name"
  value       = google_storage_bucket.backups.name
}

output "gcs_backups_bucket_url" {
  description = "URL của GCS backups bucket / GCS backups bucket URL"
  value       = google_storage_bucket.backups.url
}

# ============================================================
# IAM & Service Accounts Outputs / Output IAM & Service Accounts
# ============================================================

output "gke_service_account_email" {
  description = "Email của GKE service account / GKE service account email"
  value       = google_service_account.gke.email
}

output "cloudsql_proxy_service_account_email" {
  description = "Email của Cloud SQL Proxy service account / Cloud SQL Proxy service account email"
  value       = google_service_account.cloudsql_proxy.email
}

# ============================================================
# Kubernetes Configuration Outputs / Output Kubernetes Configuration
# ============================================================

output "kubernetes_cluster_type" {
  description = "Loại cluster: GKE / Cluster type"
  value       = "gke"
}

output "kubernetes_cluster_host" {
  description = "Host để kết nối tới Kubernetes cluster / Kubernetes cluster host"
  value       = google_container_cluster.main.endpoint
  sensitive   = true
}

output "kubectl_config_context" {
  description = "Context name cho kubectl config / kubectl config context"
  value       = "gke_${var.project_id}_${var.region}_${google_container_cluster.main.name}"
}
