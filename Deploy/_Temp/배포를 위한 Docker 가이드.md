# 배포를 위한 Docker 가이드

## Docker 환경 애플리케이션 고려 사항

Docker 환경에서 애플리케이션을 안정적으로 배포할려면, 환경 변수 및 네트워크 관리로 환경을 효율적으로 구성하고, 로그 및 모니터링 도구를 통해 애플리케이션 상태를 추적하며, 보안 및 데이터 영속성을 보장하는 설정을 적용하는 것이 중요하다. 필요하다면 k8s와 같은 오케스트레이션 도구를 통해 확장성과 자동화도 고려할 수 있다.  

 - 환경 변수 관리
    - 애플리케이션이 동작하는 환경에 따라 설정 파일이나 환경 변수가 달라질 수 있다.
    - Docker Compose 내부에 환경 변수를 설정하거나, '.env' 파일을 불러와서 적용할 수도 있다.
```yml
# Docker Compose에 환경 변수 정의
services:
  spring_app:
    image: myapp:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=db.example.com
      - DB_PORT=3306

# .env 파일 불러오기
services:
  spring_app:
    image: myapp:latest
    env_file:
      - .env
```

 - 네트워크 설정
    - 컨테이너 간의 통신, 외부 접근 제한, 네트워크 분리 등을 적절하게 설정하는 것이 중요하다.
```yml
services:
  web:
    image: nginx
    networks:
      - frontend
  app:
    image: myapp
    networks:
      - backend
  db:
    image: mysql
    networks:
      - backend

networks:
  frontend:
  backend:
```

 - 데이터 영속성 관리(볼륨)
    - Docker 컨테이너는 기본적으로 상태가 없는(stateless) 구조이므로, 애플리케이션이 저장하는 파일이나 데이터베이스 등의 영속 데이터를 유지하기 위해 볼륨을 사용하는 것이 중요하다.
```yml
services:
  db:
    image: mysql:5.7
    volumes:
      - db-data:/var/lib/mysql

volumes:
  db-data:
```

 - 보안 설정
    - 최소 권한 원칙 적용: 컨테이너 내에서 루트 사용자가 아닌 일반 사용자 권한으로 애플리케이션을 실행하도록 설정한다.
    - 네트워크 보안
        - 외부에서 접근이 필요한 포트만 개방하고, 나머지는 제한한다.
        - 컨테이너 간 통신은 필요한 경우에만 허용되도록 네트워크를 구성한다.
    - 이미지 보안
        - 신뢰할 수 있는 공식 이미지나 검증된 이미지만 사용한다.
        - Docker 이미지를 자주 스캔하여 취약점을 확인하고 패치한다.
        - 이미지 크기를 최소화하여 공격 벡터를 줄이기 위해 필요한 부분만 포함한 멀티 스테이지 빌드를 사용한다.
```dockerfile
# 최소 권한 원칙 예시
FROM openjdk:17-jdk-alpine
RUN addgroup -S mygroup && adduser -S myuser -G mygroup
USER myuser

# 멀티 스테이지 빌드 예시
# Multi-stage build example
FROM openjdk:17-jdk-alpine as build
COPY . /app
WORKDIR /app
RUN ./gradlew build

FROM openjdk:17-jdk-alpine
COPY --from=build /app/build/libs/myapp.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

 - 모니터링 및 로깅
    - Docker 환경에서는 컨테이너의 상태를 지속적으로 모니터링하고, 애플리케이션 로그 및 시스템 로그를 수집하여 관리해야 한다.
    - 모니터링 도구
        - Prometheus와 Grafana: 컨테이너의 메트릭을 수집하고 대시보드를 통해 시각화하는 데 사용한다.
        - Docker Stats: 기본적인 컨테이너 CPU, 메모리 사용량 등을 확인할 수 있다.
    - 로깅
        - 로그는 표준 출력(stdout)이나 표준 에러(stderr)로 출력하게 하고, 중앙 집중식 로깅 시스템(ELK Stack, Fluentd 등)을 활용하여 로그를 수집하고 분석할 수 있다.
```bash
docker status
```

 - 자동 재시작 및 보구
    - 컨테이너가 실패했을 때 자동으로 재시작하거나, 서버를 재부팅할 때도 컨테이너가 자동으로 실행되도록 설정할 수 있다.
```yml
services:
  spring_app:
    image: myapp:latest
    restart: always
```

 - 리소스 제한 설정
    - 컨테이너가 과도한 리소스를 사용하지 않도록 CPU와 메모리 사용량을 제한한다.
```yml
services:
  spring_app:
    image: myapp:latest
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
```

 - 오케스트레이션
    - Docker 컨테이너가 여러 대의 서버에 분산 배포되거나, 높은 수준의 가용성과 확장성을 요구하는 경우, k8s 같은 컨테이너 오케스트레이션 도구를 사용하는 것이 좋다.
    - k8s는 자동 복구, 수평 확장, 무중단 배포, 서비스 디스커버리 등을 지원하여 복잡한 환경에서 컨테이너를 안정적으로 관리할 수 있게 한다.
 - 무중단 배포 설정
    - 무중단 배포는 서비스 중단 없이 새 버전을 배포할 수 있게 한다.
    - Docker Compose와 Nginx를 조합하여 Blue-Green 배포 또는 Rolling 배포 방식 등을 적용할 수 있다.

### 네트워크 설정

 - __브리지 네트워크 (bridge)__
    - __동일한 호스트 내에서 컨테이너 간의 통신__ 을 위한 기본 네트워크 유형입니다.
    - 서로 다른 네트워크에 있는 컨테이너끼리는 직접 통신할 수 없습니다.
 - __호스트 네트워크 (host)__
    - __컨테이너가 호스트의 네트워크를 공유__ 합니다.
    - 컨테이너가 물리적으로 호스트와 동일한 네트워크 환경에서 동작합니다.
 - __오버레이 네트워크 (overlay)__
    - __여러 Docker 데몬에서 실행 중인 컨테이너 간의 통신__ 을 위한 네트워크입니다.
    - 일반적으로 Docker Swarm이나 Kubernetes와 같은 오케스트레이션 시스템에서 사용됩니다.

#### 네트워크 관련 명령어

 - 네트워크 생성
    - frontend와 backend라는 이름의 두 개의 사용자 정의 네트워크를 각각 생성한다.
    - 기본적으로 브리지 네트워크로 생성된다.
```bash
docker network create frontend
docker network create backend
```

 - 네트워크 컨테이너 연결
    - 각 네트워크에 컨테이너를 연결하려면, 컨테이너를 생성할 때 --network 옵션을 사용한다.
```bash
# frontend 네트워크에 Nginx 컨테이너 연결
docker run -d --name my-nginx --network frontend nginx

# backend 네트워크에 MySQL 컨테이너 연결
docker run -d --name my-mysql --network backend -e MYSQL_ROOT_PASSWORD=example mysql
```

 - 컨테이너에 여러 네트워크 연결
    - 만약 하나의 컨테이너가 여러 네트워크에 연결되도록 하고 싶다면, docker network connect 명령을 사용할 수 있다.
```bash
# 먼저 기본 네트워크에 컨테이너 생성
docker run -d --name my-app --network frontend myapp:latest

# backend 네트워크에도 연결
docker network connect backend my-app
```

#### 기본 네트워크 사용

별도의 네트워크를 정의하지 않으면 자동으로 default 네트워크를 생성하고, 모든 컨테이너가 이 네트워크에서 서로 통신할 수 있다.  

 - web와 db는 자동으로 생성된 default 네트워크를 통해 통신할 수 있다.
 - web 컨테이너에서 db 라는 이름으로 데이터베이스에 접근할 수 있다.
```yml
version: '3'
services:
  web:
    image: nginx
    ports:
      - "8080:80"
  db:
    image: mysql
    environment:
      MYSQL_ROOT_PASSWORD: example
```

#### 사용자 정의 네트워크 사용

사용자 정의 네트워크를 생성하여 서비스 간의 네트워크를 분리하거나, 다른 네트워크 정책을 적용할 수 있다.  

 - web 서비스는 frontend 네트워크에서만 동작
 - app 서비스는 frontend, backend 네트워크에서 동작
 - app 서비스는 backend 네트워크에서 동작
```yml
version: '3'
services:
  web:
    image: nginx
    networks:
      - frontend
  app:
    image: myapp
    networks:
      - frontend
      - backend
  db:
    image: mysql
    networks:
      - backend
    environment:
      MYSQL_ROOT_PASSWORD: example

networks:
  frontend:
  backend:
```

#### 네트워크 고급 설정

 - subnet, gateway, ip-range 등을 설정할 수 있다.
```yml
networks:
  backend:
    driver: bridge
    ipam:
      config:
        - subnet: 172.16.238.0/24
          gateway: 172.16.238.1
```

### 볼륨 설정

볼륨은 컨테이너가 데이터를 저장하고 유지할 수 있는 공간을 제공한다. 볼륨을 사용하면 컨테이너가 삭제되거나 재시작되어도 데이터가 유지된다.  

 - __익명 볼륨__
    - Docker가 자동으로 생성하는 볼륨입니다.
    - 별도로 이름을 지정하지 않은 경우, 컨테이너가 종료될 때까지 데이터가 유지되지만 볼륨의 이름을 관리할 수는 없습니다.
 - __명명된 볼륨__
    - 명시적으로 이름을 지정한 볼륨으로, 여러 컨테이너 간에 공유 가능하며, Docker가 관리합니다.
 - __바인드 마운트 (Bind Mount)__
    - 호스트 파일 시스템의 특정 디렉토리를 컨테이너와 공유하는 방법입니다.
    - 개발 환경에서 소스 코드나 설정 파일을 컨테이너에서 사용할 수 있게 할 때 유용합니다.

#### 볼륨 관련 명령어

 - 익명 볼륨 사용
    - 익명 볼륨은 이름이 없는 자동으로 생성된 볼륨이다.
    - 컨테이너가 종료되거나 삭제되면 데이터가 유지되지만, 해당 볼륨을 참조할 수 있는 이름이 없다.
    - 이 볼륨을 다시 참조하려면 Docker가 생성한 내부 식별자를 찾아야 한다.
```bash
docker run -d --name my-container -v /var/lib/mysql mysql:5.7
```

 - 명명된 볼륨 사용
    - 명명된 볼륨은 특정 이름을 지정하여 생성한 볼륨이다.
    - 여러 컨테이너 간에 데이터를 공유하거나 영구적으로 유지해야 하는 데이터를 저장할 때 사용한다.
```bash
# my-volume 이라는 볼륨 생성
docker volume create my-volume

# 컨테이너 실행시 해당 볼륨을 사용
docker run -d --name my-container -v my-volume:/var/lib/mysql mysql:5.7

# 볼륨 확인
docker volume ls

# 볼륨 정보 확인
docker volume inspect my-volume
```

 - 바인드 마운트 사용
    - 바인드 마운트는 호스트의 디렉토리를 컨테이너의 디렉토리에 연결하는 방법이다.
    - 주로 개발 환경에서 코드 변경 사항을 즉시 반영하거나, 특정 호스트 파일 시스템을 컨테이너와 공유해야 할 때 사용한다.
```bash
docker run -d --name my-container -v /host/data:/container/data nginx

# 읽기 전용 바인드 마운트
docker run -d --name my-container -v /host/data:/container/data:ro nginx
```

#### 기본 볼륨 사용

 - db-data라는 명명된 볼륨을 생성하고, 이 볼륨을 db 컨테이너의 /var/lib/mysql 경로에 마운트한다.
```yml
version: '3'
services:
  db:
    image: mysql:5.7
    volumes:
      - db-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: example

volumes:
  db-data:
```

#### 바인드 마운트 사용

 - 호스트의 './nginx/html' 디렉토리를 Nginx 컨테이너의 '/usr/share/nginx/html' 디렉토리와 공유한다.
 - 이 방식은 호스트 파일 시스템의 특정 파일을 컨테이너 내부에서 직접 접근하거나 수정할 수 있게 해준다.
```yml
version: '3'
services:
  web:
    image: nginx
    volumes:
      - ./nginx/html:/usr/share/nginx/html
```

#### 볼륨 고급 설정
 
 - 볼륨도 고급 설정을 통해 읽기 전용으로 마운트하거나, 다양한 설정을 지정할 수 있다.
    - :ro 옵션은 읽기 전용으로 볼륨을 마운트하는 것이며, app 컨테이너에서는 데이터를 읽을 수만 있고, 수정할 수 없다.
    - :rw 옵션은 읽기/쓰기를 허용하는 옵션이다.
```yml
services:
  app:
    image: myapp
    volumes:
      - app-data:/var/lib/myapp:ro  # 읽기 전용
      - ./config:/app/config:rw     # 읽기/쓰기
volumes:
  app-data:
```

## Dockerfile

Dockerfile은 Docker 이미지를 생성하기 위한 텍스트 파일로, 일련의 명령어들을 통해 이미지가 어떻게 구성되고 빌드되는지를 정의한다. Dockerfile을 기반으로 빌드된 Docker 이미지는 컨테이너 실행 시 필요한 모든 요소(코드, 런타임, 라이브러리 등)를 포함하고 있어 일관된 환경을 제공해준다.  

### 주요 Dockerfile 명령어

 - FROM
    - Docker 이미지를 빌드할 때 사용할 베이스 이미지를 정의합니다.
    - 예시: FROM openjdk:11-jre
 - COPY
    - 로컬 파일이나 디렉토리를 컨테이너 안으로 복사합니다.
    - 예시: COPY ./myapp.jar /app/myapp.jar
 - RUN
    - 컨테이너 안에서 명령어를 실행하고 그 결과를 이미지에 포함시킵니다.
    - 예시: RUN apt-get update && apt-get install -y curl
 - WORKDIR
    - 이후 명령어들이 실행될 작업 디렉토리를 설정합니다.
    - 예시: WORKDIR /app
 - CMD
    - 컨테이너가 시작될 때 실행할 명령어를 지정합니다. 이 명령어는 컨테이너가 실행될 때마다 실행됩니다.
    - 예시: CMD ["java", "-jar", "/app/myapp.jar"]
    - CMD는 한 번만 사용할 수 있으며, ENTRYPOINT와 같이 쓰는 경우가 많습니다.
 - ENTRYPOINT
    - 컨테이너가 실행될 때 반드시 실행되는 기본 명령어를 설정합니다.
    - 예시: ENTRYPOINT ["java", "-jar"]
    - CMD와는 달리, ENTRYPOINT는 고정된 명령어로 컨테이너 실행 시 외부에서 추가 인자들을 넘길 수 있습니다.
 - EXPOSE
    - 컨테이너가 외부와 통신할 포트를 지정합니다.
    - 예시: EXPOSE 8080
 - ENV
    - 환경 변수를 설정합니다.
    - 예시: ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk
 - VOLUME
    - 컨테이너와 호스트 간의 디렉토리를 공유할 수 있도록 설정합니다.
    - 예시: VOLUME /data
```dockerfile
# OpenJDK 11 베이스 이미지를 사용하여,
# 로컬의 myapp.jar 파일을 /app 디렉토리에 복사하고,
# 해당 파일을 Java 명령어로 실행합니다.
# 8080 포트를 노출하여 외부에서 접근할 수 있게 설정합니다.

# 1. 베이스 이미지 설정
FROM openjdk:11-jre

# 2. 애플리케이션 JAR 파일을 복사
COPY ./myapp.jar /app/myapp.jar

# 3. 컨테이너가 실행될 때 사용할 작업 디렉토리 설정
WORKDIR /app

# 4. 컨테이너 실행 시 JAR 파일을 실행하도록 설정
CMD ["java", "-jar", "myapp.jar"]

# 5. 컨테이너에서 외부로 노출할 포트 설정
EXPOSE 8080
```

### 이미지 빌드 및 사용 관련 명령어

 - __docker build__
```bash
# -t, --tag: 이미지를 생성할 때 이름(tag)을 붙입니다. 이미지를 구분하기 위해 주로 사용합니다.
docker build -t myapp:1.0 .

# -f, --file: Dockerfile의 경로를 지정할 수 있습니다.
# 기본적으로는 현재 경로에 있는 Dockerfile을 사용하지만, 파일 이름이나 경로가 다를 경우 이 옵션을 사용합니다.
docker build -f ./Dockerfile.dev -t myapp:dev .

# --no-cache: 빌드할 때 캐시를 사용하지 않고 새로 빌드합니다. 
# 기본적으로 Docker는 이전 빌드의 캐시를 활용해 빠르게 이미지를 생성하지만,
# --no-cache 옵션을 사용하면 모든 단계를 새로 빌드합니다.
docker build --no-cache -t myapp:latest .

# --build-arg: Dockerfile에서 사용되는 빌드 인자를 전달할 때 사용합니다.
# 예를 들어, Dockerfile에 ARG 명령어로 빌드 시에 값을 받을 수 있는 변수를 선언한 경우, 이 옵션으로 값을 전달합니다.
docker build --build-arg APP_VERSION=1.0 -t myapp:1.0 .

# -q, --quiet: 빌드 로그를 출력하지 않고, 이미지만 생성합니다.
docker build -q -t myapp:1.0 .
```

 - __docker save__
    - Docker 이미지를 하나의 파일로 저장하는 명령어입니다.
    - 이 파일은 .tar 형식으로 저장되며, 다른 시스템에서 해당 파일을 로드하여 이미지를 사용할 수 있습니다.
    - 보통 이미지를 백업하거나 다른 환경으로 이미지를 이동할 때 사용됩니다.
```bash
docker save -o [파일명.tar] [이미지 이름:태그]
docker save -o myapp_image.tar myapp:1.0
```

 - __docker load__
    - docker save로 저장한 이미지 파일을 Docker로 다시 불러오는 명령어입니다.
    - 다른 시스템에서 이미지를 사용할 수 있도록 파일에서 이미지를 복원합니다.
```bash
docker load -i [파일명.tar]
docker load -i myapp_image.tar
```

 - __docker export__
    - 실행 중이거나 종료된 컨테이너의 파일 시스템을 하나의 아카이브 파일로 내보내는 명령어입니다.
    - 컨테이너 내부의 파일 시스템을 추출할 때 사용됩니다.
    - docker save와 달리, 컨테이너에 대한 메타데이터(예: 이미지 계층, 환경 변수 등)는 포함되지 않고 순수한 파일 시스템만 저장됩니다.
```bash
docker export -o [파일명.tar] [컨테이너 ID 또는 이름]
docker export -o mycontainer.tar mycontainer
```

 - __docker import__
    - docker export 명령어로 내보낸 파일 시스템을 Docker 이미지로 불러오는 명령어입니다.
    - 기본적으로 docker load가 이미지 자체를 복원하는 데 사용되지만, docker import는 파일 시스템을 이미지로 변환하는 용도로 사용됩니다.
```bash
docker import [파일명.tar] [새 이미지 이름:태그]
cat mycontainer.tar | docker import - mynewimage:1.0
```

 - __docker push__
    - 이미지를 Docker Hub 또는 프라이빗 레지스트리에 업로드하는 명령어입니다.
    - 이미지를 공유하거나 배포할 때 사용됩니다. 이미지를 업로드하기 전에 이미지에 태그를 지정해야 합니다.
```bash
docker push [이미지 이름:태그]
docker push myrepository/myapp:1.0
```

 - __docker pull__
    - 이미지를 Docker Hub 또는 프라이빗 레지스트리에서 로컬로 가져오는 명령어입니다.
```bash
docker pull [이미지 이름:태그]
docker pull myrepository/myapp:1.0
```

 - __docker tag__
    - Docker 이미지에 새 태그를 붙이는 명령어입니다.
    - 이 명령어는 이미지를 복제하지 않고 태그만 추가합니다.
```bash
docker tag [이미지 ID 또는 이름:태그] [새 이미지 이름:새 태그]
docker tag myapp:1.0 myrepository/myapp:latest
```

### 멀티 스테이지 빌드

Dockerfile에서 FROM을 여러 번 사용하는 것은 멀티 스테이지 빌드라는 개념입니다. 멀티 스테이지 빌드는 도커 이미지의 크기를 줄이거나 불필요한 파일과 의존성을 제거하는 데 유용합니다. 각 FROM은 새로운 빌드 단계의 시작을 나타내며, 이전 단계에서 생성한 파일을 다음 단계로 가져올 수 있습니다.  

 - __각 스테이지는 독립적__
    - FROM 키워드가 나올 때마다 새로운 단계가 시작됩니다.
    - 각 단계는 독립적인 컨테이너 환경에서 실행됩니다.
 - __파일을 선택적으로 복사 가능__
    - 첫 번째 단계에서 빌드된 결과물이나 특정 파일을 이후 단계로 가져와 사용할 수 있습니다.
    - 이 과정에서 불필요한 빌드 도구나 의존성을 포함하지 않아, 최종 이미지를 가볍게 유지할 수 있습니다.
 - __멀티 스테이지 빌드 장점__
    - __이미지 크기 절감__: 빌드 도구나 불필요한 파일이 최종 이미지에 포함되지 않기 때문에, 이미지 크기가 훨씬 작아집니다.
    - __보안 및 성능 향상__: 빌드 환경에서 사용했던 라이브러리나 도구들이 런타임 이미지에 포함되지 않아, 불필요한 보안 취약점이 줄어듭니다.
    - __단일 Dockerfile__: 하나의 Dockerfile에서 빌드 및 실행 환경을 모두 처리할 수 있어 관리가 쉬워집니다.

#### 단일 스테이지 빌드 예시(Spring 애플리케이션)
    - 프로젝트 빌드: mvn clean package
    - 이미지 빌드: docker build -t my-spring-boot-app .
    - 컨테이너 실행: docker run -p 8080:8080 my-spring-boot-app
```dockerfile
# 1. 베이스 이미지를 설정합니다. OpenJDK 17을 사용합니다.
FROM openjdk:17-jdk-alpine

# 2. 유지보수를 담당할 사람을 명시할 수 있습니다.
LABEL maintainer="your-email@example.com"

# 3. 환경 변수 설정 (optional). JAR 파일의 이름을 환경 변수로 지정할 수 있습니다.
ENV JAR_FILE=app.jar

# 4. 작업 디렉토리 설정. 애플리케이션을 실행할 디렉토리를 생성하고 설정합니다.
WORKDIR /app

# 5. 로컬에 있는 JAR 파일을 컨테이너의 작업 디렉토리로 복사합니다.
COPY ./target/${JAR_FILE} /app/${JAR_FILE}

# 6. 컨테이너가 실행될 때 JAR 파일을 실행하도록 설정합니다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# 7. 컨테이너에서 외부로 노출할 포트를 설정합니다.
EXPOSE 8080
```

#### 멀티 스테이지 빌드 예시 (Spring 애플리케이션)

 - 멀티 스테이지 빌드 흐름
    - 첫 번째 단계: 빌드 환경
        - 빌드와 관련된 모든 도구와 의존성을 포함합니다.
        - 애플리케이션 소스를 빌드하고, 생성된 결과물(JAR, 바이너리 등)을 사용합니다.
        - 이후 단계에서는 빌드 도구가 필요 없으므로 이 단계에서 필요한 결과물만 가져옵니다.
    - 두 번째 단계: 실행 환경
        - 첫 번째 단계에서 생성된 결과물만 포함하여 최종 실행 환경을 구성합니다.
        - 이 단계에서는 빌드 도구나 기타 개발 의존성은 제외하고, 최소한의 런타임 환경만 포함합니다.
```dockerfile
### 첫 번째 단계: 빌드 단계
FROM gradle:8.8.0-jdk21 AS build

# 소스 파일을 복사하고 빌드 실행
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootjar

### 두 번째 단계: 런타임 단계
FROM openjdk:21-jdk-slim

# 실행 환경을 위한 디렉터리 생성
RUN mkdir /app

# 첫 번째 단계에서 생성된 JAR 파일을 런타임 환경으로 복사
COPY --from=build /home/gradle/src/build/libs/ /app/

# 8080 포트 노출 및 애플리케이션 실행
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/example-0.0.1-SNAPSHOT.jar"]
```

#### 복잡한 멀티 스테이지 빌드 예시

복잡한 환경에서 사용하는 Dockerfile 예시는 다양한 기술 스택이나 환경 요구 사항을 다루는 경우를 포함할 수 있습니다. 예를 들어, 빌드 환경과 런타임 환경을 분리하고, 다양한 빌드 단계에서 의존성을 관리하며, 추가적인 테스트 단계나 캐시 최적화 등을 적용할 수 있습니다.  

 - Node.js 애플리케이션과 Python 서비스가 동시에 필요한 환경을 다루는 복잡한 Dockerfile
 - __1단계: Node.js 빌드 단계__
    - Node.js 버전 18 이미지를 사용하여 프론트엔드 애플리케이션을 빌드합니다.
    - WORKDIR을 /app으로 설정하고, package.json과 package-lock.json 파일을 복사한 후 npm install로 필요한 종속성을 설치합니다.
    - 이후, 소스 코드를 복사하고 npm run build 명령어로 애플리케이션을 빌드합니다. 이 단계에서는 React, Vue, Angular 등 프론트엔드 애플리케이션을 빌드할 수 있습니다.
 - __2단계: Python 빌드 단계__
    - Python 버전 3.11의 경량 이미지(slim)를 사용하여 Python 환경을 설정합니다.
    - requirements.txt 파일을 복사한 후, 필요한 Python 패키지를 설치합니다.
    - 이 단계는 Python 백엔드 애플리케이션을 준비하고, 설치된 Python 패키지와 소스 코드를 /app 디렉토리에 준비합니다.
 - __3단계: 통합 실행 환경__
    - 최종 실행 환경으로 경량 Debian 이미지를 사용합니다.
    - 앞서 빌드된 Node.js와 Python 애플리케이션을 복사해 옵니다.
        - COPY --from=node_build /app/build /var/www/app: Node.js에서 빌드된 결과물을 Nginx로 제공할 디렉터리(/var/www/app)로 복사합니다.
        - COPY --from=python_build /app /app: Python 애플리케이션을 /app 디렉터리로 복사합니다.
```dockerfile
### 1단계: Node.js 빌드 단계
FROM node:18 AS node_build

# 소스 파일 복사 및 종속성 설치
WORKDIR /app
COPY package*.json ./
RUN npm install

# 애플리케이션 빌드 (예: React/Vue 같은 프론트엔드 빌드)
COPY . .
RUN npm run build

### 2단계: Python 빌드 단계
FROM python:3.11-slim AS python_build

# 필요한 패키지 설치
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

### 3단계: 통합 실행 환경
FROM debian:buster-slim

# Node.js 실행 환경 설정
COPY --from=node_build /app/build /var/www/app

# Python 실행 환경 설정
COPY --from=python_build /app /app

# 추가 시스템 패키지 설치 (예: Nginx 설치)
RUN apt-get update && \
    apt-get install -y nginx && \
    rm -rf /var/lib/apt/lists/*

# Nginx 설정 복사
COPY ./nginx/nginx.conf /etc/nginx/nginx.conf

# Python 애플리케이션 실행을 위한 스크립트 복사
COPY ./scripts/start.sh /start.sh
RUN chmod +x /start.sh

# 포트 노출 및 애플리케이션 실행
EXPOSE 80
CMD ["/start.sh"]
```

## Docker Compose

Docker Compose는 다중 컨테이너 애플리케이션을 정의하고 실행하는 도구입니다. 이를 사용하면 여러 Docker 컨테이너를 손쉽게 관리하고, 함께 동작하도록 구성할 수 있습니다. Compose는 YAML 파일(docker-compose.yml)을 통해 애플리케이션의 서비스를 정의하며, 이 파일에서 각 서비스의 이미지, 네트워크, 볼륨, 환경 변수 등을 설정할 수 있습니다.  

 - __서비스(Service)__
    - 애플리케이션을 구성하는 컨테이너의 정의입니다.
    - 예를 들어, 웹 애플리케이션, 데이터베이스 등이 각각 하나의 서비스로 정의됩니다.
 - __네트워크(Network)__
    - 각 서비스 간의 통신을 담당합니다.
    - Docker Compose는 각 서비스가 서로 통신할 수 있도록 기본 네트워크를 자동으로 설정합니다.
 - __볼륨(Volume)__
    - 컨테이너가 사용하는 데이터가 저장되는 공간을 정의합니다.
    - 데이터를 영속적으로 저장하기 위해 볼륨을 사용합니다.

### 주요 명령어

 - __docker-compose up__: 정의된 서비스들을 실행하고, 각 컨테이너를 연결된 상태로 시작합니다.
 - __docker-compose down__: 모든 서비스를 중지하고, 실행 중인 컨테이너들을 제거합니다.
 - __docker-compose logs__: 서비스들의 로그를 확인할 수 있습니다.
 - __docker-compose exec__: 실행 중인 컨테이너에서 명령을 실행할 수 있습니다.
```bash
## docker-compose up
# docker-compose.yml 파일에 정의된 모든 컨테이너 생성 및 실행
docker-compose up
docker-compose up -d # 백그라운드로 실행

## docker-compose down
# 실행 중인 모든 컨테이너를 중지하고, 네트워크 및 볼륨 등 관련 리소스 제거
docker-compose down
docker-compose down --volumes # 생성된 볼륨까지 모두 삭제

## docker-compose logs
# 현재 실행 중인 컨테이너들의 로그를 출력
docker-compose logs
docker-compose logs -f # 실시간으로 로그를 스트리밍 방식으로 확인

## docker-compose exec
# 실행 중인 컨테이너 안에서 명령어 실행
docker-compose exec web /bin/bash

## docker-compose ps
# 현재 실행 중인 컨테이너 목록과 상태 출력
docker-compose ps

## docker-compose build
# docker-compose.yml에 정의된 서비스들을 다시 빌드
# 주로 이미지 변경 사항이 있을 때 사용
docker-compose build
docker-compose build --no-cache # 캐시 사용하지 않고 이미지 빌드

## docker-compose stop / start / restart
# 특정 컨테이너를 중지하거나 중지된 컨테이너 실행
docker-compose stop
docker-compose start
docker-compose restart
```

### Docker Compose를 이용한 blue/green 배포

#### green.yml 파일과 blue.yml 파일 분리

 - docker-compose 파일
    - green.yml과 blue.yml은 포트만 다를 뿐 컨테이너 정보는 모두 동일하다. 
    - 이미지의 태그의 경우는 환경 변수를 통해서 도커 이미지 버전을 다르게 사용한다.  
    - 이미지 태그의 경우 젠킨스, 깃헙 액션 등 파이프라인 안에서 배포 시에 변경해준다.  
```yml
# docker-compose.green.yml
version: '3.1'
 
services:
  api:
    image: ${DOCKER_REGISTRY}/${DOCKER_APP_NAME}:${IMAGE_TAG}
    container_name: ${DOCKER_APP_NAME}-green
    environment:
      - LANG=ko_KR.UTF-8
      - UWSGI_PORT=8000
    ports:
      - '8080:8000'

# docker-compose.blue.yml
version: '3.1'
 
services:
  api:
    image: ${DOCKER_REGISTRY}/${DOCKER_APP_NAME}:${IMAGE_TAG}
    container_name: ${DOCKER_APP_NAME}-blue
    environment:
      - LANG=ko_KR.UTF-8
      - UWSGI_PORT=8000
    ports:
      - '8081:8000'
```

 - Nginx 설정 파일
    - green.conf과 blue.conf는 업스트림 서버의 포트만 다를 뿐 그 외 모든 설정은 동일하다. 
```conf
http {
    upstream backend {
        server localhost:8081; # green
    }
 
    access_log /var/log/nginx/access.log;
 
    server {
        listen 80;
 
        location / {
            proxy_pass          http://backend;
            proxy_http_version  1.1;
            proxy_set_header    Connection          $connection_upgrade;
            proxy_set_header    Upgrade             $http_upgrade;

            proxy_set_header    Host                $host;
            proxy_set_header    X-Real-IP           $remote_addr;
            proxy_set_header    X-Forwarded-For     $proxy_add_x_forwarded_for;
        }
 
        access_log    /var/log/nginx/access.log main;

        client_header_timeout 60;
        client_body_timeout   60;
        keepalive_timeout     60;
        gzip                  off;
        gzip_comp_level       4;
    }
}
```

 - deploy.sh
    - 배포를 위한 스크립트를 작성한다.
    - __환경 변수 확인__: DOCKER_APP_NAME 변수가 설정되지 않았을 경우 배포를 중지하고 에러 메시지를 출력합니다.
    - __컨테이너 상태 확인__: check_container_up 함수는 지정된 색상의 컨테이너가 제대로 시작되었는지 확인합니다. 실패 시에는 최대 5번의 재시도를 합니다.
    - __스위칭 로직__: blue 컨테이너가 실행 중이지 않으면 blue 컨테이너를, 그렇지 않으면 green 컨테이너를 실행합니다.
    - __컨테이너 상태 확인 후 다운__: 새 컨테이너가 정상적으로 올라온 경우에만 이전 컨테이너를 중지합니다. 만약 새 컨테이너가 제대로 올라오지 않으면 롤백 처리하여 이전 컨테이너가 계속 실행되도록 합니다.
```sh
#!/bin/bash

# 환경 변수 설정 확인
if [ -z "$DOCKER_APP_NAME" ]; then
  echo "DOCKER_APP_NAME 변수를 설정해주세요."
  exit 1
fi

# 컨테이너가 시작되었는지 확인하는 함수
function check_container_up() {
  local compose_color=$1
  local retry_count=5
  local attempt=1
  local container_up=""

  while [ $attempt -le $retry_count ]; do
    echo "Checking if ${compose_color} containers are up (attempt $attempt/$retry_count)..."
    container_up=$(docker-compose -p ${DOCKER_APP_NAME}-${compose_color} -f docker-compose.${compose_color}.yaml ps | grep "Up")
    
    if [ -n "$container_up" ]; then
      echo "${compose_color} containers are up."
      return 0
    fi

    echo "${compose_color} containers are not up yet. Retrying in 5 seconds..."
    sleep 5
    attempt=$((attempt + 1))
  done

  echo "Failed to start ${compose_color} containers."
  return 1
}

# 현재 떠 있는 컨테이너를 확인하여 blue/green 스위칭 결정
EXIST_BLUE=$(docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yaml ps | grep "Up")

if [ -z "$EXIST_BLUE" ]; then
  echo "Starting blue containers..."
  docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yaml up -d
  BEFORE_COMPOSE_COLOR="green"
  AFTER_COMPOSE_COLOR="blue"
else
  echo "Starting green containers..."
  docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yaml up -d
  BEFORE_COMPOSE_COLOR="blue"
  AFTER_COMPOSE_COLOR="green"
fi

# 새 컨테이너(AFTER_COMPOSE_COLOR)가 실행되었는지 확인
if check_container_up $AFTER_COMPOSE_COLOR; then
  echo "New ${AFTER_COMPOSE_COLOR} containers are running successfully."

  # Nginx 설정을 새로운 컨테이너에 맞게 변경해주고 reload
  cp /etc/nginx/nginx.${AFTER_COMPOSE_COLOR}.conf /etc/nginx/nginx.conf
  nginx -s reload

  # 이전 컨테이너 중지
  echo "Stopping ${BEFORE_COMPOSE_COLOR} containers..."
  docker-compose -p ${DOCKER_APP_NAME}-${BEFORE_COMPOSE_COLOR} -f docker-compose.${BEFORE_COMPOSE_COLOR}.yaml down
else
  echo "Failed to start ${AFTER_COMPOSE_COLOR} containers. Keeping ${BEFORE_COMPOSE_COLOR} containers running."
  exit 1
fi

echo "Blue/Green deployment successful! ${AFTER_COMPOSE_COLOR} is now live."
```
