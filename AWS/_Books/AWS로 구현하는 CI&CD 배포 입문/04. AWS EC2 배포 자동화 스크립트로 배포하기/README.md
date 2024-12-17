# 04장. AWS EC2 배포 자동화 스크립트로 배포하기

## 환경 변수 설정

PID를 찾아 프로세스를 종료시키기 위해서는 실행 파일의 이름이 필요하다.  
해당 이름을 쉽게 찾을 수 있도록, 환경 변수로 등록한다.  

 - 환경 변수 등록 스크립트
    - gradlew로 프로젝트를 빌드하면, '프로젝트명-버전.jar' 형식으로 Jar 파일이 생성된다. 실행 중인 Jar 파일을 쉽게 찾을 수 있도록 환경 변수로 등록한다.
    - PID의 경우 pgrep 명령어로 쉽게 찾을 수 있다. 빌드된 결과물은 'build/libs' 폴더에 생성된다.
    - GITHUB_ID: 깃헙 ID
    - PROJECT_NAME: 프로젝트명
    - PROJECT_VERSION: 프로젝트 버전
    - PROJECT_PID: PID
    - JAR_PATH
```Bash
#!/bin/bash

GITHUB_ID="codingspecialist"
PROJECT_NAME="aws-v2"
PROJECT_VERSION="0.0.1"
PROJECT_PID="$(pgrep -f ${PROJECT_NAME}-${PROJECT_VERSION}.jar)"
JAR_PATH="${HOME}/${PROJECT_NAME}/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar"

export GITHUB_ID
export PROJECT_NAME
export PROJECT_VERSION
export PROJECT_PID
export JAR_PATH
```

<br/>

## 배포 스크립트

특정 스크립트에서 위에서 만든 환경 변수를 이용하기 위해서 해당 스크립트를 적용해주면 된다.  
'./source var.sh' 명령어를 수행 후에 스크립트 내에서 환경 변수를 사용할 수 있다.  

 - 배포 스크립트
    - 1: 환경 변수 스크립트를 이용하여 환경 변수를 등록한다.
    - 2: 크론탭을 초기화한다. (재배포 도중에 크론탭에 의한 재배포가 일어나면 꼬일 수 있다.)
    - 3: PID가 존재하는 경우 재배포이고, 없는 경우 처음 실행으로 간주한다. 처음 실행하는 경우 JDK 설치와 타임존을 설정한다. apt-get으로 라이브러리 설치시에 사용자 동의를 구하는 질문에 멈추지 않도록 -y 옵션을 추가한다. 또한, 표준 출력을 볼 필요가 없어 '/dev/null'로 옮겨준다.
    - 4: 재배포를 위해 기존에 폴더를 삭제한다.
    - 5: Github에서 소스 코드를 새로 받는다.
    - 6: 소스 코드를 빌드하기 위해 gradlew의 실행 권한을 부여한다.
    - 7: gradlew로 소스 코드를 빌드하여 Jar 파일을 만든다.
    - 8: Jar 파일을 실행한다.
    - 9: 트래픽으로 인해 서버가 인위적으로 종료되었을 경우 자동으로 재실행되도록 크론탭에 등록해준다. 크론탭의 내용은 1분마다 서버 동작을 감시하고, 동작하고 있지 않다면 재실행하도록 한다.
```Bash
#!/bin/bash

# 1. env variable
source ./var.sh
echo "1. env variable setting complete"

# 2. cron delete
touch crontab_delete
crontab crontab_delete
rm crontab_delete
echo "2. cron delete complete"

# 3. server checking
if [ -n "${PROJECT_PID}" ]; then
  # re deploy
  kill -9 $PROJECT_PID
  echo "3. project kill complete"
else
  # first deploy
  # 3-1 apt update
  sudo apt-get -y update 1>/dev/null
  echo "3-1. apt update complete"
  
  # 3-2 jdk install
  sudo apt-get -y install openjdk-11-jdk 1>/dev/null
  echo "3-2. jdk install complete"
  
  # 3-3 timezone
  sudo timedatectl set-timezone Asia/Seoul
  echo "3-3. timezone setting complete"
fi

# 4. project folder delete
rm -rf ${HOME}/${PROJECT_NAME}
echo "4. project folder delete complete"

# 5. git clone
git clone https://github.com/${GITHUB_ID}/${PROJECT_NAME}.git
sleep 3s
echo "5. git clone complete"

# 6. gradlew +x
chmod u+x ${HOME}/${PROJECT_NAME}/gradlew
echo "6. gradlew u+x complete"

# 7. build
cd ${HOME}/${PROJECT_NAME}
./gradlew clean build
echo "7. gradlew build complete"

# 8. start jar
nohup java -jar -Dspring.profiles.active=prod ${JAR_PATH} 1>${HOME}/log.out 2>${HOME}/err.out &
echo "8. start server complete"

# 9. cron registration
touch crontab_new
echo "* * * * * ${HOME}/check-and-restart.sh" 1>>crontab_new
# register the others... you use >> (append)
crontab crontab_new
rm crontab_new
echo "9. cron registration complete"

```

<br/>

 - PID가 존재하지 않는 경우 재실행 스크립트
    - 서버가 동작 중인지 확인하고, 동작 중이지 않다면 재실행한다.
    - 해당 파일에서도 환경 변수가 필요하기 때문에 환경 변수 파일을 등록해준다.
    - "-z" 옵션은 문자열의 길이가 0이면 참이다. 즉, 프로세스를 찾지 못하면 내부 명령어를 실행한다. (프로세스가 없다는 것은 서버가 종료된 상태를 말한다.)
```Bash
#!/bin/bash

source ./var.sh

if [ -z "$PROJECT_PID" ]; then
  nohup java -jar -Dspring.profiles.active=prod ${JAR_PATH} 1>${HOME}/log.out 2>${HOME}/err.out &
fi

```

<br/>

## 배포 스크립트 압축하기

배포를 위해 'var.sh', 'deploy.sh', 'check-and-restart.sh' 스크립트 파일을 만들었다.  
리눅스에서는 tar 명령어를 통해 여러 개의 파일을 모아서 압축할 수 있다.  
여러 개의 파일을 쉽게 관리할 수 있도록 tar 명령어로 압축 파일을 만든다.  

 - `tar 명령어`
    - '-c': 압축
    - '-x': 압축 풀기
    - '-v': 진행 과정을 화면에 출력
    - '-f': 압축 파일명 지정
```Bash
$ touch a.txt
$ touch b.txt

# 압축: a.txt와 b.txt를 압축하면서 진행과정을 출력한다. 압축 파일명은 test.jar이다.
$ tar -cvf test.tar a.txt b.txt

# 압축 해제
$ rm a.txt
$ rm b.txt
$ tar -xvf test.jar
```

<br/>

 - `배포 스크립트 압축`
    - 아래 명령어로 'deploy.tar' 라는 압축 파일을 만든다.
    - 해당 압축 파일을 SFTP를 이용하여 로컬에 옮기거나, EC2 서버에 전송할 수 있다.
```Bash
$ tar -cvf deploy.tar check-and-restart.sh deploy.sh var.sh
```

<br/>

## 테스트 없이 소스 코드 빌드 하기

기본적으로 gradle 빌드를 진행하면 항상 테스트를 거치게 된다.  
만약, 테스트를 생략하고 빌드하고 싶다면 '-x' 옵션을 주면 된다.  

```bash
$ ./gradlew build -x test
```
