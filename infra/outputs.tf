output "instance_public_ip" {
  description = "Stable Elastic IP of the portal host — used by configure, verify and perf stages."
  value       = aws_eip.app.public_ip
}

output "instance_id" {
  description = "EC2 instance id of the portal host."
  value       = aws_instance.app.id
}

output "vpc_id" {
  description = "Id of the portal VPC."
  value       = aws_vpc.main.id
}

output "security_group_id" {
  description = "Id of the application security group."
  value       = aws_security_group.app.id
}

output "iam_role_arn" {
  description = "ARN of the EC2 instance role."
  value       = aws_iam_role.app.arn
}

output "portal_url" {
  description = "Base URL of the deployed portal."
  value       = "http://${aws_eip.app.public_ip}"
}
