# 01장. AWS 이해

## AWS 배포를 위한 프로젝트 환경설정

AWS 환경에서 스프링 부트 애플리케이션을 배포하기 위한 환경을 설정한다.  

 - `JDK 설치`
    - 다운로드 경로: https://jdk.java.net/archive/
    - 컴파일을 위한 컴파일러, Java 애플리케이션을 실행시키기 위한 JVM, 명령어 집합인 JRE가 필요하다.
```
1. 환경변수 설정
Java 실행파일을 어디에서나 실행할 수 있도록 환경변수를 설정해주어야 한다.
java.exe와 javac.exe가 있는 bin 폴더 위치를 지정한다.
 - 윈도우 검색 > 시스템 환경 변수 편집
 - 시스템 변수 새로 만들기
    - JAVA_HOME: java 설치 경로
 - Path 변수 수정
    - %JAVA_HOME%\bin
```

<br/>

 - `VSCode 설치`
    - 다운로드 경로: https://code.visualstudio.com/download
    - VSCode는 안드로이드, 자바, HTML 등 모두 사용할 수 있는 통합 개발환경(IDE) 툴이다.
    - Java 애플리케이션 개발을 위한 확장 프로그램을 설치해주어야 한다.
```
1. 확장 프로그램 설치
 - Extension Pack for Java
 - Spring Boot Extension Pack
 - Lombok Annotations Support for VS Code
```

<br/>

 - `포스트맨 설치`
    - 다운로드 경로: https://www.postman.com/downloads/
    - 포스트맨은 HTTP Client 툴로 HTTP 요청을 쉽게 테스트할 수 있다.

<br/>

 - `Git 설치`
    - 다운로드 경로: https://git-scm.com/downloads
```
1. VSCode 터미널 설정
VSCode 터미널에서 git bash로 열리도록 지정할 수 있다.
 - VSCode > 터미널 > 우클릭 > Select Default Profile > Git Bash
```

<br/>

## 학습 목표

 - 1. 클라우드 서비스 활용을 위한 기본 지식 학습
    - Linux 문법, Network에 대한 컴퓨터 지식
 - 2. 클라우드 서비스에 배포하기 위한 환경 구축
    - EC2 머신 생성
 - 3. 클라우드 서비스에 애플리케이션 배포를 간편하게 만들기
    - Shell Script 학습
 - 4. 클라우드 서비스에 환경 구축 없이 애플리케이션 배포
    - Elastic Beanstalk로 환경 구축 없이 애플리케이션 배포가 가능하도록 한다.
    - PasS: 물리적 컴퓨터에 플랫폼을 얹어서 제공해주는 서비스로 자바 프로젝트 배포시 JDK가 설치되어 있다.
    - IaaS: 물리적인 컴퓨터를 제공하여 직접 환경을 구축해야 한다.
 - 5. 클라우드 서비스에 배포 자동화 구축
    - Github Action을 통해 배포 자동화를 이용한다.
 - 6. 클라우드 서비스 무중단 배포
    - AWS가 제공해주는 Load Balancer와 Rolling 배포를 배운다.
 - 7. 정적 IP 할당을 위한 Network Load Balancer 활용
    - Network Load Balancer를 활용하여 정적 IP를 할당한다.
 - 8. 최종 목표
    - 테스트 코드 작성
    - 프로젝트를 Github에 push
    - Github Action으로 push 이벤트가 트리거되어, 애플리케이션 테스트 코드 수행 후 빌드
    - 빌드가 정상적으로 완료되면 AWS에 자동 배포
    - 배포시에 AWS 제일 앞단에 EC2와 연결된 로드밸런서를 두고, 로드밸런서가 트래픽 부하 분산을 도와주도록 한다.

<br/>

## AWS 탄생 배경

아마존은 처음 인터넷 서점으로 시작하여 다양한 상품을 판매하며 이용자가 늘어났다.  
미국에는 연말에 폭탄 세일을 해서 물건을 판매하는 블랙 프라이데이가 존재하는데, 아마존은 블랙 프라이데이와 같이 이용자가 갑자기 폭증할 때를 대비하여 필요한 서버 컴퓨터를 미리 구축해두었다.  
미리 구축해둔 서버는 블랙 프라이데이가 끝나게 되면 필요가 없게 되는 상황이 발생하는데, 아마존은 이러한 놀고 있는 컴퓨터를 다른 회사에 대여해주는 클라우드 서비스를 시작하게 된다.  

AWS 입장에서는 모든 서버가 On-premise이고, 서버를 대여하는 개인의 입장에서는 off-premise가 된다.  
AWS는 전 세계에 물리적인 서버 컴퓨터를 원격으로 대여해주는 서비스를 한다.  

<br/>

## EC2 생성 및 서버 접속하기

 - `EC2 만들기`
    - AWS 클라우드 환경에 컴퓨터를 임대하고, 우분투 OS를 설치하고, 30GB 스토리지 할당, 1GB 메모리를 할당한다.
```
 - EC2 대시보드 > 인스턴스 시작
    - AMI: Ubuntu Server 20.04 LTS
    - 인스턴스 유형: t2.micro
    - 키 페어 생성
        - 키 페어 이름: aws-key
        - 키 페어 유형: RSA
        - 프라이빗 키 파일 형식: .pem
    - 스토리지 구성
        - 30 GiB gp2
    - 인스턴스 개수
        - 1
```

<br/>

 - `EC2 서버 접속하기(Windows)`
    - mobaXterm 설치
        - mobaXterm은 윈도우 전용 원격 접속 도구이다.
        - https://mobaxterm.mobatek.net/download.html
```
1. mobaXterm 실행
 - Session 클릭
    - SSH
    - Remote host: EC2 인스턴스의 IP 주소
    - username: ubuntu
    - Port: 22
    - Use private key: 생성한 aws-key.pem 파일 경로 지정
```

 - `EC2 서버 접속하기(Mac)`
```Bash
# aws-key 파일이 저장되어 있는 경로로 이동
$ cd /Users/../

# aws-key 파일에 실행 권한 부여
$ chmod 700 aws-key.cer

# EC2 접속
$ ssh -i aws-key.cer ubuntu@{IP주소}
```

<br/>

## EC2 서버 방화벽

클라우드 컴퓨터는 모든 포트가 차단된 방화벽을 가지고 있다.  
방화벽은 미리 정의된 보안 규칙에 기반을 두고, 들어오고(인바운드) 나가는(아웃바운드) 네트워크 트래픽을 모니터링하고 제어하는 네트워크 보안 시스템이다.  

방화벽은 모든 포트가 차단되어 있는 상태에서 특정 포트를 개방하는 식으로 보안 설정을 한다. AWS에서는 라우터 및 스위치에서 제공하는 ACL을 사용한다. 특정 포트를 개방하고 싶으면 ACL에 인바운드 규칙과 아웃바운드 규칙을 정의하면 된다.  
 - SSH 22번 포트(인바운드)
 - HTTP 80번 포트(인바운드)
 - HTTPS 443번 포트(인바운드)

