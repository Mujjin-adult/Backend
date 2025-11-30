#!/usr/bin/env python3
"""
새로운 Firebase 사용자로 로그인 테스트
"""

import json
import requests
import firebase_admin
from firebase_admin import credentials, auth
import sys

def initialize_firebase():
    """Firebase Admin SDK 초기화"""
    try:
        # 이미 초기화되었는지 확인
        try:
            firebase_admin.get_app()
            print("✅ Firebase Admin SDK 이미 초기화됨\n")
        except ValueError:
            cred = credentials.Certificate('./firebase-credentials.json')
            firebase_admin.initialize_app(cred)
            print("✅ Firebase Admin SDK 초기화 성공\n")
        return True
    except Exception as e:
        print(f"❌ Firebase Admin SDK 초기화 실패: {e}\n")
        return False

def create_new_user(email, password="testpass123", display_name="새로운테스트"):
    """새 Firebase 사용자 생성"""
    try:
        # 기존 사용자가 있으면 삭제
        try:
            user = auth.get_user_by_email(email)
            auth.delete_user(user.uid)
            print(f"🗑️  기존 사용자 삭제: {email}\n")
        except auth.UserNotFoundError:
            pass

        # 새 사용자 생성
        user = auth.create_user(
            email=email,
            password=password,
            display_name=display_name,
            email_verified=True
        )
        print(f"✅ 새로운 Firebase 사용자 생성!")
        print(f"   이메일: {email}")
        print(f"   비밀번호: {password}")
        print(f"   UID: {user.uid}")
        print(f"   이름: {user.display_name}\n")
        return user
    except Exception as e:
        print(f"❌ 사용자 생성 실패: {e}\n")
        return None

def create_custom_token(uid):
    """Custom Token 생성"""
    try:
        custom_token = auth.create_custom_token(uid)
        token_str = custom_token.decode('utf-8')
        print(f"✅ Custom Token 생성 성공\n")
        return token_str
    except Exception as e:
        print(f"❌ Custom Token 생성 실패: {e}\n")
        return None

def exchange_custom_token_for_id_token(custom_token, web_api_key):
    """Custom Token을 ID Token으로 교환"""
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
            print(f"✅ ID Token 발급 성공\n")
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
        "fcmToken": "test-fcm-token-new-user"
    }

    try:
        print("📤 서버 로그인 API 호출 중...")
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

def main():
    print("=" * 60)
    print("Firebase Authentication 신규 사용자 테스트")
    print("=" * 60)
    print()

    # 1. Firebase 초기화
    if not initialize_firebase():
        sys.exit(1)

    # 2. 새 테스트 사용자 생성
    test_email = "newuser2025@inu.ac.kr"
    test_password = "newpass123"

    user = create_new_user(test_email, test_password, "신규사용자2025")
    if not user:
        sys.exit(1)

    # 3. Custom Token 생성
    custom_token = create_custom_token(user.uid)
    if not custom_token:
        sys.exit(1)

    # 4. Web API Key 사용
    web_api_key = "AIzaSyAmhyE1WLIbEay0tE_A9oLk8NxC5mYlwHM"

    # 5. ID Token 발급
    id_token = exchange_custom_token_for_id_token(custom_token, web_api_key)
    if not id_token:
        sys.exit(1)

    # 6. 서버 로그인 테스트
    login_result = test_server_login(id_token)

    print("=" * 60)
    print("테스트 완료!")
    print("=" * 60)
    print()

    if login_result and login_result.get('success'):
        user_data = login_result.get('data', {}).get('user', {})
        print("📊 생성된 사용자 정보:")
        print(f"   - User ID: {user_data.get('id')}")
        print(f"   - Email: {user_data.get('email')}")
        print(f"   - Name: {user_data.get('name')}")
        print(f"   - Student ID: {user_data.get('studentId') or '(없음 - 나중에 입력)'}")
        print(f"   - Role: {user_data.get('role')}")

if __name__ == "__main__":
    main()
