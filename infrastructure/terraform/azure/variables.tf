# ============================================================
# Basic Variables / Biến Cơ Bản
# ============================================================

variable "project_name" {
  description = "Tên project / Project name"
  type        = string
  default     = "vierp"
}

variable "subscription_id" {
  description = "ID của Azure subscription / Azure subscription ID"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Môi trường triển khai (dev/staging/prod) / Deployment environment"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment phải là dev, staging hoặc prod / Environment must be dev, staging, or prod."
  }
}

variable "location" {
  description = "Azure region / Azure region"
  type        = string
  default     = "Southeast Asia"
}

variable "location_short" {
  description = "Short code cho Azure region / Azure region short code"
  type        = string
  default     = "sea"
}

# ============================================================
# Network Variables / Biến Mạng
# ============================================================

variable "vnet_cidr" {
  description = "CIDR block cho VNet / CIDR block for VNet"
  type        = string
  default     = "10.0.0.0/16"
  validation {
    condition     = can(cidrhost(var.vnet_cidr, 0))
    error_message = "VNet CIDR phải là một dải CIDR hợp lệ / VNet CIDR must be a valid CIDR range."
  }
}

variable "aks_subnet_cidr" {
  description = "CIDR block cho AKS subnet / CIDR block for AKS subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "database_subnet_cidr" {
  description = "CIDR block cho Database subnet / CIDR block for Database subnet"
  type        = string
  default     = "10.0.2.0/24"
}

variable "redis_subnet_cidr" {
  description = "CIDR block cho Redis subnet / CIDR block for Redis subnet"
  type        = string
  default     = "10.0.3.0/24"
}

# ============================================================
# AKS Cluster Variables / Biến AKS Cluster
# ============================================================

variable "kubernetes_version" {
  description = "Phiên bản Kubernetes cho AKS cluster / Kubernetes version for AKS cluster"
  type        = string
  default     = "1.29"
}

variable "aks_node_pools" {
  description = "Cấu hình các node pool cho AKS / AKS node pool configuration"
  type = map(object({
    vm_size       = string
    min_count     = number
    max_count     = number
    os_disk_size_gb = number
    labels        = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
  default = {
    system = {
      vm_size         = "Standard_D2s_v3"
      min_count       = 1
      max_count       = 3
      os_disk_size_gb = 50
      labels = {
        workload = "system"
      }
    }
  }
}

variable "enable_managed_identity" {
  description = "Bật Managed Identity cho AKS / Enable Managed Identity for AKS"
  type        = bool
  default     = true
}

variable "network_policy" {
  description = "Network policy plugin: 'azure' hoặc 'calico' / Network policy plugin"
  type        = string
  default     = "azure"
  validation {
    condition     = contains(["azure", "calico"], var.network_policy)
    error_message = "Network policy phải là azure hoặc calico / Network policy must be azure or calico."
  }
}

variable "enable_pod_security_policy" {
  description = "Bật Pod Security Policy / Enable Pod Security Policy"
  type        = bool
  default     = true
}

variable "enable_http_application_routing" {
  description = "Bật HTTP Application Routing / Enable HTTP Application Routing"
  type        = bool
  default     = false
}

variable "enable_azure_monitor" {
  description = "Bật Azure Monitor for containers / Enable Azure Monitor for containers"
  type        = bool
  default     = true
}

# ============================================================
# Azure Database for PostgreSQL Variables / Biến Azure Database for PostgreSQL
# ============================================================

variable "postgresql_sku_name" {
  description = "SKU name cho PostgreSQL / SKU name for PostgreSQL"
  type        = string
  default     = "B_Standard_B1ms"  # Burstable tier for dev/staging / Burstable tier
}

variable "postgresql_version" {
  description = "PostgreSQL version / PostgreSQL version"
  type        = string
  default     = "15"
  validation {
    condition     = contains(["11", "12", "13", "14", "15", "16"], var.postgresql_version)
    error_message = "PostgreSQL version phải là 11-16 / PostgreSQL version must be 11-16."
  }
}

variable "postgresql_storage_mb" {
  description = "Storage size trong MB / Storage size in MB"
  type        = number
  default     = 32768  # 32GB
  validation {
    condition     = var.postgresql_storage_mb >= 32768 && var.postgresql_storage_mb <= 16777216
    error_message = "Storage phải từ 32GB đến 16TB / Storage must be between 32GB and 16TB."
  }
}

variable "postgresql_backup_retention_days" {
  description = "Số ngày giữ backup / Number of days to retain backups"
  type        = number
  default     = 7
  validation {
    condition     = var.postgresql_backup_retention_days >= 1 && var.postgresql_backup_retention_days <= 35
    error_message = "Backup retention phải từ 1 đến 35 ngày / Backup retention must be 1-35 days."
  }
}

variable "postgresql_high_availability_enabled" {
  description = "Bật High Availability cho PostgreSQL / Enable High Availability for PostgreSQL"
  type        = bool
  default     = false
}

variable "postgresql_administrator_login" {
  description = "Username cho admin user / Admin username"
  type        = string
  default     = "vierpadmin"
  sensitive   = true
}

variable "postgresql_administrator_password" {
  description = "Password cho admin user (để trống để auto-generate) / Admin password (leave empty for auto-generate)"
  type        = string
  default     = ""
  sensitive   = true
}

variable "postgresql_database_name" {
  description = "Tên database mặc định / Default database name"
  type        = string
  default     = "vierp"
}

# ============================================================
# Azure Cache for Redis Variables / Biến Azure Cache for Redis
# ============================================================

variable "redis_sku" {
  description = "SKU cho Redis: 'Basic', 'Standard', or 'Premium' / Redis SKU"
  type        = string
  default     = "Basic"
  validation {
    condition     = contains(["Basic", "Standard", "Premium"], var.redis_sku)
    error_message = "Redis SKU phải là Basic, Standard hoặc Premium / Redis SKU must be Basic, Standard, or Premium."
  }
}

variable "redis_capacity" {
  description = "Capacity cho Redis: 0, 1, 2, 3, 4, 5, 6 / Redis capacity"
  type        = number
  default     = 0
  validation {
    condition     = contains([0, 1, 2, 3, 4, 5, 6], var.redis_capacity)
    error_message = "Redis capacity phải từ 0 đến 6 / Redis capacity must be 0-6."
  }
}

variable "redis_enable_non_ssl_port" {
  description = "Bật non-SSL port cho Redis / Enable non-SSL port for Redis"
  type        = bool
  default     = false
}

variable "redis_minimum_tls_version" {
  description = "Minimum TLS version cho Redis / Minimum TLS version for Redis"
  type        = string
  default     = "1.2"
  validation {
    condition     = contains(["1.0", "1.1", "1.2"], var.redis_minimum_tls_version)
    error_message = "TLS version phải là 1.0, 1.1, hoặc 1.2 / TLS version must be 1.0, 1.1, or 1.2."
  }
}

# ============================================================
# Storage Account Variables / Biến Storage Account
# ============================================================

variable "storage_account_tier" {
  description = "Storage account tier: 'Standard' hoặc 'Premium' / Storage account tier"
  type        = string
  default     = "Standard"
  validation {
    condition     = contains(["Standard", "Premium"], var.storage_account_tier)
    error_message = "Storage account tier phải là Standard hoặc Premium / Storage account tier must be Standard or Premium."
  }
}

variable "storage_account_replication_type" {
  description = "Replication type: LRS, GRS, RAGRS, ZRS / Storage account replication type"
  type        = string
  default     = "LRS"
  validation {
    condition     = contains(["LRS", "GRS", "RAGRS", "ZRS"], var.storage_account_replication_type)
    error_message = "Replication type phải là LRS, GRS, RAGRS hoặc ZRS / Replication type must be LRS, GRS, RAGRS, or ZRS."
  }
}

variable "storage_uploads_retention_days" {
  description = "Retention days cho uploads / Retention days for uploads"
  type        = number
  default     = 365
}

variable "storage_backups_retention_days" {
  description = "Retention days cho backups / Retention days for backups"
  type        = number
  default     = 30
}

# ============================================================
# Tagging Variables / Biến Tagging
# ============================================================

variable "tags" {
  description = "Các tags bổ sung cho tất cả tài nguyên / Additional tags for all resources"
  type        = map(string)
  default     = {}
}
