output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "instance_public_ip" {
  description = "Public IP address of the EC2 instance (via Elastic IP)"
  value       = aws_eip.app.public_ip
}

# Alias used by the pipeline stages
output "elastic_ip" {
  description = "Elastic IP public address (pipeline alias)"
  value       = aws_eip.app.public_ip
}

output "instance_public_dns" {
  description = "Public DNS name of the Elastic IP"
  value       = aws_eip.app.public_dns
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "subnet_id" {
  description = "Public subnet ID"
  value       = aws_subnet.public.id
}

output "security_group_id" {
  description = "Application security group ID"
  value       = aws_security_group.app.id
}

output "iam_role_arn" {
  description = "IAM role ARN attached to the EC2 instance"
  value       = aws_iam_role.app.arn
}

output "eip_allocation_id" {
  description = "Elastic IP allocation ID"
  value       = aws_eip.app.allocation_id
}

output "cloudwatch_log_group" {
  description = "CloudWatch log group name"
  value       = aws_cloudwatch_log_group.app.name
}

output "app_url" {
  description = "Application URL"
  value       = "http://${aws_eip.app.public_ip}"
}

output "health_check_url" {
  description = "Actuator health endpoint URL"
  value       = "http://${aws_eip.app.public_ip}/actuator/health"
}
