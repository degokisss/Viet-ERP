# ============================================================
# Common Tagging Module Variables / Biến Mô-đun Tagging Chung
# ============================================================

variable "project" {
  description = "Project name / Tên dự án"
  type        = string
  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]*[a-z0-9]$|^[a-z0-9]$", var.project))
    error_message = "Project name phải là lowercase với dấu gạch nối / Project name must be lowercase with hyphens."
  }
}

variable "environment" {
  description = "Deployment environment / Môi trường triển khai"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment phải là dev, staging hoặc prod / Environment must be dev, staging, or prod."
  }
}

variable "extra_tags" {
  description = "Additional tags to apply / Các thẻ bổ sung để áp dụng"
  type        = map(string)
  default     = {}
}
