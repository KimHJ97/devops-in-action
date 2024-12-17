# EC2 JDK 설치

 - JDK 설치
    - 실행 파일로 빌드하기 위해서는 JDK가 필요하다.
    - JAR 파일을 실행하기 위해서는 JRE가 필요하다.
```Bash
$ sudo apt update
$ sudo apt-cache search jdk
$ sudo apt-cache search jdk | grep openjdk-11

$ sudo apt install openjdk-11-jdk
$ java --version
```
