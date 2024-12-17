# 07장. 엘라스틱빈스톡 & 데이터베이스 & CI/CD 배포하기

엘라스틱빈스톡과 RDS와 GithubAction을 활용한 CI/CD 배포에 대해서 배운다.  
IAM 사용자 인증에 대해서 배운다.  
무중단 배포(롤링)를 배운다.  
네트워크 로드밸런서를 활용하여 고정 IP를 설정하는 방법을 배운다.  

<br/>

## 배포 방식 구성

기존에 배포 방식은 배포 후 실행햇을 때 무조건 배포에 성공한다고 보장할 수 없다.  
왜냐하면, 해당 프로젝트를 테스트했던 환경은 로컬 컴퓨터인 윈도우 환경이지만, 실제로 배포하여 실행하는 환경은 리눅스 환경이기 때문이다.  
테스트한 환경과 실행되는 환경이 다르기 때문이다.  

CI 과정을 만들어, CI 서버에서 AWS 리눅스 환경에서의 OS에서 테스트하고 빌드하여 실행 파일을 만들도록 한다.  
이후 CD 서버에서 실행 파일을 생성한 후 자동 배포를 하도록 한다.  


 - `이전 배포 방식 구성`
    - 1: 로컬 컴퓨터에서 프로젝트 생성
    - 2: gradlew로 테스트 후 빌드하여 Jar 파일 생성
    - 3: 실행 파일을 AWS에 배포
        - EC2 사용: OS 설치, Java 설치, 실행 파일 옮기기, 직접 실행
        - 엘라스틱빈스톡 사용: 실행 파일 옮기기
 - `새로운 배포 방식 구성`
    - 1: 로컬 컴퓨터에서 프로젝트 생성
    - 2: 테스트와 빌드를 로컬 컴퓨터에서 하지 않고, 로컬 컴퓨터에서 프로젝트를 Github으로 배포
    - 3: Github에 프로젝트 코드가 들어오면 Github이 코드의 변경을 감지하여 CI 서버를 만든 뒤 이 서버로 프로젝트를 전달
    - 4: CI 서버에서 테스트하고 빌드하여 실행 파일 생성 (CI 서버는 AWS 환경과 동일해야 한다.)
    - 5: CI 서버에서 AWS로 배포한 뒤 실행 (CI 서버에서 테스트를 성공했다면 실제 환경에서도 실행에 성공을 보장하게 된다.)
    - 6: CD 서버를 통해 자동 배포 진행
    - 주의점: CI 서버에서 AWS에 접근하여 배포할 때 자동으로 CD를 하기 위해서는 Access Key가 필요하다. 접근키를 만들기 위해서 AWS의 IAM 개념 이해가 필요하다.

<br/>

## CI/CD란

CI는 개발자를 위한 자동화 프로세스인 지속적인 통합을 의미한다.  
CI를 성공적으로 구현할 경우 애플리케이션에 대한 새로운 코드 변경 사항이 정기적으로 빌드 및 테스트되어 공유 저장소에 통합되므로 여러 명의 개발자가 동시에 애플리케이션 개발과 관련된 코드 작업을 할 경우 서로 충돌할 수 있는 문제를 해결할 수 있다.  

CD는 지속적인 서비스 제공 또는 지속적인 배포를 의미한다.  
두 가지 의미 모두 파이프라인의 추가 단계에 대한 자동화를 뜻하지만 떄로는 얼마나 많은 자동화가 이루어지고 있는지를 설명하기 위해 별도로 사용되기도 한다.  

 - 폴링(Polling) 기법
    - 서버를 하나 만들고, 서버에서 Github 저장소에 주기적으로 요청을하여 코드가 push 되었는지 물어본다.
    - 간헐적인 시간을 두고 계속해서 요청을 통해 코드의 변화가 있는지 물어보는 것을 폴링 기법이라고 한다.
    - 코드의 변화가 감지되면 Github의 코드를 다운받고, 테스트 및 빌드 과정을 거쳐 배포하게 된다.
 - 웹훅(Webhook) 기법
    - 웹훅은 Github에서 제공해주는 기능으로 저장소에 변경이 일어나면 지정한 서버에 API를 요청을 준다.
    - 특정 브랜치에 소스 코드가 업로드 되었을 때 업로드되었다는 이벤트를 전달해준다.
    - 서버는 Hook을 받으면 Github의 소스 코드를 다운받고, 테스트 및 빌드 과정을 거쳐 배포하게 된다.

<br/>

## 보안 그룹 설정

EC2나 엘라스틱빈스톡을 생성하면 자동으로 보안 그룹이 생성되지만, 이름이 알아보기 힘들고 헷갈린다.  
떄문에, RDS와 EC2를 같이 관리할 수 있는 보안 그룹을 직접 만들어준다.  

 - `보안 그룹을 만들고, 보안 설정하기`
    - EC2와 RDS 각각 보안 그룹을 만들고, EC2 보안 그룹인 sg-01에 80 포트와 22 포트를 열고, RDS 보안 그룹인 sg-02에는 22 포트와 3306(내IP), 3306(sg-01) 포트를 여는 방법이 있다.
    - EC2에서 3306 포트를 열 필요가 없고, RDS에 80 포트를 열어둘 필요가 없어 해당 방법이 더 정확한 방법이지만, 편하게 사용하기 위해 하나의 보안 그룹으로 사용한다.
```
1. EC2 대시 보드 > 네트워크 및 보안 > 보안 그룹
 - 이름: security-group-aws-v5
 - 설명: sg-aws-v5
 - VPC: vpc-..

2. 보안 설정
 - SSH 22 Anywhere IPv4
 - HTTP 80 Anywhere IPv4
 - MySQL/Aurora 3306 내 IP
 - MySQL/Aurora 3306 같은 보안그룹
```

<br/>

## RDS 생성

 - `RDS 생성`
```
1. RDS 생성
MariaDB > 프리티어
 - 이름: aws-v5-mariadb
 - 마스터 사용자 이름: test
 - 마스터 암호: 1234
 - 퍼블릭액세스: 예
 - 보안 그룹: security-group-aws-v5

2. RDS 수정
RDS > 파라미터 그룹 > 파라미터 그룹 생성
 - 이름: aws-v5-mariadb-group

3. 파라미터 그룹 편집
 - time_zone: Asia/Seoul
 - character_set_client: utf8mb4
 - character_set_connection: utf8mb4
 - character_set_database: utf8mb4
 - character_set_filesystem: utf8mb4
 - character_set_results: utf8mb4
 - character_set_server: utf8mb4

4. RDS에 파라미터 그룹 적용
RDS 인스턴스 > 추가 구성
 - 데이터베이스 옵션
    - DB 파라미터 그룹: aws-v5-mariadb-group
 - 수정 예약
    - 즉시 적용
```

<br/>

 - `데이터베이스 접속`
    - RDB 생성이 완료되었다면 DB Client 툴로 접속해 기본 세팅을 한다.
    - 초기 설정이 미국 타임존으로 설정되어 SET GLOBAL time_zone='Asia/Seoul'; 명령을 수행해야 하지만, 권한이 없어 오류가 발생한다.
    - 본래 DB에서 한글 설정과 시간 설정이 가능하지만, RDS는 DB에 직접 수정할 권한이 없기 때문에 파라미터 그룹으로 한글 설정과 시간 설정을 해야한다.
```sql
-- 데이터베이스 생성
CREATE DATABASE testdb;

-- 사용할 데이터베이스 선택
USE testdb;

-- 테이블 생성
CREATE TABLE Book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    content VARCHAR(255),
    author VARCHAR(255)
);

-- 타임존 확인
SELECT @time_zone, NOW();
```

<br/>

## 엘라스틱빈스톡 구성

 - `엘라스틱빈스톡 생성`
```
1. Elastic Beanstalk 대시 보드 > Create Application
 - 애플리케이션 정보
    - 애플리케이션 이름: aws-v5
 - 플랫폼
    - 플랫폼: Java
    - 플랫폼 브랜치: Corretto 17 running on 64bit Amazon Linux 2
    - 플랫폼 버전: 3.3.1 (Recommended)
 - 추가 옵션 구성
    - 구성 사전 설정: 사용자 지정 구성
 - 소프트웨어 > 환경 속성
    - RDS_HOSTNAME: RDS의 엔드포인트
    - RDS_DB_NAME: testdb
    - RDS_PORT: 3306
    - RDS_USERNAME: test
    - RDS_PASSWORD: 1234
 - 용량 수정 (오토 스케일링 그룹)
    - 환경 유형: 로드 밸런싱 수행
    - 인스턴스: 최소2/최대4
        -> EC2 2대가 생긴다. EC2는 ALB를 두고 클라이언트는 ALB에게만 접근, ALB가 부하 분산하여 EC2에 전달
        -> 2대가 모두 바쁘다면 자동으로 서버를 4대로 복제하고, 한가해지면 다시 2대만 가동시킨다.
 - 로드 밸런서 유형
    - Application Load Balancer
```

<br/>

## Github Action

로컬 컴퓨터에서 Github에 소스 코드를 Push하면, CI 서버에서 Ubuntu 설치, JDK 설치, 소스코드 다운로드, 코드 테스트 및 빌드를 진행한다.  
자동으로 Github 저장소에서 CI 서버를 제공해주며, 해당 기능을 이용하기 위해서는 Actions 기능을 활성화해주어야 한다.  
Github 저장소로 이동하고, Actions 메뉴에서 'I understand my workflows, go ahead and enable them' 버튼을 눌러 활성화해준다.  

 - `엘라스틱빈스톡 배포 흐름`
   - .ebextensions/*.config
      - AWS Elastic Beanstalk는 소스 코드 내부에 .ebextensions 폴더를 추가하여 환경을 구성하고 환경에 잇는 AWS 리소스를 사용자 지정할 수 있다. 구성 파일은 .config 파일 확장명을 사용하는 YAML이나 JSON 형식 문서로, .ebextensions 폴더에 놓고 애플리케이션 소스 번들로 배포합니다.
      - files는 파일을 만들 수 있다. 예제에서는 '/sbin/appstart' 파일을 만들고, 권한을 부여하고, 내용으로 애플리케이션을 실행하도록 지정한다.
   - Profile
      - AWS Elastic Beanstalk는 소스 코드 내부에 Profile이 없으면, 기본적으로 'java -jar application.jar' 내용이 들어있는 Profile 파일을 만들어 해당 파일로 애플리케이션을 실행한다.
      - 직접 소스 코드에 Profile을 만들어 해당 Profile 명령으로 실행하도록 할 수 있다. 예제에서는 ebextensions으로 '/sbin/appstart' 명령을 지정하였고, Profile에서 appstart 명령을 수행하도록 한다.
```
1. JAR 배포
*.jar 파일을 엘라스틱빈스톡에 배포하면 EB 내부의 /var/app/current 폴더 내부에 *.jar 파일이 배포될 떄 자동으로 application.jar로 이름을 변경한다.
또한, 같은 경로에 'java -jar application.jar' 명령이 존재하는 Procfile 파일을 만든다.

2. zip 배포
*.zip 파일을 배포하면 알아서 압축을 풀고 current 폴더에 들어간다.
이후 배포를 시작할 때 Procfile을 실행하게 된다.

 └─ /var/app/current
                ├─ application.jar
                ├─ Procfile
                └─ .ebextensions/00-makefile.config

```

<br/>

 - `IAM 생성 및 Github 환경 변수 등록`
   - Github Action에서 AWS의 S3에 접근 권한이 있는 사용자를 생성한다.
   - AWS IAM에서 생성한 사용자 정보를 Github 환경 변수로 등록해준다. (Github Actions에서 사용하기 위함)
```
1. IAM 생성
 - IAM 대시 보드 > 사용자 > 사용자 추가
   - 사용자 이름: test
   - AWS 자격 증명 유형 선택: 액세스 키 - 프로그래밍 방식
   - 권한 설정: 기존 정책 직접 연결 > AdministratorAccess-AWSElasticBeanstalk

2. Github 환경 변수 등록
 - Github > Settings > Secrets > Actiuons > New repository secret
   - Acess Key와 Secret Key를 등록해준다.
      - Name: AWS_ACCESS_KEY
      - Value: IAM Access Key 정보 입력
```

<br/>

 - `.github/workflows/deploy.yml`
   - CI를 자동으로 진행하기 위해서는 '.github/workflows' 하위에 *.yml 파일이 존재해야 한다.
   - Github Repository는 해당 경로에 yml 파일을 확인하면 CI 서버로 이벤트를 발생한다.
   - on.push.branches: main 브랜치에 push가 되면 job를 실행한다.
   - jobs 하위에 여러 개의 job을 지정할 수 있다. 현재 jobs 하위에 build라는 이름의 job을 정의하였다.
   - runs-on: 베이스 이미지로 ubuntu를 사용한다.
   - actions/*은 스크립트 코드들을 라이브러리 형태로 제공해준다.
      - actions/checkout@v3: 소스코드 다운로드 스크립트를 수행해준다.
      - actions/setup-java@v3: apt-get install open-jdk 같이 JDK 설치 스크립트를 수행해준다. with 구문으로 상세한 설정을 해줄 수 있다.
```yml
name: aws-v5
on:
  push:
    branches:
      - main

# https://github.com/actions/setup-java
# actions/setup-java@v2는 사용자 정의 배포를 지원하고 Zulu OpenJDK, Eclipse Temurin 및 Adopt OpenJDK를 기본적으로 지원합니다. v1은 Zulu OpenJDK만 지원합니다.
jobs:
  build:
    runs-on: ubuntu-latest # Ubuntu 운영체제 환경에서 진행
    steps:
      - name: Checkout # 소스코드를 CI 서버에 다운로드
        uses: actions/checkout@v3
      - name: Set up JDK 11 # JDK 설치
        uses: actions/setup-java@v3
        with:
          java-version: 11
          distribution: zulu
      - name: Pemission # gradlew의 실행 권한 부여
        run: chmod +x ./gradlew
      - name: Build with Gradle # gradlew를 이용하여 소스코드 빌드
        run: ./gradlew clean build

      # UTC가 기준이기 때문에 한국시간으로 맞추려면 +9시간 해야 한다
      - name: Get current time
        uses: 1466587594/get-current-time@v2
        id: current-time
        with:
          format: YYYY-MM-DDTHH-mm-ss
          utcOffset: "+09:00"
      - name: Show Current Time
        run: echo "CurrentTime=${{steps.current-time.outputs.formattedTime}}"

     # EB에 CD 하기 위해 추가 작성
      - name: Generate deployment package
        run: |
          mkdir deploy
          cp build/libs/*.jar deploy/application.jar
          cp Procfile deploy/Procfile
          cp -r .ebextensions deploy/.ebextensions
          cd deploy && zip -r deploy.zip .

      # 엘라스틱빈스톡으로 실제 배포
      # AWS의 S3 저장소(버킷)으로 deploy.zip 파일을 던진다.
      - name: Deploy to EB
        uses: einaregilsson/beanstalk-deploy@v21
        with:
          aws_access_key: ${{ secrets.AWS_ACCESS_KEY }}
          aws_secret_key: ${{ secrets.AWS_SECRET_KEY }}
          application_name: aws-v5-beanstalk # 엘리스틱 빈스톡 애플리케이션 이름!
          environment_name: Aws-v5-beanstalk-env # 엘리스틱 빈스톡 환경 이름!
          version_label: aws-v5-${{steps.current-time.outputs.formattedTime}}
          region: ap-northeast-2
          deployment_package: deploy/deploy.zip
```
