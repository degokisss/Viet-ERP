# ============================================================
# GCP Provider Configuration / Cấu hình GCP Provider
# ============================================================
terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = ">= 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.11"
    }
  }

  # GCS backend for state management with state locking
  # Backend lưu trữ state file trong Google Cloud Storage
  # Tùy chỉnh thông tin backend theo project GCP của bạn
  # Customize backend information according to your GCP project
  backend "gcs" {
    # Tên bucket GCS để lưu trữ state
    # Name of GCS bucket for state storage
    bucket = "vierp-terraform-state-gcp"

    # Khóa để lưu trữ state file
    # Key to store the state file
    prefix = "vierp/terraform.tfstate"

    # Bật encryption cho state file
    # Enable encryption for state file
    encryption_key = "" # Set your encryption key if needed
  }
}

# Google Cloud Provider
# Nhà cung cấp Google Cloud
provider "google" {
  project = var.project_id
  region  = var.region

  default_labels {
    labels = local.common_labels
  }
}

# Google Cloud Provider Beta (for advanced features)
# Google Cloud Provider Beta (cho các tính năng nâng cao)
provider "google-beta" {
  project = var.project_id
  region  = var.region

  default_labels {
    labels = local.common_labels
  }
}

# Kubernetes Provider Configuration
# Cấu hình Kubernetes Provider
provider "kubernetes" {
  host                   = "https://${google_container_cluster.main.endpoint}"
  token                  = data.google_client_config.default.access_token
  cluster_ca_certificate = base64decode(google_container_cluster.main.master_auth.0.cluster_ca_certificate)
}

# Helm Provider Configuration
# Cấu hình Helm Provider
provider "helm" {
  kubernetes {
    host                   = "https://${google_container_cluster.main.endpoint}"
    token                  = data.google_client_config.default.access_token
    cluster_ca_certificate = base64decode(google_container_cluster.main.master_auth.0.cluster_ca_certificate)
  }
}

# Get current GCP configuration / Lấy thông tin GCP hiện tại
data "google_client_config" "default" {}

# Get available zones in the region / Lấy các zone có sẵn trong region
data "google_compute_zones" "available" {
  project = var.project_id
  region  = var.region
}

# ============================================================
# Local Variables / Biến Toàn Cục
# ============================================================
locals {
  # Project and environment names / Tên project và environment
  project_name = var.project_name
  environment  = var.environment

  # Common labels applied to all resources / Các nhãn chung được áp dụng cho tất cả tài nguyên
  common_labels = {
    project     = local.project_name
    environment = local.environment
    managed_by  = "Terraform"
    created_at  = timestamp()
  }

  # GKE cluster name
  gke_cluster_name = "${local.project_name}-${local.environment}-gke"

  # Resource names / Tên tài nguyên
  vpc_name           = "${local.project_name}-${local.environment}-vpc"
  cloudsql_name      = "${replace(local.project_name, "-", "")}-${local.environment}-db"
  memorystore_name   = "${local.project_name}-${local.environment}-redis"
  gcs_uploads_bucket = "${var.project_id}-${local.environment}-uploads"
  gcs_backups_bucket = "${var.project_id}-${local.environment}-backups"
}
