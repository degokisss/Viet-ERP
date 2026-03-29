# ============================================================
# AKS Cluster Outputs / Output AKS Cluster
# ============================================================

output "aks_cluster_name" {
  description = "Tên AKS cluster / AKS cluster name"
  value       = azurerm_kubernetes_cluster.main.name
}

output "aks_cluster_id" {
  description = "ID của AKS cluster / AKS cluster ID"
  value       = azurerm_kubernetes_cluster.main.id
}

output "aks_kube_config_raw" {
  description = "Raw kube config / Raw kube config"
  value       = azurerm_kubernetes_cluster.main.kube_config_raw
  sensitive   = true
}

output "aks_kube_config_context" {
  description = "Kubectl context name / Kubectl context name"
  value       = azurerm_kubernetes_cluster.main.kube_config.0.cluster_name
}

output "aks_node_resource_group" {
  description = "Resource group cho AKS node / AKS node resource group"
  value       = azurerm_kubernetes_cluster.main.node_resource_group
}

# ============================================================
# Azure Database for PostgreSQL Outputs / Output Azure Database for PostgreSQL
# ============================================================

output "postgresql_fqdn" {
  description = "FQDN của PostgreSQL server / PostgreSQL server FQDN"
  value       = azurerm_postgresql_flexible_server.main.fqdn
}

output "postgresql_id" {
  description = "ID của PostgreSQL server / PostgreSQL server ID"
  value       = azurerm_postgresql_flexible_server.main.id
}

output "postgresql_host" {
  description = "Host của PostgreSQL server / PostgreSQL server host"
  value       = azurerm_postgresql_flexible_server.main.fqdn
}

output "postgresql_port" {
  description = "Port của PostgreSQL server / PostgreSQL server port"
  value       = 5432
}

output "postgresql_database_name" {
  description = "Tên database mặc định / Default database name"
  value       = var.postgresql_database_name
}

output "postgresql_administrator_login" {
  description = "Admin username / Admin username"
  value       = var.postgresql_administrator_login
  sensitive   = true
}

# ============================================================
# Azure Cache for Redis Outputs / Output Azure Cache for Redis
# ============================================================

output "redis_hostname" {
  description = "Hostname của Redis / Redis hostname"
  value       = azurerm_redis_cache.main.hostname
}

output "redis_port" {
  description = "Port của Redis / Redis port"
  value       = azurerm_redis_cache.main.port
}

output "redis_ssl_port" {
  description = "SSL port của Redis / Redis SSL port"
  value       = azurerm_redis_cache.main.ssl_port
}

output "redis_primary_access_key" {
  description = "Primary access key cho Redis / Redis primary access key"
  value       = azurerm_redis_cache.main.primary_access_key
  sensitive   = true
}

output "redis_connection_string" {
  description = "Redis connection string / Redis connection string"
  value       = "redis://:${azurerm_redis_cache.main.primary_access_key}@${azurerm_redis_cache.main.hostname}:${azurerm_redis_cache.main.ssl_port}?ssl=true"
  sensitive   = true
}

# ============================================================
# Virtual Network Outputs / Output Virtual Network
# ============================================================

output "vnet_id" {
  description = "ID của VNet / VNet ID"
  value       = azurerm_virtual_network.main.id
}

output "vnet_name" {
  description = "Tên VNet / VNet name"
  value       = azurerm_virtual_network.main.name
}

output "aks_subnet_id" {
  description = "ID của AKS subnet / AKS subnet ID"
  value       = azurerm_subnet.aks.id
}

output "database_subnet_id" {
  description = "ID của Database subnet / Database subnet ID"
  value       = azurerm_subnet.database.id
}

# ============================================================
# Storage Account Outputs / Output Storage Account
# ============================================================

output "storage_account_id" {
  description = "ID của Storage Account / Storage Account ID"
  value       = azurerm_storage_account.main.id
}

output "storage_account_name" {
  description = "Tên Storage Account / Storage Account name"
  value       = azurerm_storage_account.main.name
}

output "storage_primary_blob_endpoint" {
  description = "Primary blob endpoint / Primary blob endpoint"
  value       = azurerm_storage_account.main.primary_blob_endpoint
}

output "storage_uploads_share_id" {
  description = "ID của uploads share / Uploads share ID"
  value       = try(azurerm_storage_share.uploads[0].id, null)
}

output "storage_backups_share_id" {
  description = "ID của backups share / Backups share ID"
  value       = try(azurerm_storage_share.backups[0].id, null)
}

# ============================================================
# Resource Group Outputs / Output Resource Group
# ============================================================

output "resource_group_id" {
  description = "ID của Resource Group / Resource Group ID"
  value       = azurerm_resource_group.main.id
}

output "resource_group_name" {
  description = "Tên Resource Group / Resource Group name"
  value       = azurerm_resource_group.main.name
}

output "resource_group_location" {
  description = "Location của Resource Group / Resource Group location"
  value       = azurerm_resource_group.main.location
}

# ============================================================
# Kubernetes Configuration Outputs / Output Kubernetes Configuration
# ============================================================

output "kubernetes_cluster_type" {
  description = "Loại cluster: AKS / Cluster type"
  value       = "aks"
}

output "kubernetes_cluster_host" {
  description = "Host để kết nối tới Kubernetes cluster / Kubernetes cluster host"
  value       = azurerm_kubernetes_cluster.main.kube_config.0.host
  sensitive   = true
}

output "kubectl_config_path" {
  description = "Path to kubeconfig file / Path to kubeconfig file"
  value       = "~/.kube/config"
}
