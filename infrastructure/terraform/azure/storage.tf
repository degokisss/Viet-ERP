# ============================================================
# Storage Account / Tài khoản Lưu trữ
# ============================================================

resource "azurerm_storage_account" "main" {
  name                     = local.storage_account_name
  resource_group_name      = azurerm_resource_group.main.name
  location                 = var.location
  account_tier             = var.storage_account_tier
  account_replication_type = var.storage_account_replication_type
  https_traffic_only_enabled = true
  min_tls_version          = "TLS1_2"
  tags                     = local.common_tags

  depends_on = [azurerm_resource_group.main]
}

# ============================================================
# Storage Account Network Rules / Quy tắc Mạng Tài khoản Lưu trữ
# ============================================================

resource "azurerm_storage_account_network_rules" "main" {
  storage_account_id = azurerm_storage_account.main.id

  default_action             = "Deny"
  bypass                     = ["AzureServices"]
  virtual_network_subnet_ids = [
    azurerm_subnet.aks.id,
    azurerm_subnet.database.id,
  ]
  ip_rules                   = []

  depends_on = [azurerm_storage_account.main]
}

# ============================================================
# Storage Container for Blobs (alternative to File Shares) / Container cho Blobs
# ============================================================

resource "azurerm_storage_container" "uploads" {
  name                  = local.storage_uploads_share
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"

  depends_on = [azurerm_storage_account.main]
}

resource "azurerm_storage_container" "backups" {
  name                  = local.storage_backups_share
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"

  depends_on = [azurerm_storage_account.main]
}

# ============================================================
# Storage File Shares (for persistent volumes) / File Shares
# ============================================================

resource "azurerm_storage_share" "uploads" {
  count                = 0  # Disabled, using containers instead / Vô hiệu hóa, sử dụng containers thay vào đó
  name                 = local.storage_uploads_share
  storage_account_name = azurerm_storage_account.main.name
  quota                = 100  # GB

  depends_on = [azurerm_storage_account.main]
}

resource "azurerm_storage_share" "backups" {
  count                = 0  # Disabled, using containers instead / Vô hiệu hóa, sử dụng containers thay vào đó
  name                 = local.storage_backups_share
  storage_account_name = azurerm_storage_account.main.name
  quota                = 500  # GB

  depends_on = [azurerm_storage_account.main]
}

# ============================================================
# Storage Account Management Policies / Chính sách Quản lý Tài khoản Lưu trữ
# ============================================================

resource "azurerm_storage_management_policy" "main" {
  storage_account_id = azurerm_storage_account.main.id

  rule {
    name    = "uploads-lifecycle"
    enabled = true

    filters {
      prefix_match = [local.storage_uploads_share]
      blob_types   = ["blockBlob", "appendBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = var.storage_uploads_retention_days
      }
    }
  }

  rule {
    name    = "backups-lifecycle"
    enabled = true

    filters {
      prefix_match = [local.storage_backups_share]
      blob_types   = ["blockBlob", "appendBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = var.storage_backups_retention_days
      }
    }
  }

  depends_on = [azurerm_storage_account.main]
}

# ============================================================
# Storage Account Encryption with Customer-Managed Keys (optional) / Mã hóa CMK (tuỳ chọn)
# ============================================================

# Note: For production, consider using customer-managed keys for encryption
# Chú ý: Đối với production, hãy xem xét sử dụng customer-managed keys cho mã hóa

# ============================================================
# Kubernetes Persistent Volume / Persistent Volume Kubernetes
# ============================================================

# Storage class for Azure Blob storage / Lớp lưu trữ cho Azure Blob storage
resource "kubernetes_storage_class" "blob" {
  metadata {
    name = "azure-blob"
  }

  storage_provisioner = "blob.csi.azure.com"
  reclaim_policy      = "Delete"

  parameters = {
    containerName = azurerm_storage_container.uploads.name
    storageAccount = azurerm_storage_account.main.name
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}

# Kubernetes secret for storage account access
resource "kubernetes_secret" "storage_account" {
  metadata {
    name      = "storage-account-secret"
    namespace = "default"
  }

  type = "Opaque"

  data = {
    storageAccount    = base64encode(azurerm_storage_account.main.name)
    storageAccountKey = base64encode(azurerm_storage_account.main.primary_access_key)
  }

  depends_on = [azurerm_kubernetes_cluster.main]
}
