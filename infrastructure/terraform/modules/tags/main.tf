# ============================================================
# Common Tagging Module / Mô-đun Tagging Chung
# ============================================================
# This module provides consistent tagging across all cloud providers
# Mô-đun này cung cấp tagging nhất quán trên tất cả các nhà cung cấp đám mây

locals {
  # Base tags / Các thẻ cơ sở
  base_tags = {
    project     = var.project
    environment = var.environment
    managed_by  = "Terraform"
    created_at  = timestamp()
  }

  # Merge with additional tags / Hợp nhất với các thẻ bổ sung
  all_tags = merge(local.base_tags, var.extra_tags)
}

# Output tags / Đầu ra thẻ
output "tags" {
  description = "Merged tags for all resources / Các thẻ đã hợp nhất cho tất cả tài nguyên"
  value       = local.all_tags
}

output "tags_as_labels" {
  description = "Tags formatted as labels for Kubernetes / Các thẻ được định dạng dưới dạng nhãn cho Kubernetes"
  value = {
    for key, value in local.all_tags :
    replace(lower(key), "_", "-") => value
  }
}

output "tags_as_string" {
  description = "Tags formatted as comma-separated string / Các thẻ được định dạng dưới dạng chuỗi được phân tách bằng dấu phẩy"
  value = join(",", [
    for key, value in local.all_tags :
    "${key}=${value}"
  ])
}
