# Let's Encrypt

Let's Encrypt는 사용자에게 무료로 TLS 인증서를 발급해주는 비영리기관이다. 몇 가지 TLS 인증서 종류 중에서 완전 자동화가 가능한 DV (Domain Validated, 도메인 확인) 인증서를 무료로 발급한다.

- 발급된 인증서는 유효기간이 90일이며 만료 30일 전부터 갱신할 수 있다. 갱신 가능 횟수는 무제한이다.

## 1. Nginx 구성

- `/etc/nginx/conf.d/default.conf`
  - Nginx 재실행: `sudo nginx -t`, `sudo systemctl restart nginx`

```conf
# 기본 설정
server {
    listen 80;
    server_name example.com;

    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
    }

    location = /50x.html {
        root /usr/share/nginx/html;
    }
}

# SSL 적용 설정
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

    # HTTP → HTTPS 리다이렉트
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com www.your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;   # 인증서
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem; # 개인키

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8080; # 애플리케이션 서버
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 2. 인증서 발급 방식

Let's Encrypt SSL 인증서 발급 방식으로는 Webroot, Standalone, DNS 방식 3가지가 있다.

- **Webroot 방식**
  - Certbot이 특정 파일을 웹서버 경로에 생성하고, Let's Encrypt가 HTTP로 접근하여 확인한다.
  - 장점: 기존 웹 서버를 중단하지 않고 인증서를 발급받을 수 있다.
  - 단점: 인증 명령에 하나의 도메인 인증서만 발급 가능하다.

```bash
# 인증서 발급
sudo certbot certonly --webroot \
  -w /var/www/html \
  -d your-domain.com

# 동작 예시
# 1. Certbot이 파일 생성
/.well-known/acme-challenge/xxxx
# 2. Let's Encrypt가 접속
http://your-domain/.well-known/acme-challenge/xxxx
# 3. 파일 확인되면 인증 성공
```

- **Standalone 방식**
  - Certbot이 직접 임시 웹서버를 띄워서 인증한다.
  - 장점: 여러 도메인을 동시에 발급 받을 수 있다.
  - 단점: 인증서 발급 전에 Nginx를 중단하고 발급 완료 후 다시 Nginx를 시작해야 한다.

```bash
# 인증서 발급
sudo systemctl stop nginx # Nginx를 미리 중단
sudo certbot certonly --standalone -d your-domain.com

# 동작 예시
# 1. Certbot이 80포트 서버 실행
# 2. Let's Encrypt가 접속
# 3. 응답 확인 → 인증 완료
```

- **DNS 방식**
  - DNS에 TXT 레코드를 추가해서 도메인 소유 증명한다.
  - 장점: 와일드 카드 인증서 가능

```bash
# 인증서 발급
sudo certbot certonly --manual \
  --preferred-challenges dns \
  -d your-domain.com \
  -d *.your-domain.com

# 동작 예시
# 1. Certbot이 TXT 값 생성
# 2. DNS에 추가
_acme-challenge.your-domain.com
# 3. Let's Encrypt가 DNS 조회
# 4. 값 일치 → 인증 성공
```

## 3. 인증서 발급

- `Certbot 설치`

```bash
# Ubuntu / Debian 계열
sudo apt update

# Ubuntu / Debian 계열 - Nginx 이용
sudo apt install nginx
sudo apt install certbot python3-certbot-nginx

# Ubuntu / Debian 계열 - Apache 이용
sudo apt install nginx
sudo apt install certbot python3-certbot-apache

# Rocky Linux / CentOS 계열
sudo dnf install nginx

# Rocky Linux / CentOS 계열 - Nginx 이용
sudo dnf install epel-release -y
sudo dnf install certbot python3-certbot-nginx

# Rocky Linux / CentOS 계열 - Apache 이용
sudo dnf install epel-release -y
sudo dnf install certbot python3-certbot-apache
```

- `인증서 발급`

```bash
# 인증서 & 개인키 발급
sudo certbot --nginx -d www.example.com

# 인증서 & 개인키 확인
cat /etc/letsencrypt/live/{domain}/fullchain.pem # 인증서
cat /etc/letsencrypt/live/{domain}/privkey.pem   # 개인키
```

- `인증서 자동 갱신 설정`

```bash
# 인증서 갱신 테스트
sudo certbot renew --dry-run

# Cront 데몬 스케줄링 - 인증서 갱신
0 3 * * 1 /user/bin/certbot renew --quiet --deploy-hook "systemctl reload nginx"
```
