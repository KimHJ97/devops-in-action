# 기본 설정

## 1. 기본 프로그램 설정

```bash
# 기본 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# 타임존(Timezone) 설정
sudo timedatectl set-timezone Asia/Seoul
timedatectl   # 현재 시간 확인

# 로케일 설정
sudo apt install -y language-pack-ko
sudo update-locale LANG=ko_KR.UTF-8
source /etc/default/locale

# 유저 설정
sudo adduser ubuntu
sudo usermod -aG sudo ubuntu
sudo passwd ubuntu

# SSH 보안을 위해 root 접속 막기
sudo vi /etc/ssh/sshd_config
# 아래 설정 확인/수정
PermitRootLogin no
PasswordAuthentication no

# 필수 패키지 설치
sudo apt install -y curl wget unzip net-tools git vim htop

sudo apt install -y openjdk-21-jdk
java -version

sudo apt install -y docker.io docker-compose
sudo systemctl enable docker
sudo systemctl start docker
```
<br/>

## 2. SSH 허용 및 포트 열기

 - `SSH 허용`
    - 로컬 PC의 대칭키 발급
    - GCP -> 메타데이터 -> SSH 키 -> 로컬 PC의 공개키 입력
 - `포트 열기`
    - GCP -> VPC 네트워크 -> 방화벽 -> 방화벽 규칙 만들기
        - 이름: 
            - `{회사}-{서비스명}-{방향}-{소스or대상}-{프로토콜}-{포트}-{액션}`
            - cmeco-hr-comp-ingress-internet-tcp-443-allow
        - 대상: 대상 태그
        - 태그: 
            - `{팀/앱}-{역할/계층}-{환경}`
            - webapp-front-prod, api-backend-dev
        - 프로토콜 및 포트
            - 프로토콜: TCP
            - 포트: 443
    - GCP -> VM 인스턴스 -> 수정 -> 방화벽 -> 태그 추가
        - 네트워크 태그 추가

