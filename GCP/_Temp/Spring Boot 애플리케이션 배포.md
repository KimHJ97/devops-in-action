# Spring Boot 애플리케이션 배포

## 1. 기본 설정

```bash
# 한국 시간대 설정
timedatectl
sudo timedatectl set-timezone Asia/Seoul
```

## 2. 배포

```bash
# JDK 설치
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version

# 로컬의 Jar 파일 옮기기
scp -i [공개키_경로] [파일_경로] ubuntu@34.xx.xx.xx:[원격_파일_경로]
```
