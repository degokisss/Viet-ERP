# ============================================================
# Cloud Storage Buckets / Các Bucket Cloud Storage
# ============================================================

# ============================================================
# Uploads Bucket / Bucket Tải lên
# ============================================================

resource "google_storage_bucket" "uploads" {
  name          = local.gcs_uploads_bucket
  location      = var.region
  force_destroy = var.environment != "prod"
  storage_class = var.gcs_storage_class

  # Uniform bucket-level access / Truy cập cấp bucket thống nhất
  uniform_bucket_level_access = true

  # Versioning / Quản lý phiên bản
  versioning {
    enabled = var.gcs_versioning_enabled
  }

  # Server-side encryption / Mã hóa phía máy chủ
  encryption {
    default_kms_key_name = try(google_kms_crypto_key.bucket_key.id, null)
  }

  # Lifecycle rules / Quy tắc vòng đời
  dynamic "lifecycle_rule" {
    for_each = var.gcs_uploads_lifecycle_rules
    content {
      action {
        type          = lifecycle_rule.value.action
        storage_class = lifecycle_rule.value.action == "SetStorageClass" ? lifecycle_rule.value.storage_class : null
      }
      condition {
        age = lifecycle_rule.value.age_days
      }
    }
  }

  # Logging / Ghi nhật ký
  logging {
    log_bucket = try(google_storage_bucket.logs.name, null)
  }

  # CORS configuration for web uploads / Cấu hình CORS cho tải lên web
  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "DELETE", "POST", "PUT"]
    response_header = ["Content-Type", "Access-Control-Allow-Origin"]
    max_age_seconds = 3600
  }

  # Labels / Nhãn
  labels = merge(
    local.common_labels,
    {
      bucket_type = "uploads"
    }
  )

  project = var.project_id
}

# ============================================================
# Backups Bucket / Bucket Sao lưu
# ============================================================

resource "google_storage_bucket" "backups" {
  name          = local.gcs_backups_bucket
  location      = var.region
  force_destroy = var.environment != "prod"
  storage_class = var.gcs_storage_class

  # Uniform bucket-level access / Truy cập cấp bucket thống nhất
  uniform_bucket_level_access = true

  # Versioning / Quản lý phiên bản
  versioning {
    enabled = var.gcs_versioning_enabled
  }

  # Server-side encryption / Mã hóa phía máy chủ
  encryption {
    default_kms_key_name = try(google_kms_crypto_key.bucket_key.id, null)
  }

  # Lifecycle rules / Quy tắc vòng đời
  dynamic "lifecycle_rule" {
    for_each = var.gcs_backups_lifecycle_rules
    content {
      action {
        type          = lifecycle_rule.value.action
        storage_class = lifecycle_rule.value.action == "SetStorageClass" ? lifecycle_rule.value.storage_class : null
      }
      condition {
        age = lifecycle_rule.value.age_days
      }
    }
  }

  # Logging / Ghi nhật ký
  logging {
    log_bucket = try(google_storage_bucket.logs.name, null)
  }

  # Labels / Nhãn
  labels = merge(
    local.common_labels,
    {
      bucket_type = "backups"
    }
  )

  project = var.project_id
}

# ============================================================
# Logs Bucket (for bucket logs) / Bucket Nhật ký
# ============================================================

resource "google_storage_bucket" "logs" {
  name          = "${var.project_id}-${local.environment}-logs"
  location      = var.region
  force_destroy = var.environment != "prod"
  storage_class = "STANDARD"

  uniform_bucket_level_access = true

  versioning {
    enabled = false
  }

  # Lifecycle rules for logs - delete old logs / Quy tắc vòng đời cho nhật ký
  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = 90
    }
  }

  labels = merge(
    local.common_labels,
    {
      bucket_type = "logs"
    }
  )

  project = var.project_id
}

# ============================================================
# Bucket Notifications / Thông báo Bucket
# ============================================================

# Create Pub/Sub topic for bucket notifications / Tạo chủ đề Pub/Sub cho thông báo bucket
resource "google_pubsub_topic" "bucket_events" {
  name = "${local.gcs_uploads_bucket}-events"

  project = var.project_id

  labels = local.common_labels
}

# Add notification to uploads bucket / Thêm thông báo vào bucket uploads
resource "google_storage_notification" "bucket_notification" {
  bucket         = google_storage_bucket.uploads.name
  payload_format = "JSON_V1"
  topic          = google_pubsub_topic.bucket_events.name
  event_types    = ["OBJECT_FINALIZE", "OBJECT_METADATA_UPDATE"]

  depends_on = [
    google_storage_bucket.uploads,
    google_pubsub_topic.bucket_events
  ]

  project = var.project_id
}

# ============================================================
# Cloud Storage IAM Bindings / Ràng buộc IAM Cloud Storage
# ============================================================

# Allow Cloud SQL to read/write to backups bucket / Cho phép Cloud SQL đọc/ghi vào bucket sao lưu
resource "google_storage_bucket_iam_member" "cloudsql_backups_access" {
  bucket = google_storage_bucket.backups.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.cloudsql_proxy.email}"

  depends_on = [
    google_storage_bucket.backups,
    google_service_account.cloudsql_proxy
  ]
}

# Allow GKE workloads to read/write to uploads bucket / Cho phép workload GKE đọc/ghi vào bucket uploads
resource "google_storage_bucket_iam_member" "gke_uploads_access" {
  bucket = google_storage_bucket.uploads.name
  role   = "roles/storage.objectCreator"
  member = "serviceAccount:${google_service_account.gke.email}"

  depends_on = [
    google_storage_bucket.uploads,
    google_service_account.gke
  ]
}

# ============================================================
# KMS Encryption Key for Buckets (optional) / Khóa Mã hóa KMS
# ============================================================

# Note: Create a KMS key for production environments
# Chú ý: Tạo khóa KMS cho môi trường production
resource "google_kms_key_ring" "bucket_keys" {
  count    = var.environment == "prod" ? 0 : 0  # Disabled for now / Vô hiệu hóa hiện tại
  name     = "${local.vpc_name}-bucket-keys"
  location = var.region

  project = var.project_id
}

resource "google_kms_crypto_key" "bucket_key" {
  count           = var.environment == "prod" ? 0 : 0  # Disabled for now / Vô hiệu hóa hiện tại
  name            = "${local.vpc_name}-bucket-key"
  key_ring        = google_kms_key_ring.bucket_keys[0].id
  rotation_period = "7776000s"  # 90 days

  project = var.project_id
}
