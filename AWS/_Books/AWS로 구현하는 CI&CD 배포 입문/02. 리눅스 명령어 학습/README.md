# 02장. 리눅스 명령어 학습

## 리눅스 기본 명령어

 - 기본 명령어
```Bash
# clear: 터미널 화면을 비운다.
$ clear

# pwd: 현재 위치를 반환한다.
$ pwd

# cd: 폴더 이동
$ cd .. # 상위 폴더 이동
$ cd 폴더명 # 현재 위치에서 해당 폴더로 이동
$ cd / # 최상위 경로로 이동

# ls: 파일 목록 출력
$ ls
$ ls -l # 파일 목록을 리스트 형태로 

# find: 파일 찾기(최상위 디렉토리에서 이름이 tomcat으로 시작하는 파일 찾기)
$ sudo find / -name tomcat*
```

<br/>

 - 파일 관련 명령어
```Bash
# mkdir: 폴더 생성
$ mkdir 폴더명

# touch: 빈 파일 생성
$ touch 파일명.확장자

# rm: 파일 및 폴더 삭제
$ rm 파일명
$ rm -r 폴더명 # 폴더와 폴더 내부 파일 모두 삭제
$ rm -f 파일명 # 강제 삭제

# cp: 파일 복사
$ cp 파일명 복사된파일명

# mv: 파일 이동
$ mv 파일명 이동위치
$ mv 파일명 파일명 # 파일명 변경

# ln: 바로가기 링크 생성
$ ln -s 파일명 바로가기파일명
```

<br/>

## 리눅스 프로그램 설치 명령어

리눅스 환경에서 압축 파일 형식은 '.tar'를 사용한다.  
또한, 윈도우에서는 .msi 설치 파일을 실행하면 설치되지만, 리눅스에서는 .deb 파일을 이용한다.  

만약, 사이트에서 압축 파일을 다운로드받기 위해서는 wget 명령어를 사용한다.  
.tar로 압축된 파일을 풀거나, .deb 설치 파일로 설치해야 한다.  
설치가 끝나면, 환경변수 등록, 링크파일 등록, 시작프로그램 등록, 방화벽 개방 등이 필요하다.  

<br/>

### ubuntu repository

우분투에서 프로그램을 편하게 설치하기 위해 ubuntu repository를 사용한다.  

 - `ubuntu repository 등록`
```
1. 우분투에서 ubuntu repository로 접근하기 위해 '/etc/apt/sources.list' 파일에 주소를 등록해야 한다.
 - sources.list 파일에 repository 주소만 등록하면 연결된다.

2. apt에 대한 목록이 처음에는 비어있다.
 - apt는 Ubuntu, Debian 및 관련 Linux 배포에 deb 패키지를 설치, 업데이트, 제거 및 관리하기 위한 명령줄 유틸리티이다.
 - 등록 초기에 repository에 어떤 프로그램을 갖고 있는지 모르기 때문에, apt update 명령어를 수행하여 목록을 갱신해준다.

3. apt install 명령어로 프로그램을 설치한다.
 - 프로그램을 다운로드 및 자동 설치가 진행된다.
```

 - `실습`
```Bash
# 저장소 업데이트
$ sudo apt update

# 특정 프로그램 찾기
$ apt list | grep tomcat

# 특정 프로그램 설치
$ sudo apt install tomcat9

# 포트 확인 프로그램
$ sudo apt install net-tools
$ netstat -nlpt
```

<br/>

### PPA 저장소

unbuntu repository를 관리하는 곳에서 특정 프로그램이나, 특정 버전의 프로그램을 동기화하지 않는다면, apt 목록을 갱신해도 찾을 수가 없다.  
때문에, repository에 등록되지 않은 프로그램 설치를 원하면 직접 해당 프로그램 사이트를 들어가 설치해야하는 과정을 해야한다.  
이러한 과정을 해결하기 위해 PPA라는 저장소를 사용할 수 있다.  

PPA 저장소는 개인들이 필요한 프로그램을 올려둔 개인 저장소로 대부분의 프로그램을 PPA 사이트에서 찾을 수 있다.  
/etc/apt/sources.list.d에 PPA 저장소 주소를 등록해주면 된다.  

 - `실습`
    - Ubuntu Repository에서 tomcat8 버전을 갖고 있지 않다.
    - 개인 저장소를 추가하고, tomcat8 버전을 설치해본다.
    - PPA 찾기: https://launchpad.net/ubuntu/+ppas
```Bash
# 프로그램 삭제
$ sudo apt remove tomcat9 # 설정 파일은 삭제하지 않는다.
$ sudo apt --purge remove tomcat9 # 설정 파일까지 모두 삭제


# 우분투 코드 네임 확인: 우분투 버전과 코드명을 확인하여 지원하는 tomcat8을 설치
$ lsb_release -a

$ sudo add-apt-repository ppa:ttyrnpuu/tomcat # PPA 등록
$ sudo apt update

# tomcat8 설치
$ sudo apt install tomcat8
$ netstat -nltp

# PPA 저장소 삭제
$ sudo add-apt-repository --remove ppa:ttyrnpuu/tomcat
```

<br/>

### 프로세스와 서비스

apt 명령어로 프로그램을 설치하면 서비스에 등록된다.  
서비스 목록에 등록되면 실행 파일을 직접 찾아서 해당 경로에서 실행하지 않아도 된다.  

service 명령어는 systemctl의 wrapper script로 systemctl을 쉽게 사용할 수 있게 나온 wrapper 명령어이다.  
service로는 모든 서비스를 제어할 수 없다. 때문에, systemctl을 사용하는 것이 좋다.  

 - `service & systemctl`
```Bash
# 서비스 목록 확인: [+]는 실행중, [-]는 미실행
$ service --status-all

# tomcat 서비스 종료 및 실행
$ sudo service tomcat8 stop
$ sudo service tomcat8 start

# systemctl
$ sudo systemctl list-unit-files # 실행 중인 모든 시스템 확인
$ sudo systemctl list-unit-files | grep tomcat8 # tomcat8 검색

$ sudo systemctl status tomcat8 # 서비스 상태 확인
$ sudo systemctl stop tomcat8 # 서비스 종료
$ sudo systemctl start tomcat8 # 서비스 실행
```

<br/>

 - `kill 명령어`
    - 프로세스를 종료할 때 kill 명령어를 사용할 수 있다. systemctl을 사용하여 종료싴녀도 되지만, 종료가 안 되는 경우가 발생할 수 있다. 이러한 경우에는 kill 명령어를 이용한다.
    - kill 명령어로 프로세스가 종료되면, exited 상태가 된다. 때문에 systemctl로 exited 상태를 실행하기 위해서는 restart로 실행해야 한다.
```Bash
# ps: 실행 중인 프로세스 확인
$ ps -ef
$ ps -ef | grep tomcat

# kill: 프로세스 종료
$ sudo kill -9 PID # 강제 종료
$ sudo kill -15 PID # 안전하게 종료
```

<br/>

 - `tomcat8 pid를 찾는 script`
```Bash
# tomcat8 프로세스 확인
ps -ef | grep tomcat8

# 찾는 명령어를 제외하고 프로세스 확인
$ ps -ef| grep tomcat8 | grep -v grep

# awk 명령어로 공백을 기준으로 토큰화시켜 배열로 만들기
# 2번지(1번 인덱스)의 PID 값만 추출
$ ps -ef | grep tomcat8 | grep -v grep | awk '{print $2}'

# tomcat8 PID 종료시키기
$ sudo kill `ps -ef | grep tomcat8 | grep -v grep | awk '{print $2}'`
```

<br/>

## 권한

권한에는 읽기(r), 쓰기(w), 실행(x) 권한이 있고, 권한 그룹으로는 소유자 권한, 그룹 권한, 기타 권한이 있다.  

 - 
    - chmod: 권한 변경
    - chown: 소유자 및 소유 그룹 변경
```Bash
# 소유자(rw), 그룹(r), 기타(rw)로 변경
$ sudo chmod 646 test.txt

# 소유자는 root이고, 그룹은 ubuntu로 변경
$ sudo chown root:ubuntu test.txt
```

<br/>

## 표준 입출력

표준 입력 스트림(stdin: 0): 표준 입력 장치의 ID는 숫자로 0이며 일반적으로 키보드가 된다.  
표준 출력 스트림(stdout: 1): 출력을 위한 스트림으로 표준 출력 장치의 ID는 1이며 일반적으로 현재 쉘을 실행한 콘솔이나 터미널이 된다.  
오류 출력 스트림(stderr: 2): 에러를 위한 스트림으로 표준 에러 장치의 ID는 2이며 일반적으로 표준 출력과 동일하다.  

 - `표준 입출력 변경`
```Bash
# 톰캣 로그 위치 찾기
$ sudo find / -name catalina.out

# 톰캣 로그 확인
$ cd /var/log/tomcat8
$ sudo tail -f catalina.out

# mylog.out 파일로 표준 출력 변경
$ sudo touch mylog.out
$ sudo tail -f catalina.out > mylog.out
```
