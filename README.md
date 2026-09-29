# PlanDoSee Note

계획(Plan) → 실행(Do) → 회고(See)가 실제 기록으로 이어지는 개인 계획·실행·회고 웹 애플리케이션입니다.

> 지금은 로그인이 없어 링크를 아는 사람은 누구나 볼 수 있습니다. 남이 봐도 괜찮은 내용만 넣으세요

## 주요 기능

- **계획**: 기간·우선순위·성공 기준·예상 시간을 적고, 수정하면 이전 값이 이력으로 남습니다.
- **할 일**: 계획에 딸린 할 일을 추가·수정·삭제하고 완료·되돌리기를 합니다. 태그, 제목 검색, 상태·우선순위·태그·마감 상태 필터, 정렬을 지원합니다.
- **실행 기록**: 실제로 한 시간을 계획과 따로 기록합니다. 완료 요청이 여러 번 와도 한 번만 반영됩니다.
- **회고**: 예상과 실제의 차이를 숫자로 보고, 그 숫자를 만든 기록까지 내려가 확인합니다.
- **다음 계획**: 회고에서 고른 개선점 한 가지를 다음 계획으로 넘깁니다.
- **내보내기**: 전체 자료를 JSON 파일 하나로 내려받습니다.

시간은 일·시간·분으로 입력하고 표시하며, 날짜와 "오늘" 판정은 서울(Asia/Seoul) 기준입니다.

## 기술 스택

Java 25, Spring Boot 4.1 (Spring MVC, Validation, Thymeleaf), MyBatis(XML Mapper), PostgreSQL, Flyway, Maven Wrapper.

## 로컬 실행

사전 조건: JDK 25, 접속할 수 있는 PostgreSQL(15 이상).

1. `.env.example`을 `.env`로 복사하고 DB 접속값을 채웁니다. `.env`는 Git에 올리지 않습니다.

   | 이름 | 용도 |
   | --- | --- |
   | `DB_URL` | JDBC URL. 예: `jdbc:postgresql://localhost:5432/plandosee` |
   | `DB_USERNAME` | DB 사용자 |
   | `DB_PASSWORD` | DB 비밀번호 |
   | `PORT` | HTTP 포트, 기본 `8080` |

2. 실행합니다. 처음 시작할 때 Flyway가 테이블을 만듭니다.

   ```bash
   set -a; source .env; set +a
   ./mvnw spring-boot:run
   ```

3. 브라우저에서 `http://localhost:8080/`을 엽니다.
