# This is the root backend file for the backend to DRY


remote_state {
  backend = "gcs"
  config = {
    project  = "essex-retail"
    location = "us"
    bucket   = "essex-retail-terragrunt"
    prefix   = "terragrunt/${path_relative_to_include()}"
  }
}