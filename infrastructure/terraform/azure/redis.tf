# ============================================================
# Azure Cache for Redis / Azure Cache for Redis
# ============================================================

resource "azurerm_redis_cache" "main" {
  name                = local.redis_name
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  capacity            = var.redis_capacity
  family              = "C"  # C for non-clustered, P for Premium
  sku_name            = var.redis_sku
  enable_non_ssl_port = var.redis_enable_non_ssl_port
  minimum_tls_version = var.redis_minimum_tls_version
  tags                = local.common_tags

  # For Premium tier, adjust family / Cho Premium tier, điều chỉnh family
  lifecycle {
    ignore_changes = [family]
  }

  redis_configuration {
    # Enable AOF persistence for Premium tier / Bật AOF persistence cho Premium tier
    aof_backup_enabled = var.redis_sku == "Premium" ? true : false

    # Maxmemory policy / Chính sách bộ nhớ tối đa
    maxmemory_policy = "allkeys-lru"

    # Enable clustering for Premium tier / Bật clustering cho Premium tier
    enable_authentication = true

    # Notify keyspace events / Thông báo sự kiện keyspace
    notify_keyspace_events = "Ex"
  }

  depends_on = [azurerm_subnet.redis]
}

# ============================================================
# Redis Firewall Rules / Quy tắc Tường lửa Redis
# ============================================================

resource "azurerm_redis_firewall_rule" "allow_aks" {
  name              = "allow-aks-subnet"
  redis_cache_name  = azurerm_redis_cache.main.name
  resource_group_name = azurerm_resource_group.main.name
  start_ip          = cidrhost(var.aks_subnet_cidr, 0)
  end_ip            = cidrhost(var.aks_subnet_cidr, pow(2, 32 - tonumber(split("/", var.aks_subnet_cidr)[1])) - 1)

  depends_on = [azurerm_redis_cache.main]
}

# ============================================================
# Azure Cache for Redis with Geo-Replication (Premium only) / Geo-Replication (chỉ Premium)
# ============================================================

# Note: This requires Premium tier
# Chú ý: Điều này yêu cầu Premium tier
resource "azurerm_redis_linked_server" "replica" {
  count = var.redis_sku == "Premium" && var.environment == "prod" ? 1 : 0

  linked_server_name      = "${local.redis_name}-replica"
  resource_group_name     = azurerm_resource_group.main.name
  cache_name              = azurerm_redis_cache.main.name
  linked_redis_cache_id   = azurerm_redis_cache.main.id
  server_role             = "Secondary"
  linked_redis_cache_location = var.location  # For geo-replication, use different location

  depends_on = [azurerm_redis_cache.main]
}

# ============================================================
# Output Redis Connection Details / Chi tiết Kết nối Redis
# ============================================================

# The primary and secondary keys are available via:
# - azurerm_redis_cache.main.primary_access_key
# - azurerm_redis_cache.main.secondary_access_key
# Connection string can be constructed from:
# - azurerm_redis_cache.main.hostname
# - azurerm_redis_cache.main.port
# - azurerm_redis_cache.main.ssl_port (if SSL enabled)
