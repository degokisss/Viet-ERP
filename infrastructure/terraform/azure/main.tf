# ============================================================
# Azure Provider Configuration / Cấu hình Azure Provider
# ============================================================
terraform {
  required_version = ">= 1.5"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.0"
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

  # Azure Storage backend for state management
  # Backend lưu trữ state file trong Azure Storage
  # Tùy chỉnh thông tin backend theo tài khoản Azure của bạn
  # Customize backend information according to your Azure account
  backend "azurerm" {
    # Tên resource group
    # Resource group name
    resource_group_name = "vierp-terraform-state"

    # Tên storage account
    # Storage account name
    storage_account_name = "vierptfstate"

    # Tên container
    # Container name
    container_name = "vierp-state"

    # Khóa state file
    # State file key
    key = "vierp/terraform.tfstate"
  }
}

# Azure Provider Configuration / Cấu hình Azure Provider
provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
    virtual_machine {
      delete_os_disk_on_delete            = true
      graceful_shutdown                   = true
      skip_shutdown_and_force_delete       = false
    }
  }

  subscription_id = var.subscription_id

  default_tags {
    tags = local.common_tags
  }
}

# Kubernetes Provider Configuration / Cấu hình Kubernetes Provider
provider "kubernetes" {
  host                   = azurerm_kubernetes_cluster.main.kube_config.0.host
  username               = azurerm_kubernetes_cluster.main.kube_config.0.username
  password               = azurerm_kubernetes_cluster.main.kube_config.0.password
  client_certificate     = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_certificate)
  client_key             = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_key)
  cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.cluster_ca_certificate)
}

# Helm Provider Configuration / Cấu hình Helm Provider
provider "helm" {
  kubernetes {
    host                   = azurerm_kubernetes_cluster.main.kube_config.0.host
    username               = azurerm_kubernetes_cluster.main.kube_config.0.username
    password               = azurerm_kubernetes_cluster.main.kube_config.0.password
    client_certificate     = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_certificate)
    client_key             = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.client_key)
    cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.main.kube_config.0.cluster_ca_certificate)
  }
}

# Get current Azure context / Lấy ngữ cảnh Azure hiện tại
data "azurerm_client_config" "current" {}

# ============================================================
# Local Variables / Biến Toàn Cục
# ============================================================
locals {
  # Project and environment names / Tên project và environment
  project_name = var.project_name
  environment  = var.environment

  # Common tags applied to all resources / Các thẻ chung được áp dụng cho tất cả tài nguyên
  common_tags = {
    project     = local.project_name
    environment = local.environment
    managed_by  = "Terraform"
    created_at  = timestamp()
  }

  # AKS cluster name
  aks_cluster_name = "${local.project_name}-${local.environment}-aks"

  # Resource names / Tên tài nguyên
  resource_group_name    = "${local.project_name}-${local.environment}-rg"
  vnet_name              = "${local.project_name}-${local.environment}-vnet"
  aks_subnet_name        = "${local.project_name}-${local.environment}-aks-subnet"
  postgresql_name        = "${replace(local.project_name, "-", "")}${local.environment}db"
  redis_name             = "${local.project_name}-${local.environment}-redis"
  storage_account_name   = "${replace(local.project_name, "-", "")}${local.environment}sa"
  storage_uploads_share  = "uploads"
  storage_backups_share  = "backups"
}
