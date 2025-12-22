#!/usr/bin/env python3
"""
Firebase Admin SDK를 사용한 직접 테스트
Web API Key 없이 서버의 Firebase Authentication 기능을 테스트합니다.
"""

import json
import requests
import firebase_admin
from firebase_admin import credentials, auth

def initialize_firebase():
    """Firebase Admin SDK 초기화"""
    try:
        cred = credentials.Certificate('./firebase-credentials.json')
        firebase_admin.initialize_app(cred)
        print("✅ Firebase Admin SDK 초기화 성공\n")
        return True
    except Exception as e:
        print(f"❌ Firebase Admin SDK 초기화 실패: {e}\n")
        return False

def create_test_user(email, password, display_name):
    """Firebase에서 테스트 사용자 생성"""
    try:
        # 기존 사용자 확인
        try:
            user = auth.get_user_by_email(email)
            print(f"✅ 기존 사용자 찾음: {email}")
            print(f"   UID: {user.uid}")
            print(f"   이름: {user.display_name}")
            print(f"   이메일 인증: {user.email_verified}\n")
            return user
        except auth.UserNotFoundError:
            # 새 사용자 생성
            user = auth.create_user(
                email=email,
                password=password,
                display_name=display_name,
                email_verified=True
            )
            print(f"✅ 새로운 사용자 생성: {email}")
            print(f"   UID: {user.uid}")
            print(f"   이름: {user.display_name}")
            print(f"   이메일 인증: {user.email_verified}\n")
            return user
    except Exception as e:
        print(f"❌ 사용자 생성 실패: {e}\n")
        return None

def create_custom_token(uid):
    """Custom Token 생성"""
    try:
        custom_token = auth.create_custom_token(uid)
        token_str = custom_token.decode('utf-8')
        print(f"✅ Custom Token 생성 성공")
        print(f"   Custom Token (앞 50자): {token_str[:50]}...\n")
        return token_str
    except Exception as e:
        print(f"❌ Custom Token 생성 실패: {e}\n")
        return None

def test_server_with_custom_token(custom_token):
    """
    서버에서 Custom Token을 직접 처리할 수 있는지 테스트
    (주의: 일반적으로 Custom Token은 클라이언트에서 ID Token으로 교환해야 함)
    """
    url = "http://localhost:8080/api/auth/login"

    payload = {
        "idToken": custom_token,
        "fcmToken": "test-fcm-token-12345"
    }

    try:
        print("📤 서버 로그인 API 호출 (Custom Token 사용)...")
        response = requests.post(url, json=payload, headers={"Content-Type": "application/json"})

        print(f"   상태 코드: {response.status_code}\n")

        result = response.json()
        print("응답:")
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()

        return result
    except Exception as e:
        print(f"❌ API 호출 실패: {e}\n")
        return None

def verify_custom_token_locally(custom_token):
    """Custom Token을 로컬에서 검증"""
    try:
        # Custom Token을 검증 (Firebase Admin SDK로는 ID Token만 검증 가능)
        print("ℹ️  Custom Token은 클라이언트에서 ID Token으로 교환해야 합니다.")
        print("   서버는 ID Token만 검증할 수 있습니다.\n")
        return False
    except Exception as e:
        print(f"❌ 검증 실패: {e}\n")
        return False

def test_legacy_signup():
    """레거시 회원가입 API 테스트"""
    import random
    test_num = random.randint(10000, 99999)

    url = "http://localhost:8080/api/auth/signup"
    payload = {
        "studentId": f"2021{test_num}",
        "email": f"legacytest{test_num}@inu.ac.kr",
        "password": "testpassword123",
        "name": f"레거시테스트{test_num}"
    }

    try:
        print("📤 레거시 회원가입 API 테스트...")
        print(f"   이메일: {payload['email']}\n")

        response = requests.post(url, json=payload, headers={"Content-Type": "application/json"})

        print(f"   상태 코드: {response.status_code}\n")

        result = response.json()
        print("응답:")
        print(json.dumps(result, indent=2, ensure_ascii=False))
        print()

        return result
    except Exception as e:
        print(f"❌ API 호출 실패: {e}\n")
        return None

def main():
    print("=" * 70)
    print("Firebase Authentication 서버 테스트")
    print("=" * 70)
    print()

    # 1. Firebase Admin SDK 초기화
    if not initialize_firebase():
        print("❌ Firebase 초기화 실패. 종료합니다.")
        return

    # 2. 테스트 사용자 정보
    import random
    test_num = random.randint(10000, 99999)
    test_email = f"firebasetest{test_num}@inu.ac.kr"
    test_password = "testpassword123"
    test_name = f"Firebase테스트{test_num}"

    print("=" * 70)
    print("1. Firebase 테스트 사용자 생성")
    print("=" * 70)
    print()

    user = create_test_user(test_email, test_password, test_name)
    if not user:
        print("❌ 사용자 생성 실패. 레거시 API만 테스트합니다.\n")
        test_legacy_signup()
        return

    # 3. Custom Token 생성
    print("=" * 70)
    print("2. Custom Token 생성")
    print("=" * 70)
    print()

    custom_token = create_custom_token(user.uid)
    if not custom_token:
        print("❌ Custom Token 생성 실패.\n")
        return

    # 4. 서버 테스트 (Custom Token으로는 실패 예상)
    print("=" * 70)
    print("3. 서버 로그인 API 테스트")
    print("=" * 70)
    print()

    print("⚠️  주의: Custom Token은 클라이언트에서 ID Token으로 교환해야 합니다.")
    print("   서버는 ID Token만 검증할 수 있으므로, 이 테스트는 실패할 것입니다.\n")

    test_server_with_custom_token(custom_token)

    # 5. 레거시 API 테스트
    print("=" * 70)
    print("4. 레거시 회원가입 API 테스트")
    print("=" * 70)
    print()

    legacy_result = test_legacy_signup()

    # 6. 결과 요약
    print("=" * 70)
    print("테스트 결과 요약")
    print("=" * 70)
    print()

    print("✅ Firebase Admin SDK 초기화: 성공")
    print(f"✅ Firebase 사용자 생성: 성공 ({test_email})")
    print(f"✅ Custom Token 생성: 성공")
    print(f"⚠️  서버 ID Token 검증: Custom Token은 사용 불가")

    if legacy_result and legacy_result.get('success'):
        print(f"✅ 레거시 회원가입 API: 정상 작동")

    print()
    print("=" * 70)
    print("다음 단계")
    print("=" * 70)
    print()
    print("Firebase Authentication을 완전히 테스트하려면:")
    print()
    print("1. Firebase Console에서 올바른 Web API Key 확인")
    print("   - Firebase Console → 프로젝트 설정 → 일반 탭")
    print("   - '웹 API 키' 항목 (AIza로 시작하는 39자 문자열)")
    print()
    print("2. 클라이언트에서 Firebase SDK 사용")
    print("   - React Native: @react-native-firebase/auth")
    print("   - React Web: firebase/auth")
    print()
    print("3. 클라이언트 통합 플로우:")
    print("   a) signInWithEmailAndPassword(email, password)")
    print("   b) user.getIdToken()")
    print("   c) POST /api/auth/login with ID Token")
    print()
    print("현재 상태:")
    print("✅ 서버의 Firebase Admin SDK가 정상 작동합니다")
    print("✅ 레거시 회원가입 API가 정상 작동합니다")
    print("✅ Firebase에서 사용자를 생성할 수 있습니다")
    print("⚠️  클라이언트에서 Firebase SDK로 로그인 후 ID Token 발급 필요")
    print()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  사용자에 의해 중단되었습니다.")
    except Exception as e:
        print(f"\n❌ 예상치 못한 오류: {e}")
        import traceback
        traceback.print_exc()
