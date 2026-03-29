# ============================================================
# Service Accounts / Tài khoản Dịch vụ
# ============================================================

# ============================================================
# GKE Node Service Account / Tài khoản Dịch vụ Node GKE
# ============================================================

resource "google_service_account" "gke" {
  account_id   = "${replace(local.project_name, "-", "")}-${local.environment}-gke"
  display_name = "Service Account for GKE nodes"
  description  = "Service account for ${local.gke_cluster_name}"

  project = var.project_id
}

# GKE service account permissions
resource "google_project_iam_member" "gke_compute_viewer" {
  project = var.project_id
  role    = "roles/compute.viewer"
  member  = "serviceAccount:${google_service_account.gke.email}"
}

resource "google_project_iam_member" "gke_monitoring_metricwriter" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.gke.email}"
}

resource "google_project_iam_member" "gke_logging_logwriter" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.gke.email}"
}

resource "google_project_iam_member" "gke_storage_objectviewer" {
  project = var.project_id
  role    = "roles/storage.objectViewer"
  member  = "serviceAccount:${google_service_account.gke.email}"
}

# ============================================================
# Cloud SQL Proxy Service Account / Tài khoản Dịch vụ Cloud SQL Proxy
# ============================================================

resource "google_service_account" "cloudsql_proxy" {
  account_id   = "${replace(local.project_name, "-", "")}-${local.environment}-cloudsql"
  display_name = "Service Account for Cloud SQL Proxy"
  description  = "Service account for Cloud SQL proxy connections"

  project = var.project_id
}

# Cloud SQL client role
resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloudsql_proxy.email}"
}

# Cloud SQL instance user role
resource "google_project_iam_member" "cloudsql_instance_user" {
  project = var.project_id
  role    = "roles/cloudsql.instanceUser"
  member  = "serviceAccount:${google_service_account.cloudsql_proxy.email}"
}

# ============================================================
# Workload Identity Bindings / Ràng buộc Workload Identity
# ============================================================

# Allow GKE pods to assume the GKE service account identity
# Cho phép GKE pod giả định danh tính tài khoản dịch vụ GKE
resource "google_service_account_iam_member" "gke_workload_identity" {
  service_account_id = google_service_account.gke.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/gke]"

  depends_on = [google_service_account.gke]
}

# Allow Cloud SQL proxy service account Workload Identity binding
resource "google_service_account_iam_member" "cloudsql_workload_identity" {
  service_account_id = google_service_account.cloudsql_proxy.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[database/cloudsql-proxy]"

  depends_on = [google_service_account.cloudsql_proxy]
}

# ============================================================
# Kubernetes Service Account Annotations / Chú thích Tài khoản Dịch vụ Kubernetes
# ============================================================

# These annotations are used by Workload Identity to bind Kubernetes SA to Google SA
# Các chú thích này được Workload Identity sử dụng để ràng buộc Kubernetes SA với Google SA

resource "kubernetes_service_account" "gke" {
  metadata {
    name      = "gke"
    namespace = "default"
    annotations = {
      "iam.gke.io/gcp-service-account" = google_service_account.gke.email
    }
  }

  depends_on = [
    google_container_cluster.main,
    google_service_account.gke
  ]
}

resource "kubernetes_service_account" "cloudsql_proxy" {
  metadata {
    name      = "cloudsql-proxy"
    namespace = "database"
    annotations = {
      "iam.gke.io/gcp-service-account" = google_service_account.cloudsql_proxy.email
    }
  }

  depends_on = [
    google_container_cluster.main,
    google_service_account.cloudsql_proxy
  ]
}

# ============================================================
# GCP IAM Custom Role (optional) / Vai trò IAM Tùy chỉnh GCP (tuỳ chọn)
# ============================================================

resource "google_project_iam_custom_role" "vierp_operator" {
  role_id     = "vierp${local.environment}operator"
  title       = "VietERP ${upper(local.environment)} Operator"
  description = "Custom role for VietERP ${local.environment} environment operations"

  permissions = [
    "compute.instances.list",
    "compute.instances.get",
    "container.clusters.list",
    "container.clusters.get",
    "cloudsql.instances.list",
    "cloudsql.instances.get",
    "redis.instances.list",
    "redis.instances.get",
    "storage.buckets.list",
    "storage.objects.list",
  ]

  project = var.project_id
}

# ============================================================
# Service Account Keys (for authentication) / Khóa Tài khoản Dịch vụ
# ============================================================

# Note: In production, use Workload Identity instead of service account keys
# Chú ý: Trong production, sử dụng Workload Identity thay vì khóa tài khoản dịch vụ

resource "google_service_account_key" "gke" {
  service_account_id = google_service_account.gke.name
  public_key_type    = "TYPE_X509_PEM_FILE"

  depends_on = [google_service_account.gke]
}

resource "google_service_account_key" "cloudsql_proxy" {
  service_account_id = google_service_account.cloudsql_proxy.name
  public_key_type    = "TYPE_X509_PEM_FILE"

  depends_on = [google_service_account.cloudsql_proxy]
}

# ============================================================
# Create Kubernetes namespace for database / Tạo namespace Kubernetes cho cơ sở dữ liệu
# ============================================================

resource "kubernetes_namespace" "database" {
  metadata {
    name = "database"
    labels = {
      "app.kubernetes.io/name" = "database"
    }
  }

  depends_on = [google_container_cluster.main]
}
