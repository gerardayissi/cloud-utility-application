terraform {
  required_version = ">= 0.14"

  required_providers {
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = ">= 1.6.2"
    }
    google = {
      source  = "hashicorp/google"
      version = ">= 3.50, < 4.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = ">= 3.50, < 4.0"
    }
  }
}