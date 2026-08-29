## Ubuntu 외부 접속 허용

### 1. Security List 포트 허용

- Ingress Rule
  - Source CIDR: 0.0.0.0/0
  - IP Protocol: TCP
  - Destination Port Range: 80

### 2. Ubuntu에서 포트 허용

```bash
# nfw 설치 및 포트 설정
sudo install nfw
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status verbose

# 재실행
sudo reboot
```

### 3. Nginx

```
sudo nginx -t
sudo systemctl reload nginx
```
