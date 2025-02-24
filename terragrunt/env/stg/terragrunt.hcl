include {
  path = find_in_parent_folders()
}

terraform {
  source = "../../code/"
}

inputs = {
  env               = local.env
  project_id        = local.project_id
  region            = local.region
}

#################################################################
# TODO UPDATE THESE PARAMETERS
#################################################################

locals {
  env               = "stg"
  project_id        = "essex-retail"
  region            = "us-east4"
}