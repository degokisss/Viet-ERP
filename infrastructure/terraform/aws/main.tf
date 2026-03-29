terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
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

  # AWS S3 backend with state locking via DynamoDB
  # Tùy chỉnh thông tin backend theo tài khoản AWS của bạn
  # Customize backend information according to your AWS account
  backend "s3" {
    # Thay đổi giá trị này theo tên bucket của bạn
    # Replace with your bucket name
    bucket         = "vierp-terraform-state"

    # Khóa để lưu trữ state file
    # Key to store the state file
    key            = "vierp/terraform.tfstate"

    # Region AWS nơi lưu trữ state
    # AWS region for state storage
    region         = "ap-southeast-1"

    # Bảng DynamoDB cho khóa state
    # DynamoDB table for state locking
    dynamodb_table = "vierp-terraform-locks"

    # Mã hóa state file
    # Encrypt state file
    encrypt        = true
  }
}

# Nhà cung cấp AWS
# AWS Provider
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

# Cấu hình Kubernetes provider
# Kubernetes Provider Configuration
provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority.0.data)
  token                  = data.aws_eks_cluster_auth.cluster.token
}

# Cấu hình Helm provider
# Helm Provider Configuration
provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.cluster.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority.0.data)
    token                  = data.aws_eks_cluster_auth.cluster.token
  }
}

# Tìm các AZ có sẵn trong region
# Data source for available AZs in the region
data "aws_availability_zones" "available" {
  state = "available"
}

# Lấy thông tin EKS cluster
# Get EKS cluster information
data "aws_eks_cluster" "cluster" {
  name = aws_eks_cluster.main.name
}

# Lấy token auth cho cluster
# Get authentication token for cluster
data "aws_eks_cluster_auth" "cluster" {
  name = aws_eks_cluster.main.name
}

# Các biến toàn cục
# Local variables
locals {
  # Tên project và environment
  # Project and environment names
  project_name = var.project_name
  environment  = var.environment

  # Các tag chung được áp dụng cho tất cả tài nguyên
  # Common tags applied to all resources
  common_tags = {
    Project     = local.project_name
    Environment = local.environment
    ManagedBy   = "Terraform"
    CreatedAt   = timestamp()
  }

  # EKS cluster name
  eks_cluster_name = "${local.project_name}-${local.environment}-eks"

  # Các tên tài nguyên
  # Resource names
  vpc_name          = "${local.project_name}-${local.environment}-vpc"
  rds_identifier    = "${replace(local.project_name, "-", "")}-${local.environment}-db"
  redis_name        = "${local.project_name}-${local.environment}-redis"
  s3_uploads_bucket = "${local.project_name}-${local.environment}-uploads"
  s3_backups_bucket = "${local.project_name}-${local.environment}-backups"
}
