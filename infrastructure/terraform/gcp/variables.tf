# ============================================================
# Basic Variables / Biến Cơ Bản
# ============================================================

variable "project_name" {
  description = "Tên project / Project name"
  type        = string
  default     = "vierp"
}

variable "project_id" {
  description = "ID của GCP project / GCP project ID"
  type        = string
  validation {
    condition     = can(regex("^[a-z][a-z0-9-]*[a-z0-9]$", var.project_id))
    error_message = "Project ID phải là lowercase với dấu gạch ngang / Project ID must be lowercase with hyphens."
  }
}

variable "environment" {
  description = "Môi trường triển khai (dev/staging/prod) / Deployment environment"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment phải là dev, staging hoặc prod / Environment must be dev, staging, or prod."
  }
}

variable "region" {
  description = "GCP region / GCP region"
  type        = string
  default     = "asia-southeast1"
}

# ============================================================
# VPC / Network Variables / Biến VPC / Mạng
# ============================================================

variable "vpc_cidr" {
  description = "CIDR block cho VPC / CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "VPC CIDR phải là một dải CIDR hợp lệ / VPC CIDR must be a valid CIDR range."
  }
}

variable "primary_subnet_cidr" {
  description = "CIDR block cho primary subnet / CIDR block for primary subnet"
  type        = string
  default     = "10.0.0.0/20"
}

variable "secondary_subnet_cidr" {
  description = "CIDR block cho secondary subnet (nếu cần) / CIDR block for secondary subnet"
  type        = string
  default     = "10.0.16.0/20"
}

variable "pods_secondary_range_cidr" {
  description = "CIDR block cho Pods secondary range / CIDR block for Pods"
  type        = string
  default     = "10.1.0.0/16"
}

variable "services_secondary_range_cidr" {
  description = "CIDR block cho Services secondary range / CIDR block for Services"
  type        = string
  default     = "10.2.0.0/16"
}

variable "enable_cloud_nat" {
  description = "Bật Cloud NAT / Enable Cloud NAT"
  type        = bool
  default     = true
}

# ============================================================
# GKE Cluster Variables / Biến GKE Cluster
# ============================================================

variable "gke_mode" {
  description = "GKE cluster mode: 'autopilot' hoặc 'standard' / GKE cluster mode"
  type        = string
  default     = "standard"
  validation {
    condition     = contains(["autopilot", "standard"], var.gke_mode)
    error_message = "GKE mode phải là autopilot hoặc standard / GKE mode must be autopilot or standard."
  }
}

variable "kubernetes_version" {
  description = "Phiên bản Kubernetes cho GKE cluster / Kubernetes version for GKE cluster"
  type        = string
  default     = "1.29"
}

variable "gke_node_pools" {
  description = "Cấu hình các node pool cho GKE / GKE node pool configuration"
  type = map(object({
    machine_type = string
    min_size     = number
    max_size     = number
    disk_size_gb = number
    labels       = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
  default = {
    default = {
      machine_type = "n2-standard-2"
      min_size     = 1
      max_size     = 5
      disk_size_gb = 50
      labels = {
        workload = "general"
      }
    }
  }
}

variable "enable_workload_identity" {
  description = "Bật Workload Identity cho GKE / Enable Workload Identity"
  type        = bool
  default     = true
}

variable "enable_network_policy" {
  description = "Bật Network Policy cho GKE / Enable Network Policy"
  type        = bool
  default     = true
}

variable "enable_stackdriver_logging" {
  description = "Bật Cloud Logging cho GKE / Enable Cloud Logging"
  type        = bool
  default     = true
}

variable "enable_stackdriver_monitoring" {
  description = "Bật Cloud Monitoring cho GKE / Enable Cloud Monitoring"
  type        = bool
  default     = true
}

# ============================================================
# Cloud SQL Variables / Biến Cloud SQL
# ============================================================

variable "cloudsql_database_version" {
  description = "PostgreSQL version cho Cloud SQL / PostgreSQL version for Cloud SQL"
  type        = string
  default     = "POSTGRES_16"
  validation {
    condition     = can(regex("^POSTGRES_\\d+$", var.cloudsql_database_version))
    error_message = "Database version phải là POSTGRES_XX / Database version must be POSTGRES_XX."
  }
}

variable "cloudsql_tier" {
  description = "Tier cho Cloud SQL instance / Tier for Cloud SQL instance"
  type        = string
  default     = "db-f1-micro"
}

variable "cloudsql_allocated_storage_gb" {
  description = "Dung lượng lưu trữ ban đầu (GB) / Initial allocated storage (GB)"
  type        = number
  default     = 50
  validation {
    condition     = var.cloudsql_allocated_storage_gb >= 10 && var.cloudsql_allocated_storage_gb <= 65536
    error_message = "Cloud SQL storage phải từ 10GB đến 65536GB / Cloud SQL storage must be between 10GB and 65536GB."
  }
}

variable "cloudsql_backup_configuration" {
  description = "Cấu hình backup cho Cloud SQL / Cloud SQL backup configuration"
  type = object({
    enabled                        = bool
    start_time                     = string
    point_in_time_recovery_enabled = bool
    transaction_log_retention_days = number
  })
  default = {
    enabled                        = true
    start_time                     = "03:00"
    point_in_time_recovery_enabled = true
    transaction_log_retention_days = 7
  }
}

variable "cloudsql_availability_type" {
  description = "Availability type: 'REGIONAL' cho HA / Availability type for Cloud SQL"
  type        = string
  default     = "ZONAL"
  validation {
    condition     = contains(["ZONAL", "REGIONAL"], var.cloudsql_availability_type)
    error_message = "Availability type phải là ZONAL hoặc REGIONAL / Availability type must be ZONAL or REGIONAL."
  }
}

variable "cloudsql_database_flags" {
  description = "Database flags cho Cloud SQL / Database flags for Cloud SQL"
  type = map(string)
  default = {
    max_connections = "100"
    shared_buffers  = "25600"
  }
}

variable "cloudsql_database_name" {
  description = "Tên database mặc định / Default database name"
  type        = string
  default     = "vierp"
}

variable "cloudsql_username" {
  description = "Username cho admin user / Admin username"
  type        = string
  default     = "vierpadmin"
  sensitive   = true
}

variable "cloudsql_password" {
  description = "Password cho admin user (để trống để auto-generate) / Admin password (leave empty for auto-generate)"
  type        = string
  default     = ""
  sensitive   = true
}

# ============================================================
# Memorystore Redis Variables / Biến Memorystore Redis
# ============================================================

variable "memorystore_redis_version" {
  description = "Redis version cho Memorystore / Redis version for Memorystore"
  type        = string
  default     = "7.2"
}

variable "memorystore_tier" {
  description = "Tier cho Memorystore: 'BASIC' hoặc 'STANDARD' / Tier for Memorystore"
  type        = string
  default     = "BASIC"
  validation {
    condition     = contains(["BASIC", "STANDARD"], var.memorystore_tier)
    error_message = "Memorystore tier phải là BASIC hoặc STANDARD / Memorystore tier must be BASIC or STANDARD."
  }
}

variable "memorystore_size_gb" {
  description = "Dung lượng Memorystore (GB) / Memorystore size (GB)"
  type        = number
  default     = 5
  validation {
    condition     = var.memorystore_size_gb >= 1 && var.memorystore_size_gb <= 300
    error_message = "Memorystore size phải từ 1GB đến 300GB / Memorystore size must be between 1GB and 300GB."
  }
}

variable "memorystore_enable_auth" {
  description = "Bật authentication cho Redis / Enable authentication for Redis"
  type        = bool
  default     = true
}

# ============================================================
# Cloud Storage Variables / Biến Cloud Storage
# ============================================================

variable "gcs_versioning_enabled" {
  description = "Bật versioning cho GCS buckets / Enable versioning for GCS buckets"
  type        = bool
  default     = true
}

variable "gcs_storage_class" {
  description = "Storage class cho GCS buckets / Storage class for GCS buckets"
  type        = string
  default     = "STANDARD"
  validation {
    condition     = contains(["STANDARD", "NEARLINE", "COLDLINE", "ARCHIVE"], var.gcs_storage_class)
    error_message = "GCS storage class phải là STANDARD, NEARLINE, COLDLINE hoặc ARCHIVE / GCS storage class must be one of these."
  }
}

variable "gcs_uploads_lifecycle_rules" {
  description = "Quy tắc lifecycle cho uploads bucket / Lifecycle rules for uploads bucket"
  type = list(object({
    action          = string # "Delete" hoặc "SetStorageClass" / "Delete" or "SetStorageClass"
    age_days        = number
    storage_class   = optional(string)
  }))
  default = [
    {
      action   = "Delete"
      age_days = 365
    }
  ]
}

variable "gcs_backups_lifecycle_rules" {
  description = "Quy tắc lifecycle cho backups bucket / Lifecycle rules for backups bucket"
  type = list(object({
    action          = string
    age_days        = number
    storage_class   = optional(string)
  }))
  default = [
    {
      action          = "SetStorageClass"
      age_days        = 30
      storage_class   = "NEARLINE"
    },
    {
      action   = "Delete"
      age_days = 365
    }
  ]
}

# ============================================================
# Tagging Variables / Biến Tagging
# ============================================================

variable "labels" {
  description = "Các labels bổ sung cho tất cả tài nguyên / Additional labels for all resources"
  type        = map(string)
  default     = {}
}
