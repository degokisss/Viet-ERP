# ============================================================
# GKE Cluster / Cụm GKE
# ============================================================

# Create GKE Cluster (Standard or Autopilot)
# Tạo cụm GKE (Standard hoặc Autopilot)
resource "google_container_cluster" "main" {
  name        = local.gke_cluster_name
  location    = var.region
  description = "GKE cluster for ${local.project_name}-${local.environment}"

  # Use 'REGIONAL' for regional cluster / Sử dụng 'REGIONAL' cho cụm toàn khu vực
  # Use specific zones for zonal cluster / Sử dụng các zone cụ thể cho cụm theo zone
  # We'll use regional for HA / Chúng tôi sẽ sử dụng toàn khu vực cho HA

  # Initial node pool (will be disabled if autopilot)
  # Pool node ban đầu (sẽ bị vô hiệu hóa nếu autopilot)
  remove_default_node_pool = var.gke_mode == "standard" ? true : false
  initial_node_count       = 1

  min_master_version = var.kubernetes_version

  # Network configuration / Cấu hình mạng
  network    = google_compute_network.main.name
  subnetwork = google_compute_subnetwork.primary.name

  # IP allocation policy with secondary ranges / Chính sách phân bổ IP với secondary ranges
  ip_allocation_policy {
    cluster_secondary_range_name  = "pods"
    services_secondary_range_name = "services"
  }

  # Workload Identity / Workload Identity
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  # Network policy / Chính sách mạng
  network_policy {
    enabled  = var.enable_network_policy
    provider = var.enable_network_policy ? "PROVIDER_UNSPECIFIED" : null
  }

  addons_config {
    # Cloud Logging / Cloud Logging
    logging_service = var.enable_stackdriver_logging ? "logging.googleapis.com/kubernetes" : "none"

    # Cloud Monitoring / Cloud Monitoring
    monitoring_service = var.enable_stackdriver_monitoring ? "monitoring.googleapis.com/kubernetes" : "none"

    # Network Policy / Chính sách mạng
    network_policy_config {
      disabled = !var.enable_network_policy
    }

    # HTTP Load Balancer / Bộ cân bằng tải HTTP
    http_load_balancing {
      disabled = false
    }

    # Horizontal Pod Autoscaler / Tự động co giãn Pod theo chiều ngang
    horizontal_pod_autoscaling {
      disabled = false
    }
  }

  # Cluster autoscaling configuration / Cấu hình tự động co giãn cụm
  cluster_autoscaling {
    enabled = true
    resource_limits {
      resource_type = "cpu"
      min_limit     = 1
      max_limit     = 100
    }
    resource_limits {
      resource_type = "memory"
      min_limit     = 1
      max_limit     = 1000
    }
  }

  # Maintenance window / Cửa sổ bảo trì
  maintenance_policy {
    daily_maintenance_window {
      start_time = "03:00"
    }
  }

  # Security configuration / Cấu hình bảo mật
  enable_shielded_nodes = true

  enable_intra_node_visibility = true

  # Logging and monitoring / Ghi nhật ký và giám sát
  logging_service    = var.enable_stackdriver_logging ? "logging.googleapis.com/kubernetes" : "none"
  monitoring_service = var.enable_stackdriver_monitoring ? "monitoring.googleapis.com/kubernetes" : "none"

  project = var.project_id

  depends_on = [
    google_compute_subnetwork.primary,
    google_service_networking_connection.private_vpc_connection
  ]

  lifecycle {
    ignore_changes = [
      addons_config[0].http_load_balancing,
      addons_config[0].horizontal_pod_autoscaling
    ]
  }
}

# ============================================================
# Node Pools / Nhóm Node
# ============================================================

# Create node pools for standard clusters / Tạo nhóm node cho cụm standard
resource "google_container_node_pool" "node_pools" {
  for_each = var.gke_mode == "standard" ? var.gke_node_pools : {}

  name           = each.key
  cluster        = google_container_cluster.main.name
  location       = var.region
  node_count     = each.value.min_size
  version        = var.kubernetes_version
  project        = var.project_id

  autoscaling {
    min_node_count = each.value.min_size
    max_node_count = each.value.max_size
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  node_config {
    machine_type = each.value.machine_type
    disk_size_gb = each.value.disk_size_gb
    disk_type    = "pd-standard"

    # Service account for nodes / Tài khoản dịch vụ cho node
    service_account = google_service_account.gke.email
    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]

    # Workload Identity / Workload Identity
    workload_metadata_config {
      mode = "GKE_METADATA"
    }

    # Labels / Nhãn
    labels = merge(
      local.common_labels,
      each.value.labels
    )

    # Taints / Vết
    dynamic "taint" {
      for_each = each.value.taints
      content {
        key    = taint.value.key
        value  = taint.value.value
        effect = taint.value.effect
      }
    }

    # Metadata / Siêu dữ liệu
    metadata = {
      disable-legacy-endpoints = "true"
    }

    # Shielded instance / Instance được bảo vệ
    shielded_instance_config {
      enable_secure_boot          = true
      enable_integrity_monitoring = true
    }

    # Security / Bảo mật
    enable_display_device = false
  }

  depends_on = [google_container_cluster.main]
}

# ============================================================
# Autopilot Node Pool Configuration
# ============================================================

# Note: Autopilot clusters manage node pools automatically
# Chú ý: Cụm Autopilot quản lý các nhóm node tự động
# The configuration above is for reference

# ============================================================
# Optional: Node Auto-Repair and Auto-Upgrade
# ============================================================

# This is handled in the node pool management configuration above
# Điều này được xử lý trong cấu hình quản lý nhóm node ở trên
