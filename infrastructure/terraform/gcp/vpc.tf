# ============================================================
# VPC Network / Mạng VPC
# ============================================================

# Create VPC Network / Tạo mạng VPC
resource "google_compute_network" "main" {
  name                    = local.vpc_name
  auto_create_subnetworks = false
  routing_mode            = "REGIONAL"
  description             = "VPC for ${local.project_name}-${local.environment}"

  project = var.project_id
}

# ============================================================
# Primary Subnet / Subnet Chính
# ============================================================

# Create primary subnet with secondary ranges for GKE
# Tạo subnet chính với secondary ranges cho GKE
resource "google_compute_subnetwork" "primary" {
  name          = "${local.vpc_name}-primary"
  ip_cidr_range = var.primary_subnet_cidr
  region        = var.region
  network       = google_compute_network.main.id

  private_ip_google_access = true
  description              = "Primary subnet for GKE"

  # Secondary ranges for pods and services / Secondary ranges cho Pods và Services
  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = var.pods_secondary_range_cidr
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = var.services_secondary_range_cidr
  }

  log_config {
    aggregation_interval = "INTERVAL_5_SEC"
    flow_sampling        = 0.5
    metadata             = "INCLUDE_ALL_METADATA"
  }

  project = var.project_id

  depends_on = [google_compute_network.main]
}

# ============================================================
# Secondary Subnet (optional) / Subnet Phụ (tuỳ chọn)
# ============================================================

resource "google_compute_subnetwork" "secondary" {
  count = var.secondary_subnet_cidr != null ? 1 : 0

  name          = "${local.vpc_name}-secondary"
  ip_cidr_range = var.secondary_subnet_cidr
  region        = var.region
  network       = google_compute_network.main.id

  private_ip_google_access = true
  description              = "Secondary subnet for additional workloads"

  project = var.project_id

  depends_on = [google_compute_network.main]
}

# ============================================================
# Cloud Router for Cloud NAT / Cloud Router cho Cloud NAT
# ============================================================

resource "google_compute_router" "main" {
  count = var.enable_cloud_nat ? 1 : 0

  name        = "${local.vpc_name}-router"
  region      = var.region
  network     = google_compute_network.main.id
  description = "Cloud Router for NAT gateway"

  project = var.project_id

  bgp {
    asn = 64514
  }

  depends_on = [google_compute_network.main]
}

# ============================================================
# Cloud NAT / Cloud NAT
# ============================================================

resource "google_compute_router_nat" "main" {
  count = var.enable_cloud_nat ? 1 : 0

  name                               = "${local.vpc_name}-nat"
  router                             = google_compute_router.main[0].name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  # Log NAT gateway activity / Ghi log hoạt động NAT gateway
  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }

  project = var.project_id

  depends_on = [google_compute_router.main]
}

# ============================================================
# Firewall Rules / Quy Tắc Tường Lửa
# ============================================================

# Allow internal communication / Cho phép giao tiếp nội bộ
resource "google_compute_firewall" "allow_internal" {
  name        = "${local.vpc_name}-allow-internal"
  network     = google_compute_network.main.id
  direction   = "INGRESS"
  priority    = 1000
  description = "Allow internal communication between nodes"

  source_ranges = [
    var.vpc_cidr,
    var.pods_secondary_range_cidr,
    var.services_secondary_range_cidr
  ]

  allow {
    protocol = "tcp"
  }

  allow {
    protocol = "udp"
  }

  allow {
    protocol = "icmp"
  }

  project = var.project_id
}

# Allow inbound from Cloud NAT / Cho phép đến từ Cloud NAT
resource "google_compute_firewall" "allow_cloud_nat" {
  count = var.enable_cloud_nat ? 1 : 0

  name        = "${local.vpc_name}-allow-cloud-nat"
  network     = google_compute_network.main.id
  direction   = "EGRESS"
  priority    = 1000
  description = "Allow outbound to Cloud NAT"

  destination_ranges = ["0.0.0.0/0"]

  allow {
    protocol = "tcp"
  }

  allow {
    protocol = "udp"
  }

  project = var.project_id
}

# Allow GKE health checks / Cho phép GKE health checks
resource "google_compute_firewall" "allow_health_checks" {
  name        = "${local.vpc_name}-allow-health-checks"
  network     = google_compute_network.main.id
  direction   = "INGRESS"
  priority    = 1000
  description = "Allow health checks from GKE"

  source_ranges = [
    "35.191.0.0/16",  # Google Cloud Health Checks
    "130.211.0.0/22"  # Google Cloud Health Checks
  ]

  allow {
    protocol = "tcp"
  }

  project = var.project_id
}

# ============================================================
# Private Service Access / Truy Cập Dịch Vụ Riêng Tư
# ============================================================

# Reserve IP range for private service access / Dự trữ dải IP cho truy cập dịch vụ riêng
resource "google_compute_global_address" "private_service_access" {
  name            = "${local.vpc_name}-private-service-access"
  purpose         = "VPC_PEERING"
  address_type    = "INTERNAL"
  prefix_length   = 16
  network         = google_compute_network.main.id
  description     = "Private service access for Google Cloud services"

  project = var.project_id
}

# Create private VPC connection / Tạo kết nối VPC riêng tư
resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = google_compute_network.main.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_service_access.name]

  depends_on = [
    google_compute_global_address.private_service_access,
    google_compute_network.main
  ]
}
