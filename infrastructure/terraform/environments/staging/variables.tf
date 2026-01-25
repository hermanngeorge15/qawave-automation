# QAWave Staging Environment Variables

variable "hcloud_token" {
  description = "Hetzner Cloud API Token"
  type        = string
  sensitive   = true
}

variable "location" {
  description = "Hetzner datacenter location"
  type        = string
  default     = "nbg1" # Nuremberg, Germany

  validation {
    condition     = contains(["nbg1", "fsn1", "hel1", "ash"], var.location)
    error_message = "Location must be one of: nbg1, fsn1, hel1, ash"
  }
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key file"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}
