# ============================================================
# Security Group cho Redis
# Security Group for Redis
# ============================================================

resource "aws_security_group" "redis" {
  name_prefix = "${local.redis_name}-"
  vpc_id      = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-sg"
    }
  )
}

# Cho phép inbound từ EKS nodes
# Allow inbound from EKS nodes
resource "aws_vpc_security_group_ingress_rule" "redis_from_eks" {
  security_group_id = aws_security_group.redis.id

  description              = "Allow Redis from EKS nodes"
  from_port                = 6379
  to_port                  = 6379
  ip_protocol              = "tcp"
  referenced_security_group_id = aws_security_group.eks_nodes.id
}

# ============================================================
# ElastiCache Subnet Group
# ============================================================

resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.redis_name}-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-subnet-group"
    }
  )
}

# ============================================================
# ElastiCache Parameter Group
# ============================================================

resource "aws_elasticache_parameter_group" "main" {
  family = var.redis_parameter_group_family
  name   = "${local.redis_name}-params"

  # Thiết lập maxmemory policy
  # Set maxmemory policy
  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  # Bật Append-Only File (AOF)
  # Enable Append-Only File (AOF)
  parameter {
    name  = "appendonly"
    value = "yes"
  }

  # Chế độ AOF fsync
  # AOF fsync mode
  parameter {
    name  = "appendfsync"
    value = "everysec"
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-params"
    }
  )
}

# ============================================================
# ElastiCache Replication Group
# ============================================================

resource "aws_elasticache_replication_group" "main" {
  # Nhận dạng
  # Identifiers
  replication_group_description = "Redis cluster for ${var.project_name}-${var.environment}"
  replication_group_id          = local.redis_name

  # Engine
  engine               = "redis"
  engine_version       = var.redis_engine_version
  node_type            = var.redis_node_type
  parameter_group_name = aws_elasticache_parameter_group.main.name
  port                 = 6379

  # Mạng
  # Networking
  subnet_group_name          = aws_elasticache_subnet_group.main.name
  security_group_ids         = [aws_security_group.redis.id]
  at_rest_encryption_enabled = true
  transit_encryption_enabled = false # Thay đổi thành true nếu cần TLS
  # transit_encryption_mode = "preferred"

  # Số lượng cache nodes
  # Number of cache nodes
  num_cache_clusters = var.environment == "prod" ? 3 : var.redis_num_cache_clusters

  # Automatic failover
  automatic_failover_enabled = var.environment == "prod" ? true : var.redis_automatic_failover_enabled

  # Multi-AZ
  multi_az_enabled = var.environment == "prod" ? true : var.redis_multi_az_enabled

  # Backup
  snapshot_retention_limit = var.environment == "prod" ? 7 : 1
  snapshot_window          = "03:00-05:00"
  maintenance_window       = "mon:04:00-mon:05:00"

  # Notifications
  notification_topic_arn = aws_sns_topic.redis_notifications.arn

  # Thẻ
  # Tags
  tags = merge(
    local.common_tags,
    {
      Name = local.redis_name
    }
  )

  depends_on = [aws_security_group.redis]
}

# ============================================================
# SNS Topic cho Redis notifications
# ============================================================

resource "aws_sns_topic" "redis_notifications" {
  name = "${local.redis_name}-notifications"

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-notifications"
    }
  )
}

resource "aws_sns_topic_policy" "redis_notifications" {
  arn = aws_sns_topic.redis_notifications.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "elasticache.amazonaws.com"
        }
        Action   = "SNS:Publish"
        Resource = aws_sns_topic.redis_notifications.arn
      }
    ]
  })
}

# ============================================================
# CloudWatch Alarms cho Redis
# CloudWatch Alarms for Redis
# ============================================================

resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  alarm_name          = "${local.redis_name}-high-cpu"
  alarm_description   = "Alert when Redis CPU is high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 75
  alarm_actions       = [aws_sns_topic.redis_notifications.arn]

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-high-cpu"
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "redis_memory" {
  alarm_name          = "${local.redis_name}-high-memory"
  alarm_description   = "Alert when Redis memory usage is high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "DatabaseMemoryUsagePercentage"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 85
  alarm_actions       = [aws_sns_topic.redis_notifications.arn]

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-high-memory"
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  alarm_name          = "${local.redis_name}-evictions"
  alarm_description   = "Alert when Redis is evicting keys"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Evictions"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_sns_topic.redis_notifications.arn]

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-evictions"
    }
  )
}

resource "aws_cloudwatch_metric_alarm" "redis_replication_lag" {
  count               = var.environment == "prod" ? 1 : 0
  alarm_name          = "${local.redis_name}-replication-lag"
  alarm_description   = "Alert when Redis replication lag is high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "ReplicationLag"
  namespace           = "AWS/ElastiCache"
  period              = 300
  statistic           = "Average"
  threshold           = 30
  alarm_actions       = [aws_sns_topic.redis_notifications.arn]

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.redis_name}-replication-lag"
    }
  )
}
