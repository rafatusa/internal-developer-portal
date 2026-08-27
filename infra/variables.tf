variable "project_name" {
  description = "Branch-scoped project name used as the prefix for every resource."
  type        = string
}

variable "aws_region" {
  description = "AWS region the portal is deployed into."
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "CIDR block for the portal VPC."
  type        = string
  default     = "10.42.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet hosting the application instance."
  type        = string
  default     = "10.42.1.0/24"
}

variable "instance_type" {
  description = "EC2 instance type for the application host."
  type        = string
  default     = "t3.medium"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 30
}

variable "ssh_public_key" {
  description = "Public half of the platform-managed project SSH keypair."
  type        = string
}

variable "ssh_ingress_cidr" {
  description = "CIDR allowed to reach SSH. Narrow this to a corporate range in production."
  type        = string
  default     = "0.0.0.0/0"
}

variable "db_password" {
  description = "Password for the PostgreSQL application user."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "HMAC signing secret for portal JWTs."
  type        = string
  sensitive   = true
}
