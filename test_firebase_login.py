#!/usr/bin/env python3
"""
Firebase Authentication 로그인 테스트 스크립트
"""

import json
import requests
import firebase_admin
from firebase_admin import credentials, auth
import sys

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

def create_or_get_test_user(email, password="testpassword123", display_name="테스트사용자"):
    """Firebase에서 테스트 사용자 생성 또는 조회"""
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
            print(f"✅ 새로운 Firebase 사용자 생성 완료!")
            print(f"   이메일: {email}")
            print(f"   비밀번호: {password}")
            print(f"   UID: {user.uid}")
            print(f"   이름: {user.display_name}\n")
            return user
    except Exception as e:
        print(f"❌ 사용자 생성/조회 실패: {e}\n")
        return None

def create_custom_token(uid):
    """Custom Token 생성"""
    try:
        custom_token = auth.create_custom_token(uid)
        token_str = custom_token.decode('utf-8')
        print(f"✅ Custom Token 생성 성공")
        print(f"   Token (앞 50자): {token_str[:50]}...\n")
        return token_str
    except Exception as e:
        print(f"❌ Custom Token 생성 실패: {e}\n")
        return None

def exchange_custom_token_for_id_token(custom_token, web_api_key):
    """Custom Token을 ID Token으로 교환 (Firebase REST API 사용)"""
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key={web_api_key}"

    payload = {
        "token": custom_token,
        "returnSecureToken": True
    }

    try:
        print("🔄 Custom Token을 ID Token으로 교환 중...")
        response = requests.post(url, json=payload)

        if response.status_code == 200:
            result = response.json()
            id_token = result.get('idToken')
            print(f"✅ ID Token 발급 성공")
            print(f"   ID Token (앞 50자): {id_token[:50]}...\n")
            return id_token
        else:
            print(f"❌ ID Token 교환 실패: {response.status_code}")
            print(f"   응답: {response.text}\n")
            return None
    except Exception as e:
        print(f"❌ ID Token 교환 중 오류: {e}\n")
        return None

def test_server_login(id_token):
    """서버 로그인 API 테스트"""
    url = "http://localhost:8080/api/auth/login"

    payload = {
        "idToken": id_token,
        "fcmToken": "test-fcm-token-python-script"
    }

    try:
        print("📤 서버 로그인 API 호출 중...")
        print(f"   URL: {url}")
        response = requests.post(url, json=payload, headers={"Content-Type": "application/json"})

        print(f"   응답 코드: {response.status_code}\n")

        if response.status_code == 200:
            result = response.json()
            print("✅ 로그인 성공!")
            print("응답 데이터:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            print()
            return result
        else:
            print(f"❌ 로그인 실패")
            print("응답:")
            print(response.text)
            print()
            return None
    except Exception as e:
        print(f"❌ API 호출 실패: {e}\n")
        return None

def test_authenticated_api(id_token):
    """인증이 필요한 API 테스트 (북마크 조회)"""
    url = "http://localhost:8080/api/bookmarks"

    headers = {
        "Authorization": f"Bearer {id_token}",
        "Content-Type": "application/json"
    }

    try:
        print("📤 인증 API 테스트 (북마크 조회)...")
        response = requests.get(url, headers=headers)

        print(f"   응답 코드: {response.status_code}\n")

        if response.status_code == 200:
            result = response.json()
            print("✅ 인증 API 호출 성공!")
            print("응답 데이터:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            print()
            return result
        else:
            print(f"⚠️  응답 코드: {response.status_code}")
            print("응답:")
            print(response.text)
            print()
            return None
    except Exception as e:
        print(f"❌ API 호출 실패: {e}\n")
        return None

def main():
    print("=" * 60)
    print("Firebase Authentication 로그인 테스트")
    print("=" * 60)
    print()

    # 1. Firebase 초기화
    if not initialize_firebase():
        sys.exit(1)

    # 2. 테스트 사용자 생성/조회
    test_email = "firebasetest@inu.ac.kr"
    test_password = "testpass123"

    user = create_or_get_test_user(test_email, test_password, "Firebase테스트")
    if not user:
        sys.exit(1)

    # 3. Custom Token 생성
    custom_token = create_custom_token(user.uid)
    if not custom_token:
        sys.exit(1)

    # 4. Web API Key 입력 받기 (선택사항)
    print("⚠️  ID Token을 발급받으려면 Firebase Web API Key가 필요합니다.")
    print("Firebase Console → 프로젝트 설정 → 일반 → 웹 API 키")
    print()
    web_api_key = input("Web API Key를 입력하세요 (Enter 키만 누르면 Custom Token으로 테스트): ").strip()
    print()

    if web_api_key:
        # 5. Custom Token을 ID Token으로 교환
        id_token = exchange_custom_token_for_id_token(custom_token, web_api_key)
        if not id_token:
            print("❌ ID Token 발급 실패. Custom Token으로 테스트를 시도합니다.\n")
            id_token = custom_token
    else:
        print("ℹ️  Web API Key가 없으므로 Custom Token으로 테스트합니다.")
        print("   (참고: 서버는 ID Token을 요구하므로 실패할 수 있습니다)\n")
        id_token = custom_token

    # 6. 서버 로그인 API 테스트
    login_result = test_server_login(id_token)

    if login_result and login_result.get('success'):
        # 7. 인증 API 테스트
        returned_token = login_result.get('data', {}).get('idToken', id_token)
        test_authenticated_api(returned_token)

    print("=" * 60)
    print("테스트 완료!")
    print("=" * 60)
    print()

    if not web_api_key:
        print("💡 팁: Web API Key를 입력하면 정확한 ID Token으로 테스트할 수 있습니다.")
        print("   Firebase Console에서 확인 가능: https://console.firebase.google.com/")

if __name__ == "__main__":
    main()
