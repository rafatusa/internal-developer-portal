variable "project_name" {
  description = "Project name — used as a prefix for all AWS resources"
  type        = string
  default     = "internal-developer-portal"
}

variable "environment" {
  description = "Deployment environment (development | staging | production)"
  type        = string
  default     = "production"
}

variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "allowed_ssh_cidr" {
  description = "CIDR allowed to reach port 22 (restrict to your runner IP in production)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GB"
  type        = number
  default     = 30
}

variable "ssh_public_key" {
  description = "SSH public key material injected by the platform"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "PostgreSQL password for the application database user"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret (≥ 32 alphanumeric characters)"
  type        = string
  sensitive   = true
}

variable "app_image" {
  description = "Docker image to deploy (ghcr.io/<org>/<repo>:<tag>)"
  type        = string
  default     = "ghcr.io/enterprise/internal-developer-portal:latest"
}
