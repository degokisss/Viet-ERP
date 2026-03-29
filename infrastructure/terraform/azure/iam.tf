# ============================================================
# Identity and Access Management / Quản lý Danh tính và Truy cập
# ============================================================

# ============================================================
# AKS Cluster Managed Identity / Danh tính Được quản lý của Cluster AKS
# ============================================================

# The managed identity is created automatically by AKS
# Danh tính được quản lý được tạo tự động bởi AKS

# Get AKS principal ID for role assignments / Lấy principal ID của AKS
data "azurerm_kubernetes_cluster" "main" {
  name                = azurerm_kubernetes_cluster.main.name
  resource_group_name = azurerm_resource_group.main.name

  depends_on = [azurerm_kubernetes_cluster.main]
}

# ============================================================
# Role Assignments / Gán Vai trò
# ============================================================

# Allow AKS cluster to read from Container Registry / Cho phép cluster AKS đọc từ Container Registry
resource "azurerm_role_assignment" "aks_acr_pull" {
  scope            = azurerm_resource_group.main.id
  role_definition_name = "AcrPull"
  principal_id     = data.azurerm_kubernetes_cluster.main.identity[0].principal_id

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Allow AKS cluster to manage VNet / Cho phép cluster AKS quản lý VNet
resource "azurerm_role_assignment" "aks_network" {
  scope            = azurerm_virtual_network.main.id
  role_definition_name = "Network Contributor"
  principal_id     = data.azurerm_kubernetes_cluster.main.identity[0].principal_id

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Allow AKS to read storage account / Cho phép AKS đọc storage account
resource "azurerm_role_assignment" "aks_storage_read" {
  scope            = azurerm_storage_account.main.id
  role_definition_name = "Storage Account Contributor"
  principal_id     = data.azurerm_kubernetes_cluster.main.identity[0].principal_id

  depends_on = [azurerm_kubernetes_cluster.main]
}

# ============================================================
# Service Principal for External Applications / Service Principal cho Ứng dụng Bên ngoài
# ============================================================

# Create service principal for database access / Tạo service principal cho truy cập cơ sở dữ liệu
resource "azuread_service_principal" "database" {
  client_id = azuread_application.database.client_id

  depends_on = [azuread_application.database]
}

resource "azuread_application" "database" {
  display_name = "${local.project_name}-${local.environment}-db-access"

  required_resource_access {
    resource_app_id = "00000003-0000-0000-c000-000000000000"  # Microsoft Graph

    resource_access {
      id   = "e1fe6dd8-ba31-4d61-89e7-88639da4683d"
      type = "Scope"
    }
  }
}

# ============================================================
# Kubernetes Service Accounts / Tài khoản Dịch vụ Kubernetes
# ============================================================

# Default service account for general workloads / Tài khoản dịch vụ mặc định cho workloads chung
resource "kubernetes_service_account" "default" {
  metadata {
    name      = "default"
    namespace = "default"
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Service account for database operations / Tài khoản dịch vụ cho hoạt động cơ sở dữ liệu
resource "kubernetes_service_account" "database" {
  metadata {
    name      = "database"
    namespace = "default"
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Service account for storage operations / Tài khoản dịch vụ cho hoạt động lưu trữ
resource "kubernetes_service_account" "storage" {
  metadata {
    name      = "storage"
    namespace = "default"
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# ============================================================
# Kubernetes RBAC Roles / Vai trò RBAC Kubernetes
# ============================================================

# Role for database access / Vai trò cho truy cập cơ sở dữ liệu
resource "kubernetes_role" "database" {
  metadata {
    name      = "database-access"
    namespace = "default"
  }

  rule {
    api_groups = [""]
    resources  = ["secrets"]
    verbs      = ["get", "list"]
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Role binding for database service account
resource "kubernetes_role_binding" "database" {
  metadata {
    name      = "database-access-binding"
    namespace = "default"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Role"
    name      = kubernetes_role.database.metadata[0].name
  }

  subject {
    kind      = "ServiceAccount"
    name      = kubernetes_service_account.database.metadata[0].name
    namespace = kubernetes_service_account.database.metadata[0].namespace
  }

  depends_on = [kubernetes_role.database]
}

# ============================================================
# Kubernetes Network Policies / Chính sách Mạng Kubernetes
# ============================================================

# Deny all ingress traffic by default / Từ chối tất cả lưu lượng ingress theo mặc định
resource "kubernetes_network_policy" "deny_all_ingress" {
  metadata {
    name      = "deny-all-ingress"
    namespace = "default"
  }

  spec {
    pod_selector {}

    policy_types = ["Ingress"]
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Allow traffic within the same namespace / Cho phép lưu lượng trong cùng một namespace
resource "kubernetes_network_policy" "allow_same_namespace" {
  metadata {
    name      = "allow-same-namespace"
    namespace = "default"
  }

  spec {
    pod_selector {}

    policy_types = ["Ingress"]

    ingress {
      from {
        pod_selector {}
      }
    }
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# ============================================================
# Pod Security Policy / Chính sách Bảo mật Pod
# ============================================================

# Note: Pod Security Policies are deprecated in Kubernetes 1.25+
# Use Pod Security Standards instead
# Chú ý: Pod Security Policies bị eol trong Kubernetes 1.25+

resource "kubernetes_pod_security_policy" "restricted" {
  metadata {
    name = "restricted"
  }

  spec {
    privileged                 = false
    allow_privilege_escalation = false

    required_drop_capabilities = [
      "ALL"
    ]

    volumes = [
      "configMap",
      "emptyDir",
      "projected",
      "secret",
      "downwardAPI",
      "persistentVolumeClaim"
    ]

    host_network = false
    host_ipc     = false
    host_pid     = false

    run_as_user {
      rule = "MustRunAsNonRoot"
    }

    se_linux {
      rule = "MustRunAs"
      se_linux_options {
        level = "s0:c123,c456"
      }
    }

    fs_group {
      rule = "MustRunAs"
      range {
        min = 1
        max = 65535
      }
    }

    read_only_root_filesystem = false
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Pod Security Policy Cluster Role Binding / Ràng buộc Vai trò Cluster cho PSP
resource "kubernetes_cluster_role_binding" "psp_restricted" {
  metadata {
    name = "psp-restricted"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "system:restricted-psp"
  }

  subject {
    kind = "Group"
    name = "system:serviceaccounts"
  }

  depends_on = [kubernetes_pod_security_policy.restricted]
}
