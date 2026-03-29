# ============================================================
# Azure Database for PostgreSQL Flexible Server / Azure Database for PostgreSQL Flexible Server
# ============================================================

resource "random_password" "postgresql_password" {
  length  = 32
  special = true
}

resource "azurerm_postgresql_flexible_server" "main" {
  name                   = local.postgresql_name
  location               = var.location
  resource_group_name    = azurerm_resource_group.main.name
  administrator_login    = var.postgresql_administrator_login
  administrator_password = var.postgresql_administrator_password != "" ? var.postgresql_administrator_password : random_password.postgresql_password.result
  database_charset       = "UTF8"
  database_collation     = "en_US.utf8"
  sku_name               = var.postgresql_sku_name
  storage_mb             = var.postgresql_storage_mb
  version                = var.postgresql_version
  zone                   = "1"
  tags                   = local.common_tags

  # Backup configuration / Cấu hình backup
  backup_retention_days             = var.postgresql_backup_retention_days
  geo_redundant_backup_enabled      = var.environment == "prod" ? true : false
  point_in_time_restore_time_in_days = 7

  # High Availability / Tính sẵn sàng Cao
  high_availability {
    mode                      = var.postgresql_high_availability_enabled ? "ZoneRedundant" : "Disabled"
    standby_availability_zone = var.postgresql_high_availability_enabled ? "2" : null
  }

  # Maintenance window / Cửa sổ bảo trì
  maintenance_window {
    day_of_week  = 3  # Wednesday / Thứ 4
    start_hour   = 3
    start_minute = 0
  }

  # Network configuration / Cấu hình mạng
  delegated_subnet_id = azurerm_subnet.database.id
  private_dns_zone_id = azurerm_private_dns_zone.postgresql.id

  depends_on = [
    azurerm_subnet.database,
    azurerm_private_dns_zone_virtual_network_link.postgresql
  ]
}

# ============================================================
# PostgreSQL Database / Cơ sở dữ liệu PostgreSQL
# ============================================================

resource "azurerm_postgresql_flexible_server_database" "main" {
  name            = var.postgresql_database_name
  server_id       = azurerm_postgresql_flexible_server.main.id
  charset         = "UTF8"
  collation       = "en_US.utf8"

  depends_on = [azurerm_postgresql_flexible_server.main]
}

# ============================================================
# PostgreSQL Configuration / Cấu hình PostgreSQL
# ============================================================

resource "azurerm_postgresql_flexible_server_configuration" "log_connections" {
  name            = "log_connections"
  server_id       = azurerm_postgresql_flexible_server.main.id
  value           = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_disconnections" {
  name            = "log_disconnections"
  server_id       = azurerm_postgresql_flexible_server.main.id
  value           = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_duration" {
  name            = "log_duration"
  server_id       = azurerm_postgresql_flexible_server.main.id
  value           = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_min_duration_statement" {
  name            = "log_min_duration_statement"
  server_id       = azurerm_postgresql_flexible_server.main.id
  value           = var.environment == "prod" ? "1000" : "0"
}

# ============================================================
# Private DNS Zone for PostgreSQL / Private DNS Zone cho PostgreSQL
# ============================================================

resource "azurerm_private_dns_zone" "postgresql" {
  name                = "postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.main.name

  depends_on = [azurerm_resource_group.main]
}

# Link Private DNS Zone to VNet / Liên kết Private DNS Zone tới VNet
resource "azurerm_private_dns_zone_virtual_network_link" "postgresql" {
  name                  = "${local.vnet_name}-postgresql-link"
  private_dns_zone_name = azurerm_private_dns_zone.postgresql.name
  virtual_network_id    = azurerm_virtual_network.main.id
  resource_group_name   = azurerm_resource_group.main.name
  registration_enabled  = true

  depends_on = [
    azurerm_private_dns_zone.postgresql,
    azurerm_virtual_network.main
  ]
}

# ============================================================
# PostgreSQL Firewall Rule for VNet / Quy tắc Tường lửa PostgreSQL
# ============================================================

resource "azurerm_postgresql_flexible_server_firewall_rule" "allow_azure_services" {
  name             = "AllowAzureServices"
  server_id        = azurerm_postgresql_flexible_server.main.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"

  depends_on = [azurerm_postgresql_flexible_server.main]
}

# ============================================================
# Application Database User / Người dùng Cơ sở dữ liệu Ứng dụng
# ============================================================

# Note: Application user creation should be done via direct SQL connection
# Chú ý: Tạo người dùng ứng dụng nên được thực hiện qua kết nối SQL trực tiếp
# Or use Azure KeyVault for secret management
# Hoặc sử dụng Azure KeyVault để quản lý bí mật
