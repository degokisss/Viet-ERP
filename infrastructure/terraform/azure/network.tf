# ============================================================
# Resource Group / Nhóm Tài nguyên
# ============================================================

resource "azurerm_resource_group" "main" {
  name       = local.resource_group_name
  location   = var.location
  tags       = local.common_tags
}

# ============================================================
# Virtual Network / Mạng Ảo
# ============================================================

resource "azurerm_virtual_network" "main" {
  name                = local.vnet_name
  address_space       = [var.vnet_cidr]
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.common_tags
}

# ============================================================
# Subnets / Các Subnet
# ============================================================

# AKS Subnet
resource "azurerm_subnet" "aks" {
  name                 = local.aks_subnet_name
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.aks_subnet_cidr]

  depends_on = [azurerm_virtual_network.main]
}

# Database Subnet
resource "azurerm_subnet" "database" {
  name                 = "${local.vnet_name}-database-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.database_subnet_cidr]

  service_endpoints = [
    "Microsoft.Storage",
    "Microsoft.Sql",
    "Microsoft.CognitiveServices"
  ]

  depends_on = [azurerm_virtual_network.main]
}

# Redis Subnet
resource "azurerm_subnet" "redis" {
  name                 = "${local.vnet_name}-redis-subnet"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = [var.redis_subnet_cidr]

  depends_on = [azurerm_virtual_network.main]
}

# ============================================================
# Network Security Groups / Các Nhóm Bảo mật Mạng
# ============================================================

# AKS NSG
resource "azurerm_network_security_group" "aks" {
  name                = "${local.aks_cluster_name}-nsg"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.common_tags
}

# Allow internal AKS communication
resource "azurerm_network_security_rule" "aks_internal" {
  name                        = "allow-aks-internal"
  priority                    = 100
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "*"
  source_port_range           = "*"
  destination_port_range      = "*"
  source_address_prefix       = var.aks_subnet_cidr
  destination_address_prefix  = var.aks_subnet_cidr
  resource_group_name         = azurerm_resource_group.main.name
  network_security_group_name = azurerm_network_security_group.aks.name
}

# Allow traffic to database subnet
resource "azurerm_network_security_rule" "aks_to_database" {
  name                        = "allow-aks-to-database"
  priority                    = 110
  direction                   = "Outbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "5432"
  source_address_prefix       = var.aks_subnet_cidr
  destination_address_prefix  = var.database_subnet_cidr
  resource_group_name         = azurerm_resource_group.main.name
  network_security_group_name = azurerm_network_security_group.aks.name
}

# Database NSG
resource "azurerm_network_security_group" "database" {
  name                = "${local.vnet_name}-database-nsg"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.common_tags
}

# Allow database access from AKS
resource "azurerm_network_security_rule" "database_from_aks" {
  name                        = "allow-from-aks"
  priority                    = 100
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "5432"
  source_address_prefix       = var.aks_subnet_cidr
  destination_address_prefix  = var.database_subnet_cidr
  resource_group_name         = azurerm_resource_group.main.name
  network_security_group_name = azurerm_network_security_group.database.name
}

# Redis NSG
resource "azurerm_network_security_group" "redis" {
  name                = "${local.vnet_name}-redis-nsg"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  tags                = local.common_tags
}

# Allow Redis access from AKS
resource "azurerm_network_security_rule" "redis_from_aks" {
  name                        = "allow-from-aks"
  priority                    = 100
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "6380"
  source_address_prefix       = var.aks_subnet_cidr
  destination_address_prefix  = var.redis_subnet_cidr
  resource_group_name         = azurerm_resource_group.main.name
  network_security_group_name = azurerm_network_security_group.redis.name
}

# ============================================================
# Associate NSGs with Subnets / Liên kết NSG với Subnet
# ============================================================

resource "azurerm_subnet_network_security_group_association" "aks" {
  subnet_id                 = azurerm_subnet.aks.id
  network_security_group_id = azurerm_network_security_group.aks.id
}

resource "azurerm_subnet_network_security_group_association" "database" {
  subnet_id                 = azurerm_subnet.database.id
  network_security_group_id = azurerm_network_security_group.database.id
}

resource "azurerm_subnet_network_security_group_association" "redis" {
  subnet_id                 = azurerm_subnet.redis.id
  network_security_group_id = azurerm_network_security_group.redis.id
}

# ============================================================
# Public IP for NAT Gateway / Public IP cho NAT Gateway
# ============================================================

resource "azurerm_public_ip" "nat" {
  name                = "${local.vnet_name}-nat-pip"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  allocation_method   = "Static"
  sku                 = "Standard"
  tags                = local.common_tags

  depends_on = [azurerm_resource_group.main]
}

# ============================================================
# NAT Gateway / Cổng NAT
# ============================================================

resource "azurerm_nat_gateway" "main" {
  name                = "${local.vnet_name}-nat"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  sku_name            = "Standard"
  tags                = local.common_tags

  depends_on = [azurerm_public_ip.nat]
}

# Associate Public IP with NAT Gateway
resource "azurerm_nat_gateway_public_ip_association" "main" {
  nat_gateway_id       = azurerm_nat_gateway.main.id
  public_ip_address_id = azurerm_public_ip.nat.id
}

# Associate NAT Gateway with AKS Subnet
resource "azurerm_subnet_nat_gateway_association" "aks" {
  subnet_id      = azurerm_subnet.aks.id
  nat_gateway_id = azurerm_nat_gateway.main.id

  depends_on = [
    azurerm_nat_gateway.main,
    azurerm_subnet.aks
  ]
}
