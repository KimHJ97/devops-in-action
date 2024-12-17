# 03장. AWS EC2 기본 배포하기

 - 소스 코드: https://github.com/codingspecialist/aws-v1

<br/>

## 소스 코드

 - HelloController
    - '/aws/v1' 요청을 받고, 로그를 출력한다.
    - ex) http://localhost:8080/aws/v1?number=1
```java
@Slf4j
@RestController
public class HelloController {


    @GetMapping("/aws/v1")
    public String hello(@RequestParam(defaultValue = "1") Integer number){
        if(number == 1){ // info 로그
            log.info("/aws/v1 이 호출되었어요. info 로그 #####################################");
        }else if(number == -1){ // error 로그
            log.error("/aws/v1 이 호출되었어요. error 로그 #####################################");
        }else if(number == 0){ // warn 로그
            log.warn("/aws/v1 이 호출되었어요. warn 로그 #####################################");
        }
        
        return "<h1>aws v1</h1>";
    }
}
```

<br/>

## 배포 방법

 - `배포 흐름`
```
1. 로컬에서 Github에 소스 코드 업로드
2. EC2에서 Github 소스 코드 다운로드
3. 프로젝트 테스트
4. 프로젝트 빌드
5. nohup으로 백그라운드 실행
6. 오류 로그 남기기 (표준 입출력 리다이렉션)
7. 서버가 종료되면 cron으로 자동 재시작
```

<br/>

 - `배포`
    - gradlew는 프로젝트를 빌드 해주는 실행파일로 해당 파일을 실행하여 *.jar 파일을 만들 수 있다.
    - 프로젝트를 백그라운드로 실행하기 위해서는 nohub 명령어를 이용한다. nogub은 프로세스를 실행한 터미널의 세션 연결이 끊어지더라도 지속적으로 동작할 수 있게 해주는 명령어이고, 명령어 끝에 & 를 정의하면 해당 명령어가 백그라운드로 동작하게 된다.
```Bash
# 1. 소스 코드 다운로드(적당한 폴더로 이동하고, 다운로드받는다.)
$ git --version # git 설치 확인
$ git clone 깃허브주소

# 2. 프로젝트 소스코드 빌드
$ cd aws-v1 # 프로젝트 폴더로 이동
$ ls -l # gradlwe의 실행 권한을 확인
$ chmod u+x gradlew # gradlew 실행 권한 추가
$ ./gradlew build # 프로젝트 빌드: build/libs 폴더에 생성된다.

# 3. jar 파일 실행하기
$ cd build/libs
$ java -jar v1-0.0.1-SNAPSHOT.jar # java -jar *.jar

# 4. 백그라운드로 실행하기
$ nohub java -jar *.jar &

# 5. 프로세스 종료
$ netstat -nltp # 포트 확인
$ ps -ef | grep java # 프로세스 확인
$ kill -9 포트번호
```

<br/>

 - `종료 스크립트`
    - 실행 중인 서버를 종료하기 위해서 매번 PID를 찾고, 직접 kill 하는 일은 번거롭다. 때문에, 종료 스크립트를 만들어 자동화할 수 있다.
```Bash
# grep 명령어로 PID 찾기
$ ps -ef | grep *.jar | grep -v grep | awk '{print $2}'

# pgrep 명령어로 PID 찾기
$ pgrep -f *.jar

# 종료 스크립트 만들기
$ vi spring-stop.sh

echo "SPRINGBOOT STOP..."
SPRING_PID=$(pgrep -f v1-0.0.1-SNAPSHOT.jar)
kill -9 $SPRING_PID

# 종료 스크립트 권한 부여 및 사용해보기
$ chmod u+x spring-stop.sh
$ ./spring-stop.sh
```

<br/>

## cron 주기적 실행

애플리케이션은 도중에 부하가 심하거나, 에러가 발생하는 등의 이유로 서버가 종료될 수 있다.  
서버가 종료된 것을 확인하고, 서버에 접속하여 재시작하는 과정은 번거롭다.  
때문에, cron 기능을 이용하여 자동으로 재시작시킬 수 있다.  

cron은 시간 기반 잡 스케줄러로 작업을 고정된 시간, 날짜, 간격에 주기적으로 실행할 수 있도록 스케줄링 하기 위해 사용한다.  
cron 작업을 설정하는 파일을 crontab 이라고하며, cron 프로세스는 /etc/crontab 파일에 설정된 내용을 읽어서 작업을 수행한다.  
분(0~59), 시간(0~23), 일(1~31), 월(1~12), 요일(0~7) 순서로 주기를 설정하고, 이후 실행할 명령어 혹은 파일을 지정한다.  

 - `cron 등록해보기`
    - 1분마다 파일 목록을 cron.log 파일에 쌓는 스케줄러를 만든다.
```Bash
# cron table 편집
$ crontab -e
* * * * * ls -l 1>>cron.log
```

<br/>

 - ``
```Bash
# 애플리케이션 종료 스크립트
$ vi spring-stop.sh

echo "SPRINGBOOT STOP..."
SPRING_PID=$(pgrep -f v1-0.0.1-SNAPSHOT.jar)
kill -9 $SPRING_PID

# 애플리케이션 재실행 스크립트
$ vi spring-restart.sh

SPRING_PID=$(pgrep -f v1-0.0.1-SNAPSHOT.jar)
SPRING_PATH="/home/ubuntu/aws-v1/build/libs/v1-0.0.1-SNAPSHOT.jar"

echo $SPRING_PID
echo $SPRING_PATH

if [ -z "$SPRING_PID" ]; then
  echo "스프링이 종료된 상태..."
  echo "스프링 재시작 - $(date)" 1>>/home/ubuntu/cron-restart/spring-restart.log
  nohup java -jar $SPRING_PATH 1>log.out 2>err.out &
else
  echo "스프링이 시작된 상태..."
fi

# 재실행 cron 등록
$ crontab -e

* * * * * /home/ubuntu/cron-restart-spring-restart.sh

```
