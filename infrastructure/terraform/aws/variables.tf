# ============================================================
# Biến cơ bản / Basic Variables
# ============================================================

variable "project_name" {
  description = "Tên project / Project name"
  type        = string
  default     = "vierp"
}

variable "environment" {
  description = "Môi trường triển khai (dev/staging/prod) / Deployment environment"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment phải là dev, staging hoặc prod / Environment must be dev, staging, or prod."
  }
}

variable "aws_region" {
  description = "AWS region / AWS region"
  type        = string
  default     = "ap-southeast-1"
}

# ============================================================
# VPC / Network Variables
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

variable "public_subnet_cidrs" {
  description = "CIDR blocks cho public subnets / CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks cho private subnets / CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24", "10.0.13.0/24"]
}

variable "enable_nat_gateway_per_az" {
  description = "Tạo NAT Gateway cho mỗi AZ (true) hay dùng một NAT Gateway (false) / Create NAT Gateway per AZ"
  type        = bool
  default     = false
}

variable "enable_vpc_flow_logs" {
  description = "Bật VPC Flow Logs / Enable VPC Flow Logs"
  type        = bool
  default     = true
}

# ============================================================
# EKS / Kubernetes Variables
# ============================================================

variable "kubernetes_version" {
  description = "Phiên bản Kubernetes cho EKS cluster / Kubernetes version for EKS cluster"
  type        = string
  default     = "1.29"
}

variable "eks_node_groups" {
  description = "Cấu hình các node group cho EKS / EKS node group configuration"
  type = map(object({
    instance_types = list(string)
    min_size       = number
    max_size       = number
    desired_size   = number
    disk_size      = number
    labels         = optional(map(string), {})
    taints = optional(list(object({
      key    = string
      value  = string
      effect = string
    })), [])
  }))
  default = {
    general = {
      instance_types = ["t3.medium"]
      min_size       = 1
      max_size       = 5
      desired_size   = 2
      disk_size      = 50
      labels = {
        workload = "general"
      }
    }
    compute = {
      instance_types = ["t3.large"]
      min_size       = 0
      max_size       = 3
      desired_size   = 0
      disk_size      = 100
      labels = {
        workload = "compute"
      }
    }
  }
}

variable "cluster_log_retention_in_days" {
  description = "Số ngày giữ CloudWatch logs / Number of days to retain CloudWatch logs"
  type        = number
  default     = 7
}

variable "enable_cluster_autoscaler" {
  description = "Bật Cluster Autoscaler / Enable Cluster Autoscaler"
  type        = bool
  default     = true
}

# ============================================================
# RDS / PostgreSQL Variables
# ============================================================

variable "rds_instance_class" {
  description = "Instance class cho RDS / Instance class for RDS"
  type        = string
  default     = "db.t4g.medium"
}

variable "rds_engine_version" {
  description = "PostgreSQL engine version / PostgreSQL engine version"
  type        = string
  default     = "16.1"
}

variable "rds_allocated_storage" {
  description = "Dung lượng lưu trữ ban đầu (GB) / Initial allocated storage (GB)"
  type        = number
  default     = 50
  validation {
    condition     = var.rds_allocated_storage >= 20 && var.rds_allocated_storage <= 65536
    error_message = "RDS storage phải từ 20GB đến 65536GB / RDS storage must be between 20GB and 65536GB."
  }
}

variable "rds_max_allocated_storage" {
  description = "Dung lượng lưu trữ tối đa cho autoscaling (GB) / Maximum allocated storage for autoscaling (GB)"
  type        = number
  default     = 200
}

variable "rds_backup_retention_days" {
  description = "Số ngày giữ backup tự động / Number of days to retain automated backups"
  type        = number
  default     = 7
  validation {
    condition     = var.rds_backup_retention_days >= 1 && var.rds_backup_retention_days <= 35
    error_message = "Retention period phải từ 1 đến 35 ngày / Retention period must be between 1 and 35 days."
  }
}

variable "rds_backup_window" {
  description = "Cửa sổ backup (UTC) / Backup window in UTC"
  type        = string
  default     = "03:00-04:00"
}

variable "rds_maintenance_window" {
  description = "Cửa sổ bảo trì (UTC) / Maintenance window in UTC"
  type        = string
  default     = "mon:04:00-mon:05:00"
}

variable "rds_multi_az" {
  description = "Bật Multi-AZ cho RDS / Enable Multi-AZ for RDS"
  type        = bool
  default     = false
}

variable "rds_performance_insights_enabled" {
  description = "Bật Performance Insights / Enable Performance Insights"
  type        = bool
  default     = true
}

variable "rds_database_name" {
  description = "Tên database mặc định / Default database name"
  type        = string
  default     = "vierp"
}

variable "rds_username" {
  description = "Username cho admin user / Admin username"
  type        = string
  default     = "vierpadmin"
  sensitive   = true
}

variable "rds_password" {
  description = "Password cho admin user (để trống để auto-generate) / Admin password (leave empty for auto-generate)"
  type        = string
  default     = ""
  sensitive   = true
}

# ============================================================
# ElastiCache Redis Variables
# ============================================================

variable "redis_node_type" {
  description = "Node type cho Redis / Node type for Redis"
  type        = string
  default     = "cache.t4g.micro"
}

variable "redis_engine_version" {
  description = "Redis engine version / Redis engine version"
  type        = string
  default     = "7.0"
}

variable "redis_num_cache_clusters" {
  description = "Số lượng cache nodes cho dev/staging / Number of cache nodes for dev/staging"
  type        = number
  default     = 1
  validation {
    condition     = var.redis_num_cache_clusters >= 1 && var.redis_num_cache_clusters <= 6
    error_message = "Cache clusters phải từ 1 đến 6 / Cache clusters must be between 1 and 6."
  }
}

variable "redis_automatic_failover_enabled" {
  description = "Bật automatic failover cho production / Enable automatic failover for production"
  type        = bool
  default     = false
}

variable "redis_multi_az_enabled" {
  description = "Bật Multi-AZ cho Redis (chỉ khi failover được bật) / Enable Multi-AZ for Redis"
  type        = bool
  default     = false
}

variable "redis_parameter_group_family" {
  description = "Redis parameter group family / Redis parameter group family"
  type        = string
  default     = "redis7"
}

# ============================================================
# S3 Variables
# ============================================================

variable "s3_versioning_enabled" {
  description = "Bật versioning cho S3 buckets / Enable versioning for S3 buckets"
  type        = bool
  default     = true
}

variable "s3_server_side_encryption_enabled" {
  description = "Bật server-side encryption cho S3 / Enable server-side encryption for S3"
  type        = bool
  default     = true
}

variable "s3_uploads_lifecycle_rules" {
  description = "Quy tắc lifecycle cho bucket uploads / Lifecycle rules for uploads bucket"
  type = list(object({
    id      = string
    enabled = bool
    days    = number
    action  = string # "TRANSITION" hoặc "EXPIRATION" / "TRANSITION" or "EXPIRATION"
  }))
  default = [
    {
      id      = "archive-old-uploads"
      enabled = true
      days    = 90
      action  = "TRANSITION"
    },
    {
      id      = "delete-very-old-uploads"
      enabled = true
      days    = 365
      action  = "EXPIRATION"
    }
  ]
}

variable "s3_backups_lifecycle_rules" {
  description = "Quy tắc lifecycle cho bucket backups / Lifecycle rules for backups bucket"
  type = list(object({
    id      = string
    enabled = bool
    days    = number
    action  = string # "TRANSITION" hoặc "EXPIRATION" / "TRANSITION" or "EXPIRATION"
  }))
  default = [
    {
      id      = "archive-old-backups"
      enabled = true
      days    = 30
      action  = "TRANSITION"
    },
    {
      id      = "delete-very-old-backups"
      enabled = true
      days    = 365
      action  = "EXPIRATION"
    }
  ]
}

# ============================================================
# Tagging Variables
# ============================================================

variable "tags" {
  description = "Các tag bổ sung cho tất cả tài nguyên / Additional tags for all resources"
  type        = map(string)
  default     = {}
}
