resource "aws_iam_role" "eks_role" {
  name = "${var.eks_cluster_name}-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role" "eks_nodes" {
  name = "${var.eks_cluster_name}-eks-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_eks_cluster" "eks" {
  name     = var.eks_cluster_name
  role_arn = aws_iam_role.eks_role.arn

  vpc_config {
    subnet_ids = ["subnet-12345678", "subnet-87654321"]
  }
}

resource "aws_eks_node_group" "eks_nodes" {
  cluster_name   = aws_eks_cluster.eks.name
  node_role_arn  = aws_iam_role.eks_nodes.arn
  instance_types = ["t3.medium"]
  subnet_ids     = ["subnet-12345678", "subnet-87654321"]

  scaling_config {
    desired_size = 2
    min_size     = 1
    max_size     = 3
  }
}
