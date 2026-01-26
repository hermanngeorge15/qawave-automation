# QAWave Staging Outputs

output "control_plane_ip" {
  value = hcloud_server.control_plane.ipv4_address
}

output "worker_1_ip" {
  value = hcloud_server.worker_1.ipv4_address
}

output "worker_2_ip" {
  value = hcloud_server.worker_2.ipv4_address
}

output "ssh_commands" {
  value = {
    control_plane = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.control_plane.ipv4_address}"
    worker_1      = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.worker_1.ipv4_address}"
    worker_2      = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.worker_2.ipv4_address}"
  }
}

output "check_setup" {
  value = {
    logs      = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.control_plane.ipv4_address} 'tail -50 /var/log/user-data.log'"
    nodes     = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.control_plane.ipv4_address} 'k0s kubectl get nodes'"
    pods      = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.control_plane.ipv4_address} 'k0s kubectl get pods -A'"
    argocd_pw = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.control_plane.ipv4_address} 'cat /root/argocd-password.txt'"
  }
}

output "access_urls" {
  value = {
    argocd = "http://${hcloud_server.worker_1.ipv4_address}:30080"
    note   = "Wait ~10 min after apply for setup to complete"
  }
}

output "kubeconfig" {
  value = "ssh -i ~/.ssh/qawave-staging root@${hcloud_server.control_plane.ipv4_address} 'k0s kubeconfig admin' > ~/.kube/qawave-staging.conf && export KUBECONFIG=~/.kube/qawave-staging.conf"
}
