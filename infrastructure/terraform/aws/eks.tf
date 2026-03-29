# ============================================================
# Security Group cho EKS Cluster
# Security Group for EKS Cluster
# ============================================================

resource "aws_security_group" "eks_cluster" {
  name_prefix = "${local.eks_cluster_name}-"
  vpc_id      = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-cluster-sg"
    }
  )
}

# Cho phép inbound từ Node Security Group
# Allow inbound from Node Security Group
resource "aws_vpc_security_group_ingress_rule" "cluster_from_nodes" {
  security_group_id = aws_security_group.eks_cluster.id

  description              = "Allow inbound traffic from EKS nodes"
  from_port                = 443
  to_port                  = 443
  ip_protocol              = "tcp"
  referenced_security_group_id = aws_security_group.eks_nodes.id
}

# ============================================================
# Security Group cho EKS Nodes
# Security Group for EKS Nodes
# ============================================================

resource "aws_security_group" "eks_nodes" {
  name_prefix = "${local.eks_cluster_name}-nodes-"
  vpc_id      = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-nodes-sg"
    }
  )
}

# Cho phép nodes giao tiếp với nhau
# Allow nodes to communicate with each other
resource "aws_vpc_security_group_ingress_rule" "nodes_from_nodes" {
  security_group_id = aws_security_group.eks_nodes.id

  description              = "Allow node to node communication"
  from_port                = 0
  to_port                  = 65535
  ip_protocol              = "tcp"
  referenced_security_group_id = aws_security_group.eks_nodes.id
}

# Cho phép outbound từ nodes
# Allow outbound from nodes
resource "aws_vpc_security_group_egress_rule" "nodes" {
  security_group_id = aws_security_group.eks_nodes.id

  description      = "Allow all outbound traffic"
  from_port        = 0
  to_port          = 0
  ip_protocol      = "-1"
  cidr_ipv4        = "0.0.0.0/0"
}

# ============================================================
# IAM Role cho EKS Service
# IAM Role for EKS Service
# ============================================================

resource "aws_iam_role" "eks_service_role" {
  name = "${local.eks_cluster_name}-service-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-service-role"
    }
  )
}

# Attach EKS service policy
resource "aws_iam_role_policy_attachment" "eks_service_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSServiceRolePolicy"
  role       = aws_iam_role.eks_service_role.name
}

# Attach VPC CNI policy
resource "aws_iam_role_policy_attachment" "eks_vpc_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_service_role.name
}

# ============================================================
# IAM Role cho EKS Node Group
# IAM Role for EKS Node Group
# ============================================================

resource "aws_iam_role" "eks_node_role" {
  name = "${local.eks_cluster_name}-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-node-role"
    }
  )
}

# Attach node policies
resource "aws_iam_role_policy_attachment" "eks_node_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "eks_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "eks_registry_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "eks_ssm_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
  role       = aws_iam_role.eks_node_role.name
}

# ============================================================
# CloudWatch Log Group cho EKS
# CloudWatch Log Group for EKS
# ============================================================

resource "aws_cloudwatch_log_group" "eks" {
  name              = "/aws/eks/${local.eks_cluster_name}/cluster"
  retention_in_days = var.cluster_log_retention_in_days

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-logs"
    }
  )
}

# ============================================================
# EKS Cluster
# ============================================================

resource "aws_eks_cluster" "main" {
  name     = local.eks_cluster_name
  role_arn = aws_iam_role.eks_service_role.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids              = concat(aws_subnet.public[*].id, aws_subnet.private[*].id)
    security_groups         = [aws_security_group.eks_cluster.id]
    endpoint_private_access = true
    endpoint_public_access  = true
  }

  # Bật logging cho cluster
  # Enable cluster logging
  enabled_cluster_log_types = [
    "api",
    "audit",
    "authenticator",
    "controllerManager",
    "scheduler"
  ]

  depends_on = [
    aws_iam_role_policy_attachment.eks_service_policy,
    aws_cloudwatch_log_group.eks,
  ]

  tags = merge(
    local.common_tags,
    {
      Name = local.eks_cluster_name
    }
  )
}

# ============================================================
# EKS Node Groups
# ============================================================

resource "aws_eks_node_group" "main" {
  for_each = var.eks_node_groups

  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${local.eks_cluster_name}-${each.key}"
  node_role_arn   = aws_iam_role.eks_node_role.arn

  subnet_ids       = aws_subnet.private[*].id
  version          = var.kubernetes_version
  instance_types   = each.value.instance_types
  disk_size        = each.value.disk_size

  scaling_config {
    desired_size = each.value.desired_size
    max_size     = each.value.max_size
    min_size     = each.value.min_size
  }

  # Cập nhật từng node một lúc
  # Update one node at a time
  update_config {
    max_unavailable_percentage = 33
  }

  labels = merge(
    {
      NodeGroup = each.key
    },
    each.value.labels
  )

  # Thêm taints nếu có
  # Add taints if any
  dynamic "taint" {
    for_each = each.value.taints
    content {
      key    = taint.value.key
      value  = taint.value.value
      effect = taint.value.effect
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.eks_node_policy,
    aws_iam_role_policy_attachment.eks_cni_policy,
    aws_iam_role_policy_attachment.eks_registry_policy,
  ]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-${each.key}-ng"
    }
  )
}

# ============================================================
# OIDC Provider cho EKS (cho IRSA)
# OIDC Provider for EKS (for IRSA)
# ============================================================

data "tls_certificate" "eks_oidc" {
  url = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks_oidc.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.main.identity[0].oidc[0].issuer

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-oidc"
    }
  )
}

# ============================================================
# EKS Add-ons
# ============================================================

# CoreDNS Add-on
resource "aws_eks_addon" "coredns" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "coredns"
  addon_version            = data.aws_eks_addon_version.coredns.version
  resolve_conflicts        = "OVERWRITE"
  service_account_role_arn = aws_iam_role.eks_service_role.arn

  depends_on = [aws_eks_node_group.main]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-coredns"
    }
  )
}

data "aws_eks_addon_version" "coredns" {
  addon_name         = "coredns"
  kubernetes_version = var.kubernetes_version
  most_recent        = true
}

# kube-proxy Add-on
resource "aws_eks_addon" "kube_proxy" {
  cluster_name      = aws_eks_cluster.main.name
  addon_name        = "kube-proxy"
  addon_version     = data.aws_eks_addon_version.kube_proxy.version
  resolve_conflicts = "OVERWRITE"

  depends_on = [aws_eks_node_group.main]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-kube-proxy"
    }
  )
}

data "aws_eks_addon_version" "kube_proxy" {
  addon_name         = "kube-proxy"
  kubernetes_version = var.kubernetes_version
  most_recent        = true
}

# VPC CNI Add-on
resource "aws_eks_addon" "vpc_cni" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "vpc-cni"
  addon_version            = data.aws_eks_addon_version.vpc_cni.version
  resolve_conflicts        = "OVERWRITE"
  service_account_role_arn = aws_iam_role.vpc_cni_role.arn

  depends_on = [aws_eks_node_group.main]

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-vpc-cni"
    }
  )
}

data "aws_eks_addon_version" "vpc_cni" {
  addon_name         = "vpc-cni"
  kubernetes_version = var.kubernetes_version
  most_recent        = true
}

# ============================================================
# IAM Role cho VPC CNI Add-on
# IAM Role for VPC CNI Add-on
# ============================================================

resource "aws_iam_role" "vpc_cni_role" {
  name = "${local.eks_cluster_name}-vpc-cni-role"

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
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:kube-system:aws-node"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-vpc-cni-role"
    }
  )
}

resource "aws_iam_role_policy_attachment" "vpc_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.vpc_cni_role.name
}

# ============================================================
# Cluster Autoscaler IAM Role (nếu bật)
# Cluster Autoscaler IAM Role (if enabled)
# ============================================================

resource "aws_iam_role" "cluster_autoscaler" {
  count = var.enable_cluster_autoscaler ? 1 : 0
  name  = "${local.eks_cluster_name}-cluster-autoscaler-role"

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
            "${replace(aws_iam_openid_connect_provider.eks.url, "https://", "")}:sub" = "system:serviceaccount:kube-system:cluster-autoscaler"
          }
        }
      }
    ]
  })

  tags = merge(
    local.common_tags,
    {
      Name = "${local.eks_cluster_name}-cluster-autoscaler-role"
    }
  )
}

resource "aws_iam_role_policy" "cluster_autoscaler" {
  count = var.enable_cluster_autoscaler ? 1 : 0
  name  = "${local.eks_cluster_name}-cluster-autoscaler-policy"
  role  = aws_iam_role.cluster_autoscaler[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "autoscaling:DescribeAutoScalingGroups",
          "autoscaling:DescribeAutoScalingInstances",
          "autoscaling:DescribeLaunchConfigurations",
          "autoscaling:DescribeScalingActivities",
          "ec2:DescribeInstanceTypes",
          "ec2:DescribeLaunchTemplateVersions"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "autoscaling:SetDesiredCapacity",
          "autoscaling:TerminateInstanceInAutoScalingGroup"
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "autoscaling:ResourceTag/k8s.io/cluster-autoscaler/${local.eks_cluster_name}" = "owned"
          }
        }
      }
    ]
  })
}
