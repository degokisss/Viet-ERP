# ============================================================
# VPC Outputs
# ============================================================

output "vpc_id" {
  description = "ID của VPC / VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr" {
  description = "CIDR block của VPC / VPC CIDR block"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "IDs của public subnets / Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "IDs của private subnets / Private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "nat_gateway_ips" {
  description = "Elastic IPs của NAT Gateways / NAT Gateway Elastic IPs"
  value       = aws_eip.nat[*].public_ip
}

output "availability_zones" {
  description = "Danh sách AZ được sử dụng / List of availability zones used"
  value       = data.aws_availability_zones.available.names
}

# ============================================================
# EKS Outputs
# ============================================================

output "eks_cluster_name" {
  description = "Tên EKS cluster / EKS cluster name"
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "Endpoint của EKS cluster / EKS cluster endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_version" {
  description = "Phiên bản Kubernetes của cluster / Kubernetes version of the cluster"
  value       = aws_eks_cluster.main.version
}

output "eks_cluster_security_group_id" {
  description = "Security group ID của EKS cluster / EKS cluster security group ID"
  value       = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

output "eks_node_security_group_id" {
  description = "Security group ID của EKS nodes / EKS node security group ID"
  value       = aws_security_group.eks_nodes.id
}

output "eks_oidc_provider_arn" {
  description = "ARN của OIDC provider cho EKS / ARN of OIDC provider for EKS"
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "eks_oidc_provider_url" {
  description = "URL của OIDC provider / OIDC provider URL"
  value       = replace(aws_iam_openid_connect_provider.eks.url, "https://", "")
}

output "kubeconfig_raw" {
  description = "Kubeconfig dạng JSON / Kubeconfig in JSON format"
  value = base64encode(jsonencode({
    apiVersion = "v1"
    kind       = "Config"
    clusters = [{
      cluster = {
        server                     = aws_eks_cluster.main.endpoint
        certificate-authority-data = aws_eks_cluster.main.certificate_authority[0].data
      }
      name = aws_eks_cluster.main.name
    }]
    contexts = [{
      context = {
        cluster = aws_eks_cluster.main.name
        user    = aws_eks_cluster.main.name
      }
      name = aws_eks_cluster.main.name
    }]
    current-context = aws_eks_cluster.main.name
    users = [{
      name = aws_eks_cluster.main.name
      user = {
        exec = {
          apiVersion = "client.authentication.k8s.io/v1beta1"
          command    = "aws"
          args       = ["eks", "get-token", "--cluster-name", aws_eks_cluster.main.name, "--region", var.aws_region]
        }
      }
    }]
  }))
  sensitive = true
}

output "kubeconfig_command" {
  description = "Lệnh để lấy kubeconfig / Command to get kubeconfig"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"
}

# ============================================================
# RDS Outputs
# ============================================================

output "rds_endpoint" {
  description = "Endpoint của RDS instance / RDS instance endpoint"
  value       = aws_db_instance.main.endpoint
}

output "rds_address" {
  description = "Địa chỉ RDS (hostname) / RDS address (hostname)"
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "Port của RDS / RDS port"
  value       = aws_db_instance.main.port
}

output "rds_database_name" {
  description = "Tên database / Database name"
  value       = aws_db_instance.main.db_name
}

output "rds_username" {
  description = "Username RDS / RDS username"
  value       = aws_db_instance.main.username
  sensitive   = true
}

output "rds_instance_id" {
  description = "ID của RDS instance / RDS instance ID"
  value       = aws_db_instance.main.identifier
}

output "rds_connection_string" {
  description = "PostgreSQL connection string / PostgreSQL connection string"
  value       = "postgresql://${aws_db_instance.main.username}:PASSWORD@${aws_db_instance.main.address}:${aws_db_instance.main.port}/${aws_db_instance.main.db_name}"
  sensitive   = true
}

output "rds_security_group_id" {
  description = "Security group ID của RDS / RDS security group ID"
  value       = aws_security_group.rds.id
}

# ============================================================
# Redis Outputs
# ============================================================

output "redis_endpoint" {
  description = "Endpoint của Redis cluster / Redis cluster endpoint"
  value       = aws_elasticache_replication_group.main.configuration_endpoint_address
}

output "redis_port" {
  description = "Port của Redis / Redis port"
  value       = aws_elasticache_replication_group.main.port
}

output "redis_address" {
  description = "Primary endpoint address của Redis / Redis primary endpoint address"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "redis_connection_string" {
  description = "Redis connection string / Redis connection string"
  value       = "redis://${aws_elasticache_replication_group.main.primary_endpoint_address}:${aws_elasticache_replication_group.main.port}"
}

output "redis_replication_group_id" {
  description = "ID của Redis replication group / Redis replication group ID"
  value       = aws_elasticache_replication_group.main.id
}

output "redis_security_group_id" {
  description = "Security group ID của Redis / Redis security group ID"
  value       = aws_security_group.redis.id
}

# ============================================================
# S3 Outputs
# ============================================================

output "s3_uploads_bucket" {
  description = "Tên bucket S3 cho uploads / S3 uploads bucket name"
  value       = aws_s3_bucket.uploads.id
}

output "s3_uploads_bucket_arn" {
  description = "ARN của uploads bucket / ARN of uploads bucket"
  value       = aws_s3_bucket.uploads.arn
}

output "s3_backups_bucket" {
  description = "Tên bucket S3 cho backups / S3 backups bucket name"
  value       = aws_s3_bucket.backups.id
}

output "s3_backups_bucket_arn" {
  description = "ARN của backups bucket / ARN of backups bucket"
  value       = aws_s3_bucket.backups.arn
}

# ============================================================
# IAM Outputs
# ============================================================

output "eks_service_role_arn" {
  description = "ARN của EKS service role / ARN of EKS service role"
  value       = aws_iam_role.eks_service_role.arn
}

output "eks_node_role_arn" {
  description = "ARN của EKS node role / ARN of EKS node role"
  value       = aws_iam_role.eks_node_role.arn
}

output "s3_access_role_arn" {
  description = "ARN của S3 access role / ARN of S3 access role"
  value       = aws_iam_role.s3_access_role.arn
}

# ============================================================
# Summary Output
# ============================================================

output "deployment_summary" {
  description = "Tóm tắt các thông tin quan trọng / Summary of important information"
  value = {
    project     = var.project_name
    environment = var.environment
    region      = var.aws_region

    eks = {
      cluster_name = aws_eks_cluster.main.name
      endpoint     = aws_eks_cluster.main.endpoint
      version      = aws_eks_cluster.main.version
    }

    rds = {
      endpoint = aws_db_instance.main.endpoint
      port     = aws_db_instance.main.port
      database = aws_db_instance.main.db_name
    }

    redis = {
      endpoint = aws_elasticache_replication_group.main.primary_endpoint_address
      port     = aws_elasticache_replication_group.main.port
    }

    s3 = {
      uploads = aws_s3_bucket.uploads.id
      backups = aws_s3_bucket.backups.id
    }
  }
}
