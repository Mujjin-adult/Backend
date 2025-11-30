#!/usr/bin/env python3
"""
Firebase Authentication API 테스트 스크립트

이 스크립트는 Firebase Admin SDK를 사용하여:
1. 테스트 사용자 생성 (Firebase Authentication)
2. Custom Token 생성
3. 서버 API 테스트
"""

import json
import requests
import firebase_admin
from firebase_admin import credentials, auth
import sys

# Firebase Admin SDK 초기화
def initialize_firebase():
    """Firebase Admin SDK 초기화"""
    try:
        cred = credentials.Certificate('./firebase-credentials.json')
        firebase_admin.initialize_app(cred)
        print("✅ Firebase Admin SDK 초기화 성공")
        return True
    except Exception as e:
        print(f"❌ Firebase Admin SDK 초기화 실패: {e}")
        return False

# 테스트 사용자 생성 또는 가져오기
def get_or_create_test_user(email, password, display_name):
    """테스트 사용자 생성 또는 기존 사용자 가져오기"""
    try:
        # 기존 사용자 확인
        user = auth.get_user_by_email(email)
        print(f"✅ 기존 사용자 찾음: {email}")
        return user
    except auth.UserNotFoundError:
        # 새 사용자 생성
        try:
            user = auth.create_user(
                email=email,
                password=password,
                display_name=display_name,
                email_verified=True  # 테스트를 위해 이메일 인증 완료로 설정
            )
            print(f"✅ 새로운 사용자 생성: {email}")
            return user
        except Exception as e:
            print(f"❌ 사용자 생성 실패: {e}")
            return None
    except Exception as e:
        print(f"❌ 사용자 조회 실패: {e}")
        return None

# Custom Token 생성
def create_custom_token(uid):
    """Firebase Custom Token 생성"""
    try:
        custom_token = auth.create_custom_token(uid)
        print(f"✅ Custom Token 생성 성공")
        return custom_token.decode('utf-8')
    except Exception as e:
        print(f"❌ Custom Token 생성 실패: {e}")
        return None

# ID Token 발급 (실제 클라이언트 대신 REST API 사용)
def sign_in_with_custom_token(custom_token, api_key):
    """Custom Token으로 ID Token 발급"""
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key={api_key}"

    payload = {
        "token": custom_token,
        "returnSecureToken": True
    }

    try:
        response = requests.post(url, json=payload)
        response.raise_for_status()
        data = response.json()
        print(f"✅ ID Token 발급 성공")
        return data['idToken']
    except Exception as e:
        print(f"❌ ID Token 발급 실패: {e}")
        if hasattr(e, 'response') and e.response:
            print(f"   응답: {e.response.text}")
        return None

# 서버 로그인 API 테스트
def test_server_login_api(id_token, server_url="http://localhost:8080"):
    """서버의 Firebase 로그인 API 테스트"""
    url = f"{server_url}/api/auth/login"

    payload = {
        "idToken": id_token,
        "fcmToken": "test-fcm-token-12345"
    }

    headers = {
        "Content-Type": "application/json"
    }

    try:
        print(f"\n📤 서버 로그인 API 호출: {url}")
        response = requests.post(url, json=payload, headers=headers)

        print(f"   상태 코드: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"✅ 로그인 성공!")
            print(f"   응답: {json.dumps(data, indent=2, ensure_ascii=False)}")
            return data
        else:
            print(f"❌ 로그인 실패")
            print(f"   응답: {response.text}")
            return None
    except Exception as e:
        print(f"❌ API 호출 실패: {e}")
        return None

# 인증이 필요한 API 테스트
def test_authenticated_api(id_token, server_url="http://localhost:8080"):
    """인증이 필요한 API 테스트 (북마크 조회)"""
    url = f"{server_url}/api/bookmarks"

    headers = {
        "Authorization": f"Bearer {id_token}",
        "Content-Type": "application/json"
    }

    try:
        print(f"\n📤 인증 API 호출: {url}")
        response = requests.get(url, headers=headers)

        print(f"   상태 코드: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"✅ 인증 성공!")
            print(f"   응답: {json.dumps(data, indent=2, ensure_ascii=False)}")
            return data
        else:
            print(f"⚠️  응답 상태: {response.status_code}")
            print(f"   응답: {response.text}")
            return None
    except Exception as e:
        print(f"❌ API 호출 실패: {e}")
        return None

def main():
    """메인 테스트 함수"""
    print("=" * 60)
    print("Firebase Authentication API 테스트")
    print("=" * 60)

    # 테스트 사용자 정보
    test_email = "firebasetest@inu.ac.kr"
    test_password = "testpassword123"
    test_name = "Firebase테스트"

    # Firebase Web API Key (firebase-credentials.json에서 프로젝트 ID 확인 필요)
    # 실제로는 Firebase Console에서 가져와야 함
    print("\n⚠️  주의: Firebase Web API Key가 필요합니다.")
    print("Firebase Console → 프로젝트 설정 → 일반 → 웹 API 키")
    api_key = input("\nFirebase Web API Key를 입력하세요 (Enter를 누르면 Custom Token 테스트만 실행): ").strip()

    # 1. Firebase Admin SDK 초기화
    if not initialize_firebase():
        print("\n❌ Firebase 초기화 실패. 종료합니다.")
        sys.exit(1)

    # 2. 테스트 사용자 생성/가져오기
    print(f"\n📝 테스트 사용자: {test_email}")
    user = get_or_create_test_user(test_email, test_password, test_name)
    if not user:
        print("\n❌ 사용자 생성/조회 실패. 종료합니다.")
        sys.exit(1)

    print(f"   UID: {user.uid}")
    print(f"   이름: {user.display_name}")
    print(f"   이메일 인증: {user.email_verified}")

    # 3. Custom Token 생성
    print(f"\n🔐 Custom Token 생성 중...")
    custom_token = create_custom_token(user.uid)
    if not custom_token:
        print("\n❌ Custom Token 생성 실패. 종료합니다.")
        sys.exit(1)

    print(f"   Custom Token (앞 50자): {custom_token[:50]}...")

    # 4. ID Token 발급
    id_token = None
    if api_key:
        print(f"\n🔑 ID Token 발급 중...")
        id_token = sign_in_with_custom_token(custom_token, api_key)

        if id_token:
            print(f"   ID Token (앞 50자): {id_token[:50]}...")
        else:
            print("\n⚠️  ID Token 발급 실패. Custom Token 테스트만 계속합니다.")
    else:
        print("\n⚠️  Web API Key가 없어 ID Token 발급을 건너뜁니다.")
        print("   실제 클라이언트에서는 Firebase SDK로 로그인 후 getIdToken()을 사용하세요.")

    # 5. 서버 로그인 API 테스트
    if id_token:
        print(f"\n{'=' * 60}")
        print("서버 API 테스트")
        print("=" * 60)

        login_response = test_server_login_api(id_token)

        if login_response and login_response.get('success'):
            # 6. 인증이 필요한 API 테스트
            print(f"\n{'=' * 60}")
            print("인증 필요 API 테스트")
            print("=" * 60)
            test_authenticated_api(id_token)

    print(f"\n{'=' * 60}")
    print("테스트 완료")
    print("=" * 60)

    # 테스트 사용자 정보 출력
    print(f"\n📋 테스트 요약:")
    print(f"   ✅ Firebase Admin SDK 초기화 성공")
    print(f"   ✅ 테스트 사용자: {test_email}")
    print(f"   ✅ Custom Token 생성 성공")
    if id_token:
        print(f"   ✅ ID Token 발급 성공")
        print(f"   ✅ 서버 API 테스트 완료")
    else:
        print(f"   ⚠️  ID Token 발급 없음 (Web API Key 필요)")

    print(f"\n💡 다음 단계:")
    if not id_token:
        print("   1. Firebase Console에서 Web API Key 확인")
        print("   2. 다시 이 스크립트 실행")
        print("   3. 또는 클라이언트에서 Firebase SDK로 직접 테스트")
    else:
        print("   1. 클라이언트 앱에서 Firebase SDK 통합")
        print("   2. signInWithEmailAndPassword()로 로그인")
        print("   3. getIdToken()으로 ID Token 발급")
        print("   4. 서버 /api/auth/login에 ID Token 전송")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  사용자에 의해 중단되었습니다.")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ 예상치 못한 오류: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
