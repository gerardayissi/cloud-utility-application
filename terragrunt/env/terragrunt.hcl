# This is the root backend file for the backend to DRY

generate "provider" {
    path = "provider.tf"
    if_exists = "overwrite_terragrunt"
    contents = <<EOF
provider "google" {
  project = var.project_id
  region  = var.region
}

terraform {
  required_version = ">=1.5.7"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.67.0"
    }
  }
}
EOF
}

remote_state {
  backend = "gcs"
  config = {
    project  = "essex-retail"
    location = "us"
    bucket   = "essex-retail-terragrunt"
    prefix   = "terragrunt/${path_relative_to_include()}"
  }
}