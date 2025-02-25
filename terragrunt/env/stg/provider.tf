terraform {
  required_version = ">= 0.13"

  required_providers {
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