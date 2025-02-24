resource "google_pubsub_topic" "demo-topic" {
  name = "demo-topic-${var.env}"
  project = var.project_id
}

resource "google_pubsub_subscription" "demo-subscription" {
  name  = "demo-subscription-${var.env}"
  topic = "projects/${var.project_id}/topics/demo-topic"
  project = var.project_id

  retain_acked_messages      = true
  enable_message_ordering    = false

  depends_on = [google_pubsub_topic.demo-topic]
}

resource "google_pubsub_topic" "demo-topic-1" {
  name = "demo-topic-1-${var.env}"
  project = var.project_id
}