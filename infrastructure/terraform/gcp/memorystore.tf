# ============================================================
# Memorystore Redis Instance / Instance Memorystore Redis
# ============================================================

# Create Memorystore Redis instance / Tạo instance Memorystore Redis
resource "google_redis_instance" "main" {
  name            = local.memorystore_name
  memory_size_gb  = var.memorystore_size_gb
  tier            = var.memorystore_tier
  redis_version   = var.memorystore_redis_version
  region          = var.region
  location_id     = data.google_compute_zones.available.names[0]
  display_name    = "Memorystore Redis for ${local.project_name}-${local.environment}"

  # Authentication / Xác thực
  auth_enabled = var.memorystore_enable_auth

  # Transit encryption / Mã hóa đang truyền
  transit_encryption_mode = var.memorystore_enable_auth ? "SERVER_AUTHENTICATION" : "DISABLED"

  # Network configuration / Cấu hình mạng
  connect_mode = "PRIVATE_SERVICE_ACCESS"
  authorized_network = google_compute_network.main.id

  # Persistence / Tính bền vững
  persistence_config {
    persistence_mode = var.environment == "prod" ? "RDB" : "DISABLED"
    rdb_snapshot_period = var.environment == "prod" ? "ONE_HOUR" : null
  }

  # Eviction policy / Chính sách Eviction
  eviction_policy = "ALLKEYS_LRU"

  # Redis configuration / Cấu hình Redis
  redis_configs = {
    # Max memory policy / Chính sách bộ nhớ tối đa
    maxmemory-policy = "allkeys-lru"

    # Notify keyspace events for cache invalidation / Thông báo sự kiện keyspace cho vô hiệu hóa bộ nhớ cache
    notify-keyspace-events = "Ex"

    # Timeout for idle clients / Timeout cho client không hoạt động
    timeout = "300"
  }

  # Labels / Nhãn
  labels = local.common_labels

  project = var.project_id

  depends_on = [google_service_networking_connection.private_vpc_connection]

  lifecycle {
    ignore_changes = [
      redis_configs
    ]
  }
}

# ============================================================
# Memorystore Failover Replica (production only) / Bản sao Failover (chỉ production)
# ============================================================

resource "google_redis_instance" "replica" {
  count = var.environment == "prod" && var.memorystore_tier == "STANDARD" ? 1 : 0

  name            = "${local.memorystore_name}-replica"
  memory_size_gb  = var.memorystore_size_gb
  tier            = "STANDARD"
  redis_version   = var.memorystore_redis_version
  region          = var.region
  display_name    = "Memorystore Redis Replica for ${local.project_name}-${local.environment}"

  # Replica specific configuration / Cấu hình cụ thể cho bản sao
  replication_spec {
    master = google_redis_instance.main.name
  }

  # Network / Mạng
  connect_mode       = "PRIVATE_SERVICE_ACCESS"
  authorized_network = google_compute_network.main.id

  # Labels / Nhãn
  labels = local.common_labels

  project = var.project_id

  depends_on = [
    google_redis_instance.main,
    google_service_networking_connection.private_vpc_connection
  ]
}

# ============================================================
# Redis Auth String for Application / Chuỗi Auth Redis cho Ứng dụng
# ============================================================

# The auth string is available in the output
# Chuỗi auth có sẵn trong output
