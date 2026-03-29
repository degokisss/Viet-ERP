# ============================================================
# IAM Role cho S3 Access từ Pods
# IAM Role for S3 Access from Pods
# ============================================================

resource "aws_iam_role" "s3_access_role" {
  name = "${local.project_name}-${local.environment}-s3-access-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringLike = {
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:*:*"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.project_name}-${local.environment}-s3-access-role"
    }
  )
}

# Policy cho uploads bucket
# Policy for uploads bucket
resource "aws_iam_role_policy" "s3_uploads_access" {
  name = "${local.project_name}-${local.environment}-s3-uploads-policy"
  role = aws_iam_role.s3_access_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.uploads.arn,
          "${aws_s3_bucket.uploads.arn}/*"
        ]
      }
    ]
  })
}

# Policy cho backups bucket
# Policy for backups bucket
resource "aws_iam_role_policy" "s3_backups_access" {
  name = "${local.project_name}-${local.environment}-s3-backups-policy"
  role = aws_iam_role.s3_access_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:PutObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.backups.arn,
          "${aws_s3_bucket.backups.arn}/*"
        ]
      }
    ]
  })
}

# ============================================================
# IAM Role cho RDS Proxy (Optional)
# IAM Role for RDS Proxy (Optional)
# ============================================================

resource "aws_iam_role" "rds_proxy_role" {
  name = "${local.project_name}-${local.environment}-rds-proxy-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "rds.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.project_name}-${local.environment}-rds-proxy-role"
    }
  )
}

resource "aws_iam_role_policy" "rds_proxy_policy" {
  name = "${local.project_name}-${local.environment}-rds-proxy-policy"
  role = aws_iam_role.rds_proxy_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "rds:DescribeDBClusters",
          "rds:DescribeDBInstances"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "secretsmanager:ResourceTag/Environment" = var.environment
          }
        }
      }
    ]
  })
}

# ============================================================
# IAM Role cho Backup/Restore (Optional)
# IAM Role for Backup/Restore (Optional)
# ============================================================

resource "aws_iam_role" "backup_restore_role" {
  name = "${local.project_name}-${local.environment}-backup-restore-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringLike = {
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:vierp:backup-restore"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.project_name}-${local.environment}-backup-restore-role"
    }
  )
}

# Policy cho RDS backup/restore
# Policy for RDS backup/restore
resource "aws_iam_role_policy" "backup_restore_policy" {
  name = "${local.project_name}-${local.environment}-backup-restore-policy"
  role = aws_iam_role.backup_restore_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "rds:DescribeDBInstances",
          "rds:DescribeDBSnapshots",
          "rds:CreateDBSnapshot",
          "rds:CopyDBSnapshot",
          "rds:DeleteDBSnapshot",
          "rds:DescribeDBSnapshotAttributes",
          "rds:ModifyDBSnapshotAttribute",
          "rds:RestoreDBInstanceFromDBSnapshot"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.backups.arn,
          "${aws_s3_bucket.backups.arn}/*"
        ]
      }
    ]
  })
}

# ============================================================
# IAM Role cho Monitoring/Logging (Optional)
# IAM Role for Monitoring/Logging (Optional)
# ============================================================

resource "aws_iam_role" "monitoring_role" {
  name = "${local.project_name}-${local.environment}-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringLike = {
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:monitoring:*"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.project_name}-${local.environment}-monitoring-role"
    }
  )
}

# Policy cho CloudWatch metrics
# Policy for CloudWatch metrics
resource "aws_iam_role_policy" "monitoring_policy" {
  name = "${local.project_name}-${local.environment}-monitoring-policy"
  role = aws_iam_role.monitoring_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData",
          "ec2:DescribeVolumes",
          "ec2:DescribeTags",
          "ec2:DescribeInstances",
          "logs:PutLogEvents",
          "logs:CreateLogStream",
          "logs:CreateLogGroup"
        ]
        Resource = "*"
      }
    ]
  })
}

# ============================================================
# IAM Role cho AWS Load Balancer Controller (Optional)
# IAM Role for AWS Load Balancer Controller (Optional)
# ============================================================

resource "aws_iam_role" "alb_controller_role" {
  name = "${local.project_name}-${local.environment}-alb-controller-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringEquals = {
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.project_name}-${local.environment}-alb-controller-role"
    }
  )
}

# Attach AWS managed policy cho ALB controller
# Attach AWS managed policy for ALB controller
resource "aws_iam_role_policy_attachment" "alb_controller_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AWSLoadBalancerControllerIAMPolicy"
  role       = aws_iam_role.alb_controller_role.name
}

# ============================================================
# IAM Role cho Secrets Manager Access (Optional)
# IAM Role for Secrets Manager Access (Optional)
# ============================================================

resource "aws_iam_role" "secrets_access_role" {
  name = "${local.project_name}-${local.environment}-secrets-access-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.eks.arn
        }
        Condition = {
          StringLike = {
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:*:*"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.project_name}-${local.environment}-secrets-access-role"
    }
  )
}

resource "aws_iam_role_policy" "secrets_access_policy" {
  name = "${local.project_name}-${local.environment}-secrets-access-policy"
  role = aws_iam_role.secrets_access_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "secretsmanager:ResourceTag/Environment" = var.environment
          }
        }
      }
    ]
  })
}
