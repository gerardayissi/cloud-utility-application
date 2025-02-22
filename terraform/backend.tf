
terraform {
  backend "gcs"{
    bucket      = "essex-retail-terragrunt"
    prefix      = "retail-state"
  }
}