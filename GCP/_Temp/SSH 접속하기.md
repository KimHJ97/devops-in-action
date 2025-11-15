# GCP VM 인스턴스 SSH 접속하기

 - SSH 키로 접속
 - gcloud 명렁어로 접속

<br/>

## 1. SSH 키로 접속 (수동)

### 1-1. ssh-keygen을 이용하여 비대칭키 발급

ssh-keygen은 SSH(Secure Shell) 프로토콜에서 사용하는 공개키/개인키 쌍을 생성하는 도구이다. 리눅스, macOS, Windows(WSL, Git Bash 포함) 등 거의 모든 환경에 기본 내장되어 있다.
 - `-t`: 키 타입 지정 (rsa, ed25519, ecdsa)
 - `-b`: 키 비트수(길이) 지정
 - `-C`: 키에 주석 추가
 - `-f`: 키 파일 저장 경로 지정
 - `-N`: 패스프레이즈 직접 지정
 - `-y`: 개인키에서 공개키 추출
 - `-p`: 기존 키의 패스프레이즈 변경
 - `-e`: 형식 변환 (OepnSSH -> PEM)
```bash
# RSA 키 발급
ssh-keygen -t rsa -f ~/.ssh/[키이름] -C [gmail계정] -b 2048

# 개인키에서 공개키 추출
ssh-keygen -y -f id_rsa > id_rsa.pub

# 기존 키의 패스프레이즈 변경
ssh-keygen -p -f ~/.ssh/id_rsa

# 형식 변환: OpenSSH ↔ PEM(외부 형식) 변환
ssh-keygen -e -m PEM -f id_rsa.pub
```
<br/>

### 1-2. 생성된 공개키에 대한 메타데이터(SSH 키) 등록

 - GCP -> 메타데이터 -> SSH 키: 해당 옵션으로 설정시 해당 키의 `comment` 부분이 사용자명으로 설정될 수 있다.
 - GCP -> VM 인스턴스 -> 상세 화면 -> 수정 -> SSH 키 항목 추가
    - 생성된 공개키(gcp-key.pub) 파일 내용 등록
```bash
# 키 발급
ssh-keygen -t rsa -b 4096 -C "myemail@example.com" -f ~/.ssh/gcp-key
```
<br/>

### 1-3. SSH 접속

 - ssh 명령어로 접속
    - USERNAME은 보통 이미지에 따라 다르다.
        - Ubuntu: `ubuntu`
        - Debian: `debian`
        - Rocky / CentOS / RHEL: `centos`, `rocky`, `ec2-user`
        - COS (Container-Optimized): `gcp`
```bash
ssh -i "$env:USERPROFILE\.ssh\gcp-key" [USERNAME]@[GCP_EXTERNAL_IP]
ssh -i "$env:USERPROFILE\.ssh\gcp-key" ubuntu@34.xx.xx.xx

# 특정 파일 업로드
scp -i [비밀키_경로] [로컬_파일_경로] [USERNAME]@[GCP_EXTERNAL_IP]:[원격_파일_경로]
```

### 1-4. SSH 주의 사항

SSH는 권한이 널널하면 인증 자체를 거부한다.

 - __파일 소유자 외에 다른 유저가 그 파일을 읽을 수 있으면 "Permission denied (publickey)" 에러가 발생한다.__
 - 개인키 (id_ed25519)는 오직 본인만 읽을 수 있어야 한다.
    - `id_ed25519`: 600
 - 서버의 authorized_keys 파일도 오직 소유자만 수정할 수 있어야 한다.
    - `~/.ssh`: 700
    - `~/.ssh/authorized_keys`: 600

```
# SSH 접속 흐름

1. 사용자가 SSH 실행
 - ~/.ssh/id_ed25519 (개인키)
 - ~/.ssh/id_ed25519.pub (공개키, 비교용)
 - ~/.ssh/known_hosts (서버 신뢰 확인용)
$ ssh -i ~/.ssh/id_ed25519 {user_id}@{ip}


2. 서버가 Host Key를 보내고 신뢰 여부 확인
서버(GCP VM)는 “이 서버가 맞다”는 Host Key 를 Mac에 보냄
Mac은 이걸 ~/.ssh/known_hosts 에서 확인.
 - 처음 접속 → fingerprint 확인 메시지 출력
 - yes 입력 → known_hosts 에 저장됨


3. SSH 인증 단계 – 공개키 인증 시도
Mac은 선택한 private key(예: id_ed25519) 를 사용하여
서버가 보낸 challenge 에 서명하고 서버에 보냄.
 - /home/<계정>/.ssh/authorized_keys 파일로 확인
 - 해당 파일 안에 Mac 공개키(id_ed25519.pub) 내용이 있어야 함


4. 서버는 authorized_keys 에 등록된 공개키와 비교
 - authorized_keys 안에 public key 있는지 확인
 - Mac이 서명한 signature 로 private key가 맞는지 검사
 - 서버 계정 권한 체크
 - 만약 아래 중 하나라도 틀리면 → Permission denied (publickey)


5. 로그인 성공 후 shell 제공
SSH 서버(SSHD)는 계정의 shell(/bin/bash 등)을 실행하고 세션 시작
```
<br/>

 - `SSH 접속 실패 주요 원인`
    - authorized_keys 에 키가 없음
    - authorized_keys 위치가 잘못됨. /home/user/.ssh/authorized_keys 여야 함
    - 서버 .ssh 권한 오류. 700, 600 아니면 실패
    - 계정 이름 틀림. authorized_keys 는 계정별로 다름
    - GCP OS Login ON. 이 경우 authorized_keys 무시됨

