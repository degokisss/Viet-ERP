# ============================================================
# VPC
# ============================================================

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(
    local.common_tags,
    {
      Name = local.vpc_name
    }
  )
}

# ============================================================
# Internet Gateway
# ============================================================

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-igw"
    }
  )
}

# ============================================================
# Public Subnets
# ============================================================

resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = merge(
    local.common_tags,
    {
      Name                            = "${local.vpc_name}-public-${count.index + 1}"
      "kubernetes.io/role/elb"        = "1"
      "kubernetes.io/role/alb-ingress" = "1"
    }
  )
}

# ============================================================
# Private Subnets
# ============================================================

resource "aws_subnet" "private" {
  count             = length(var.private_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = merge(
    local.common_tags,
    {
      Name                           = "${local.vpc_name}-private-${count.index + 1}"
      "kubernetes.io/role/internal-elb" = "1"
    }
  )
}

# ============================================================
# Elastic IPs for NAT Gateways
# ============================================================

resource "aws_eip" "nat" {
  count  = var.enable_nat_gateway_per_az ? length(data.aws_availability_zones.available.names) : 1
  domain = "vpc"

  depends_on = [aws_internet_gateway.main]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-eip-nat-${count.index + 1}"
    }
  )
}

# ============================================================
# NAT Gateways
# ============================================================

resource "aws_nat_gateway" "main" {
  count         = var.enable_nat_gateway_per_az ? length(data.aws_availability_zones.available.names) : 1
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  depends_on = [aws_internet_gateway.main]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-nat-${count.index + 1}"
    }
  )
}

# ============================================================
# Public Route Table
# ============================================================

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block      = "0.0.0.0/0"
    gateway_id      = aws_internet_gateway.main.id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-public-rt"
    }
  )
}

# Liên kết public subnets với public route table
# Associate public subnets with public route table
resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ============================================================
# Private Route Tables
# ============================================================

resource "aws_route_table" "private" {
  count  = length(aws_subnet.private)
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = var.enable_nat_gateway_per_az ? aws_nat_gateway.main[count.index].id : aws_nat_gateway.main[0].id
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-private-rt-${count.index + 1}"
    }
  )
}

# Liên kết private subnets với private route tables
# Associate private subnets with private route tables
resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

# ============================================================
# VPC Flow Logs
# ============================================================

resource "aws_flow_log_s3_bucket" "main" {
  count = var.enable_vpc_flow_logs ? 1 : 0

  bucket = "${local.vpc_name}-flow-logs-${data.aws_caller_identity.current.account_id}"

  versioning {
    enabled = false
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-flow-logs"
    }
  )
}

# CloudWatch Logs group cho VPC Flow Logs
# CloudWatch Logs group for VPC Flow Logs
resource "aws_cloudwatch_log_group" "vpc_flow_logs" {
  count             = var.enable_vpc_flow_logs ? 1 : 0
  name              = "/aws/vpc/flowlogs/${local.vpc_name}"
  retention_in_days = 7

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-flow-logs"
    }
  )
}

# IAM role cho VPC Flow Logs
# IAM role for VPC Flow Logs
resource "aws_iam_role" "vpc_flow_logs" {
  count = var.enable_vpc_flow_logs ? 1 : 0

  name = "${local.vpc_name}-flow-logs-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "vpc-flow-logs.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-flow-logs-role"
    }
  )
}

# IAM policy cho VPC Flow Logs
# IAM policy for VPC Flow Logs
resource "aws_iam_role_policy" "vpc_flow_logs" {
  count = var.enable_vpc_flow_logs ? 1 : 0

  name = "${local.vpc_name}-flow-logs-policy"
  role = aws_iam_role.vpc_flow_logs[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogGroups",
          "logs:DescribeLogStreams"
        ]
        Effect   = "Allow"
        Resource = "*"
      }
    ]
  })
}

# VPC Flow Logs resource
resource "aws_flow_log" "main" {
  count                   = var.enable_vpc_flow_logs ? 1 : 0
  iam_role_arn            = aws_iam_role.vpc_flow_logs[0].arn
  log_destination         = aws_cloudwatch_log_group.vpc_flow_logs[0].arn
  traffic_type            = "ALL"
  vpc_id                  = aws_vpc.main.id
  log_destination_type    = "cloud-watch-logs"
  log_format              = "${aws_vpc_flow_log_format.standard[0].log_format}"

  tags = merge(
    local.common_tags,
    {
      Name = "${local.vpc_name}-flow-logs"
    }
  )
}

# Custom log format cho VPC Flow Logs
# Custom log format for VPC Flow Logs
resource "aws_vpc_flow_log_format" "standard" {
  count = var.enable_vpc_flow_logs ? 1 : 0

  log_format = "$${version} $${account-id} $${interface-id} $${srcaddr} $${dstaddr} $${srcport} $${dstport} $${protocol} $${packets} $${bytes} $${windowstart} $${windowend} $${action} $${tcp-flags} $${type} $${pkt-srcaddr} $${pkt-dstaddr} $${region} $${vpc-id} $${subnet-id} $${instance-id} $${interface-type} $${eni-id} $${local-gateway-route-table-id} $${vpc-peering-connection-id} $${flow-logs-id} $${traffic-type} $${packet-aggregation-flags}"
}

# Data source để lấy caller identity
# Data source to get caller identity
data "aws_caller_identity" "current" {}
