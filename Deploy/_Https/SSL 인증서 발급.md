# SSL 인증서 발급

## 1. SSL 인증서 발급

- `CSR 생성 후 CA에 제출`
  - CSR: 인증서 발급 신청서(CA에 제출)

```bash
# 개인키 생성
openssl genrsa -out example.com.key 2048
openssl genpkey -algorithm RSA -out example.com.key

# CSR 생성
openssl req -new -key example.com.key -out example.com.csr
```

## 2. 인증서 발급 결과

- `발급된 인증서`
  - 서버 인증서 (example.com.crt, cert.pem)
  - 중간 인증서 (intermediate.crt)
  - 체인 인증서 (ca-bundle.crt)
  - 또는 합쳐진 fullchain

```bash
# 서버 인증서, 중간 인증서로 발급된 경우 fullchain을 생성해야 한다.
cat example.com.crt intermediate.crt > fullchain.pem
```

## 3. 인증서 적용

- fullchain.pem → 서버 인증서 + 중간 인증서
- privkey.pem → 직접 생성한 개인키

```bash
# Nginx 기준 설정
ssl_certificate     /path/to/fullchain.pem;
ssl_certificate_key /path/to/privkey.pem;
```

## 4. SSL 흐름

- 클라이언트 접속
- 서버가 fullchain 전달
- 클라이언트가 인증서 체인 검증
- 서버는 개인키로 암호화 통신 수행
