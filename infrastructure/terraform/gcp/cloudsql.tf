# ============================================================
# Cloud SQL PostgreSQL Instance / Instance Cloud SQL PostgreSQL
# ============================================================

# Create Cloud SQL instance / Tạo instance Cloud SQL
resource "google_sql_database_instance" "main" {
  name                = local.cloudsql_name
  database_version    = var.cloudsql_database_version
  deletion_protection = var.environment == "prod" ? true : false
  region              = var.region

  settings {
    tier              = var.cloudsql_tier
    availability_type = var.cloudsql_availability_type
    disk_type         = "PD_SSD"
    disk_size         = var.cloudsql_allocated_storage_gb

    # Backup configuration / Cấu hình backup
    backup_configuration {
      enabled                        = var.cloudsql_backup_configuration.enabled
      start_time                     = var.cloudsql_backup_configuration.start_time
      point_in_time_recovery_enabled = var.cloudsql_backup_configuration.point_in_time_recovery_enabled
      transaction_log_retention_days = var.cloudsql_backup_configuration.transaction_log_retention_days
      backup_retention_settings {
        retained_backups = var.environment == "prod" ? 30 : 7
        retention_unit   = "COUNT"
      }
    }

    # Maintenance window / Cửa sổ bảo trì
    maintenance_window {
      day          = 3  # Wednesday / Thứ 4
      hour         = 3
      update_track = var.environment == "prod" ? "stable" : "canary"
    }

    # IP configuration / Cấu hình IP
    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = google_compute_network.main.id
      enable_private_path_for_cloudsql_cloud_sql    = true
      require_ssl                                   = true
      authorized_networks {
        name  = "office"
        value = "0.0.0.0/0"  # In production, restrict this / Trong production, hãy hạn chế điều này
      }
    }

    # Database flags / Cờ cơ sở dữ liệu
    database_flags {
      name  = "max_connections"
      value = var.cloudsql_database_flags["max_connections"]
    }

    database_flags {
      name  = "shared_buffers"
      value = var.cloudsql_database_flags["shared_buffers"]
    }

    database_flags {
      name  = "log_min_duration_statement"
      value = var.environment == "prod" ? "1000" : "0"
    }

    # Insights configuration / Cấu hình Insights
    insights_config {
      query_insights_enabled  = true
      query_string_length     = 1024
      record_application_tags = true
    }

    # User labels / Nhãn người dùng
    user_labels = local.common_labels
  }

  project = var.project_id

  depends_on = [google_service_networking_connection.private_vpc_connection]
}

# ============================================================
# Cloud SQL Replica (optional for HA) / Bản sao Cloud SQL (tuỳ chọn cho HA)
# ============================================================

resource "google_sql_database_instance" "replica" {
  count = var.environment == "prod" && var.cloudsql_availability_type == "REGIONAL" ? 0 : 0

  name                 = "${local.cloudsql_name}-replica"
  region               = var.region
  database_version     = var.cloudsql_database_version
  master_instance_name = google_sql_database_instance.main.name

  deletion_protection = false
  replica_configuration {
    kind             = "REPLICA"
    master_instances = [google_sql_database_instance.main.name]
  }

  settings {
    tier              = var.cloudsql_tier
    availability_type = "ZONAL"
    disk_type         = "PD_SSD"
    disk_size         = var.cloudsql_allocated_storage_gb

    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.main.id
      require_ssl     = true
    }

    user_labels = local.common_labels
  }

  project = var.project_id

  depends_on = [google_sql_database_instance.main]
}

# ============================================================
# Cloud SQL Database / Cơ sở dữ liệu Cloud SQL
# ============================================================

resource "google_sql_database" "main" {
  name     = var.cloudsql_database_name
  instance = google_sql_database_instance.main.name
  charset  = "UTF8"
  collation = "en_US.UTF8"

  project = var.project_id

  depends_on = [google_sql_database_instance.main]
}

# ============================================================
# Cloud SQL Admin User / Người dùng Admin Cloud SQL
# ============================================================

resource "random_password" "cloudsql_password" {
  length  = 32
  special = true
}

resource "google_sql_user" "admin" {
  name     = var.cloudsql_username
  instance = google_sql_database_instance.main.name
  password = var.cloudsql_password != "" ? var.cloudsql_password : random_password.cloudsql_password.result
  type     = "BUILT_IN"

  project = var.project_id

  depends_on = [google_sql_database_instance.main]
}

# ============================================================
# Application Database User / Người dùng Cơ sở dữ liệu Ứng dụng
# ============================================================

resource "google_sql_user" "app_user" {
  name     = "vierp_app"
  instance = google_sql_database_instance.main.name
  type     = "BUILT_IN"

  project = var.project_id

  depends_on = [google_sql_database_instance.main]
}

# ============================================================
# Cloud SQL Backup / Sao lưu Cloud SQL
# ============================================================

# The backup is configured in the database instance settings above
# Sao lưu được cấu hình trong cài đặt instance cơ sở dữ liệu ở trên
