# ============================================================
# Security Group cho RDS
# Security Group for RDS
# ============================================================

resource "aws_security_group" "rds" {
  name_prefix = "${local.rds_identifier}-"
  vpc_id      = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-sg"
    }
  )
}

# Cho phép inbound từ EKS nodes
# Allow inbound from EKS nodes
resource "aws_vpc_security_group_ingress_rule" "rds_from_eks" {
  security_group_id = aws_security_group.rds.id

  description              = "Allow PostgreSQL from EKS nodes"
  from_port                = 5432
  to_port                  = 5432
  ip_protocol              = "tcp"
  referenced_security_group_id = aws_security_group.eks_nodes.id
}

# ============================================================
# RDS Subnet Group
# ============================================================

resource "aws_db_subnet_group" "main" {
  name       = "${local.rds_identifier}-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-subnet-group"
    }
  )
}

# ============================================================
# RDS Parameter Group
# ============================================================

resource "aws_db_parameter_group" "main" {
  family = "postgres16"
  name   = "${local.rds_identifier}-params"

  # Tuning parameters cho VietERP
  # Tuning parameters for VietERP

  # Log queries chậm (trên 5 giây)
  # Log slow queries (over 5 seconds)
  parameter {
    name  = "log_min_duration_statement"
    value = "5000"
  }

  # Kích hoạt log statement
  # Enable statement logging
  parameter {
    name  = "log_statement"
    value = "all"
  }

  # Tối ưu hóa cho đọc/ghi
  # Optimize for read/write
  parameter {
    name  = "shared_preload_libraries"
    value = "pgaudit,pg_stat_statements"
  }

  # Kích hoạt pg_stat_statements
  # Enable pg_stat_statements
  parameter {
    name  = "pg_stat_statements.track"
    value = "all"
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-params"
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# ============================================================
# RDS Instance
# ============================================================

# Tạo mật khẩu ngẫu nhiên nếu không được cung cấp
# Generate random password if not provided
resource "random_password" "rds" {
  length  = 32
  special = true
  # Loại bỏ các ký tự gây vấn đề trong password
  # Avoid problematic characters
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_db_instance" "main" {
  # Nhận dạng
  # Identifiers
  identifier     = local.rds_identifier
  db_name        = var.rds_database_name
  username       = var.rds_username
  password       = var.rds_password != "" ? var.rds_password : random_password.rds.result

  # Engine
  engine               = "postgres"
  engine_version       = var.rds_engine_version
  parameter_group_name = aws_db_parameter_group.main.name

  # Instance và lưu trữ
  # Instance and storage
  instance_class           = var.rds_instance_class
  allocated_storage        = var.rds_allocated_storage
  max_allocated_storage    = var.rds_max_allocated_storage
  storage_type             = "gp3"
  storage_encrypted        = true
  iops                     = 3000
  storage_throughput       = 125

  # Mạng
  # Networking
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Backup và maintenance
  # Backup and maintenance
  backup_retention_period = var.rds_backup_retention_days
  backup_window           = var.rds_backup_window
  maintenance_window      = var.rds_maintenance_window
  copy_tags_to_snapshot   = true

  # Multi-AZ
  multi_az = var.environment == "prod" ? true : var.rds_multi_az

  # Performance Insights
  performance_insights_enabled    = var.rds_performance_insights_enabled
  performance_insights_retention_period = 7

  # Bật deletion protection cho production
  # Enable deletion protection for production
  deletion_protection = var.environment == "prod" ? true : false

  # Kích hoạt Enhanced Monitoring
  # Enable Enhanced Monitoring
  enabled_cloudwatch_logs_exports = ["postgresql"]
  monitoring_interval             = 60
  monitoring_role_arn             = aws_iam_role.rds_monitoring.arn

  # Skip final snapshot nếu cần
  # Skip final snapshot if needed
  skip_final_snapshot = var.environment != "prod" ? true : false

  # Tên final snapshot nếu không skip
  # Final snapshot name if not skipped
  final_snapshot_identifier = var.environment == "prod" ? "${local.rds_identifier}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}" : null

  depends_on = [aws_security_group.rds]

  tags = merge(
    local.common_tags,
    {
      Name = local.rds_identifier
    }
  )

  lifecycle {
    ignore_changes = [password]
  }
}

# ============================================================
# IAM Role cho RDS Enhanced Monitoring
# IAM Role for RDS Enhanced Monitoring
# ============================================================

resource "aws_iam_role" "rds_monitoring" {
  name = "${local.rds_identifier}-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-monitoring-role"
    }
  )
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
  role       = aws_iam_role.rds_monitoring.name
}

# ============================================================
# CloudWatch Alarms cho RDS
# CloudWatch Alarms for RDS
# ============================================================

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  alarm_name          = "${local.rds_identifier}-high-cpu"
  alarm_description   = "Alert when RDS CPU is high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_actions       = []

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-high-cpu"
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "rds_storage" {
  alarm_name          = "${local.rds_identifier}-low-storage"
  alarm_description   = "Alert when RDS free storage is low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 10737418240 # 10 GB in bytes

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-low-storage"
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "rds_database_connections" {
  alarm_name          = "${local.rds_identifier}-high-connections"
  alarm_description   = "Alert when RDS database connections are high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = 300
  statistic           = "Average"
  threshold           = 80

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.rds_identifier}-high-connections"
    }
  )
}
