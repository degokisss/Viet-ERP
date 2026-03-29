# ============================================================
# AKS Cluster / Cụm AKS
# ============================================================

resource "azurerm_kubernetes_cluster" "main" {
  name                = local.aks_cluster_name
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  dns_prefix          = local.aks_cluster_name
  kubernetes_version  = var.kubernetes_version
  tags                = local.common_tags

  # Network configuration / Cấu hình mạng
  network_profile {
    network_plugin      = "azure"
    network_policy      = var.network_policy
    service_cidr        = "10.2.0.0/16"
    docker_bridge_cidr  = "172.17.0.1/16"
    dns_service_ip      = "10.2.0.10"
  }

  # Default node pool (system pool)
  default_node_pool {
    name                = "system"
    node_count          = try(var.aks_node_pools["system"].min_count, 1)
    min_count           = try(var.aks_node_pools["system"].min_count, 1)
    max_count           = try(var.aks_node_pools["system"].max_count, 3)
    vm_size             = try(var.aks_node_pools["system"].vm_size, "Standard_D2s_v3")
    os_disk_size_gb     = try(var.aks_node_pools["system"].os_disk_size_gb, 50)
    vnet_subnet_id      = azurerm_subnet.aks.id
    enable_auto_scaling = true
    max_pods            = 110

    node_labels = merge(
      {
        "workload" = "system"
      },
      try(var.aks_node_pools["system"].labels, {})
    )

    dynamic "node_taints" {
      for_each = try(var.aks_node_pools["system"].taints, [])
      content {
        key    = node_taints.value.key
        value  = node_taints.value.value
        effect = node_taints.value.effect
      }
    }
  }

  # Identity configuration / Cấu hình danh tính
  identity {
    type = var.enable_managed_identity ? "SystemAssigned" : "None"
  }

  # Enable Pod Security Policy / Bật Pod Security Policy
  pod_security_policy_enabled = var.enable_pod_security_policy

  # Azure Monitor configuration / Cấu hình Azure Monitor
  oms_agent {
    enabled = var.enable_azure_monitor
  }

  # Enable HTTP Application Routing / Bật HTTP Application Routing
  http_application_routing_enabled = var.enable_http_application_routing

  # RBAC configuration / Cấu hình RBAC
  role_based_access_control_enabled = true

  # API server authorized IP ranges / Các dải IP được ủy quyền của máy chủ API
  # api_server_authorized_ip_ranges = var.api_server_authorized_ip_ranges

  depends_on = [
    azurerm_subnet.aks,
    azurerm_network_security_group.aks
  ]
}

# ============================================================
# Additional Node Pools / Các Node Pool Bổ sung
# ============================================================

resource "azurerm_kubernetes_cluster_node_pool" "additional" {
  for_each = {
    for key, value in var.aks_node_pools :
    key => value
    if key != "system"
  }

  name                  = each.key
  kubernetes_cluster_id = azurerm_kubernetes_cluster.main.id
  node_count            = each.value.min_count
  min_count             = each.value.min_count
  max_count             = each.value.max_count
  vm_size               = each.value.vm_size
  os_disk_size_gb       = each.value.os_disk_size_gb
  vnet_subnet_id        = azurerm_subnet.aks.id
  enable_auto_scaling   = true
  max_pods              = 110

  node_labels = merge(
    {
      "workload" = each.key
    },
    each.value.labels
  )

  dynamic "node_taints" {
    for_each = each.value.taints
    content {
      key    = node_taints.value.key
      value  = node_taints.value.value
      effect = node_taints.value.effect
    }
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# ============================================================
# Cluster Role Binding for Default Service Account / Ràng buộc Vai trò Cluster
# ============================================================

resource "kubernetes_cluster_role_binding" "default" {
  metadata {
    name = "cluster-admin-binding"
  }

  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "cluster-admin"
  }

  subject {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Group"
    name      = "system:masters"
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# ============================================================
# Storage Classes / Các Lớp Lưu trữ
# ============================================================

# Fast SSD storage class / Lớp lưu trữ SSD nhanh
resource "kubernetes_storage_class" "fast" {
  metadata {
    name = "fast"
  }

  storage_provisioner = "kubernetes.io/azure-disk"
  reclaim_policy      = "Delete"

  parameters = {
    kind            = "Premium_LRS"
    cachingmode     = "ReadWrite"
    storageaccounts = azurerm_storage_account.main.name
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Standard storage class / Lớp lưu trữ tiêu chuẩn
resource "kubernetes_storage_class" "standard" {
  metadata {
    name = "standard"
  }

  storage_provisioner = "kubernetes.io/azure-disk"
  reclaim_policy      = "Delete"

  parameters = {
    kind            = "Standard_LRS"
    cachingmode     = "ReadWrite"
    storageaccounts = azurerm_storage_account.main.name
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}
