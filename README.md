# ADB Quest - Android ADB Wireless Connection Manager

Oculus Quest와 Android 기기 간 무선 ADB 연결을 관리하는 Android 앱입니다.

## 🚀 주요 기능

### 1. 자동 네트워크 스캔
- WiFi 네트워크 내 ADB 활성화된 기기 자동 검색
- IP 주소, 응답 시간, 호스트명 표시
- 192.168.x.1-255 범위 스캔 (포트 5555)

### 2. ADB 연결 상태 확인
- 실제 ADB 프로토콜을 사용한 연결 테스트
- `host:version` 명령으로 handshake 시도
- 연결 가능 여부 실시간 확인

### 3. TCPIP 5555 자동 활성화
- ADB 연결 성공 시 자동으로 `tcpip:5555` 명령 전송
- Quest를 무선 ADB 모드로 자동 전환
- 수동 터미널 작업 없이 원클릭 설정

### 4. 직관적인 UI
- 간단한 IP 입력 또는 자동 스캔 선택
- 실시간 연결 상태 표시
- 명확한 성공/실패 메시지

## 📱 사용 방법

### 1. 권한 허용
앱 실행 시 위치 권한을 허용해주세요 (WiFi 네트워크 스캔용).

### 2. Quest 연결 방법

#### 방법 A: 자동 스캔
1. "네트워크 스캔" 버튼 클릭
2. 검색된 기기 목록에서 Quest 선택
3. "Connect" 버튼 클릭

#### 방법 B: 수동 입력
1. Quest IP 주소 직접 입력
2. "Connect" 버튼 클릭

### 3. 연결 결과 확인
- ✅ **성공**: "ADB Connection Successful!" + "TCPIP 5555 command sent successfully!"
- ⚠️ **인증 필요**: "Service active (authentication may be required)"
- ❌ **실패**: 구체적인 오류 메시지 표시

## 🔧 기술 사양

### 개발 환경
- **언어**: Kotlin
- **플랫폼**: Android
- **최소 SDK**: API 21 (Android 5.0)
- **타겟 SDK**: API 34 (Android 14)

### 권한 요구사항
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

#### 🔐 권한이 필요한 이유

**📱 설치 시 자동 허용되는 권한:**
- **INTERNET**: ADB 소켓 연결 및 네트워크 통신용
- **ACCESS_WIFI_STATE**: WiFi 연결 상태 확인 및 현재 IP 주소 획득용

**⚠️ 사용자 승인이 필요한 권한:**
- **ACCESS_COARSE_LOCATION** / **ACCESS_FINE_LOCATION**: 위치 권한

**🤔 왜 위치 권한이 필요한가요?**

앱은 실제로 GPS 위치를 사용하지 않습니다. 하지만 Android 6.0 이후부터 보안상의 이유로 WiFi 정보 접근 시 위치 권한이 필수가 되었습니다.

**실제 동작 방식:**
1. **현재 기기 IP 확인**: `WifiManager.getConnectionInfo().ipAddress` 호출
2. **서브넷 추출**: 예) `192.168.1.100` → `192.168.1` 
3. **동일 서브넷 스캔**: `192.168.1.1` ~ `192.168.1.255` 범위에서 포트 5555 연결 시도
4. **ADB 기기 검색**: 연결 성공한 IP를 ADB 활성화된 기기로 판단

**위치 권한 없이는:**
- `WifiManager`가 실제 IP 대신 `0.0.0.0` 반환
- 네트워크 스캔 기능 사용 불가
- 수동 IP 입력은 여전히 가능

**📍 개인정보 보호:**
- 앱은 실제 위치 데이터를 수집하지 않음
- 오직 현재 네트워크의 IP 주소 정보만 사용
- 네트워크 스캔은 로컬 서브넷에만 제한됨

### 핵심 구현
- **ADB 프로토콜**: 실제 ADB handshake 구현
- **네트워크 스캔**: Coroutines 기반 비동기 스캔
- **소켓 통신**: 타임아웃 처리가 포함된 안정적인 연결

## 📋 연결 상태 메시지

| 메시지 | 의미 | 다음 단계 |
|--------|------|-----------|
| ✅ ADB Connection Successful! | 완전한 연결 성공 | 터미널에서 `adb connect` 가능 |
| ✅ TCPIP 5555 command sent successfully! | 무선 모드 활성화 완료 | Quest 무선 연결 준비됨 |
| ⚠️ Service active (authentication may be required) | 포트 열림, 인증 필요 | PC에서 한 번 `adb connect` 실행 |
| ❌ Connection timeout | ADB 비활성화 | Quest 개발자 모드 확인 |
| ❌ Port closed | ADB 서비스 중단 | Quest USB 디버깅 활성화 |

## 🎯 사용 시나리오

### Quest 초기 설정
1. Quest를 USB로 PC에 연결
2. 앱에서 Quest IP로 연결 시도
3. 자동으로 무선 ADB 모드 활성화
4. USB 해제 후 무선 연결 사용

### 일상적인 사용
1. 앱에서 네트워크 스캔
2. Quest 발견 시 원클릭 연결
3. 터미널에서 `adb connect [IP]:5555` 실행

## 🔍 문제 해결

### Quest가 검색되지 않을 때
- Quest 개발자 모드 활성화 확인
- USB 디버깅 설정 확인
- 같은 WiFi 네트워크 연결 확인

### 연결은 되지만 인증 오류 시
- PC에서 `adb connect [Quest IP]:5555` 한 번 실행
- Quest에서 인증 대화상자 승인
- 이후 앱에서 정상 연결 가능

## 🚀 빌드 방법

```bash
# 프로젝트 클론
git clone [repository-url]
cd ladb

# 빌드
gradle build

# 설치 (Android 기기 연결 후)
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 🤝 기여

버그 리포트, 기능 요청, Pull Request는 언제든 환영합니다!

---

**개발자**: ADB Quest Team  
**버전**: 1.0.0  
**최종 업데이트**: 2024 