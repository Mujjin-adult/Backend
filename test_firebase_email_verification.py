#!/usr/bin/env python3
"""
Firebase 이메일 인증 기능 테스트
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

def create_test_user(email, password="testpass123", display_name="이메일인증테스트"):
    """테스트용 Firebase 사용자 생성 (이메일 인증되지 않은 상태)"""
    try:
        # 기존 사용자가 있으면 삭제
        try:
            user = auth.get_user_by_email(email)
            auth.delete_user(user.uid)
            print(f"🗑️  기존 사용자 삭제: {email}\n")
        except auth.UserNotFoundError:
            pass

        # 새 사용자 생성 (이메일 인증되지 않은 상태)
        user = auth.create_user(
            email=email,
            password=password,
            display_name=display_name,
            email_verified=False  # 이메일 인증 안된 상태
        )
        print(f"✅ 새로운 Firebase 사용자 생성!")
        print(f"   이메일: {email}")
        print(f"   비밀번호: {password}")
        print(f"   UID: {user.uid}")
        print(f"   이름: {user.display_name}")
        print(f"   이메일 인증: {user.email_verified}")
        print()
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
    """서버 로그인 API 테스트 (자동 회원가입)"""
    url = "http://localhost:8080/api/auth/login"

    payload = {
        "idToken": id_token,
        "fcmToken": "test-fcm-token-email-verification"
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

def test_send_verification_email(email):
    """이메일 인증 메일 발송 API 테스트"""
    url = f"http://localhost:8080/api/auth/send-verification-email?email={email}"

    try:
        print("📤 이메일 인증 메일 발송 API 호출 중...")
        response = requests.post(url, headers={"Content-Type": "application/json"})

        print(f"   응답 코드: {response.status_code}\n")

        if response.status_code == 200:
            result = response.json()
            print("✅ 이메일 인증 메일 발송 성공!")
            print("응답 데이터:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            print()
            return True
        else:
            print(f"❌ 이메일 인증 메일 발송 실패")
            print("응답:")
            print(response.text)
            print()
            return False
    except Exception as e:
        print(f"❌ API 호출 실패: {e}\n")
        return False

def test_resend_verification_email(email):
    """이메일 인증 메일 재발송 API 테스트"""
    url = f"http://localhost:8080/api/auth/resend-verification-email?email={email}"

    try:
        print("📤 이메일 인증 메일 재발송 API 호출 중...")
        response = requests.post(url, headers={"Content-Type": "application/json"})

        print(f"   응답 코드: {response.status_code}\n")

        if response.status_code == 200:
            result = response.json()
            print("✅ 이메일 인증 메일 재발송 성공!")
            print("응답 데이터:")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            print()
            return True
        else:
            print(f"❌ 이메일 인증 메일 재발송 실패")
            print("응답:")
            print(response.text)
            print()
            return False
    except Exception as e:
        print(f"❌ API 호출 실패: {e}\n")
        return False

def check_firebase_email_verified(email):
    """Firebase에서 이메일 인증 상태 확인"""
    try:
        user = auth.get_user_by_email(email)
        print(f"📊 Firebase 사용자 이메일 인증 상태: {user.email_verified}")
        return user.email_verified
    except Exception as e:
        print(f"❌ Firebase 사용자 조회 실패: {e}")
        return False

def main():
    print("=" * 60)
    print("Firebase 이메일 인증 기능 테스트")
    print("=" * 60)
    print()

    # 1. Firebase 초기화
    if not initialize_firebase():
        sys.exit(1)

    # 2. 테스트 사용자 생성 (이메일 인증되지 않은 상태)
    test_email = "emailverify@inu.ac.kr"
    test_password = "verifypass123"

    user = create_test_user(test_email, test_password, "이메일인증테스트")
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

    # 6. 서버 로그인 테스트 (자동 회원가입)
    login_result = test_server_login(id_token)
    if not login_result:
        sys.exit(1)

    print("=" * 60)
    print("이메일 인증 메일 발송 테스트")
    print("=" * 60)
    print()

    # 7. Firebase에서 이메일 인증 상태 확인 (발송 전)
    print("📋 이메일 인증 메일 발송 전 상태:")
    check_firebase_email_verified(test_email)
    print()

    # 8. 이메일 인증 메일 발송 API 테스트
    send_success = test_send_verification_email(test_email)

    if send_success:
        print("=" * 60)
        print("이메일 인증 메일 재발송 테스트")
        print("=" * 60)
        print()

        # 9. 이메일 인증 메일 재발송 API 테스트
        resend_success = test_resend_verification_email(test_email)

    print("=" * 60)
    print("테스트 완료!")
    print("=" * 60)
    print()

    print("📝 테스트 요약:")
    print(f"   - 테스트 이메일: {test_email}")
    print(f"   - Firebase 사용자 생성: ✅")
    print(f"   - 서버 로그인 (자동 회원가입): {'✅' if login_result else '❌'}")
    print(f"   - 이메일 인증 메일 발송: {'✅' if send_success else '❌'}")
    if send_success:
        print(f"   - 이메일 인증 메일 재발송: {'✅' if resend_success else '❌'}")
    print()

    print("⚠️  참고사항:")
    print("   1. 이메일 발송은 SMTP 설정이 필요합니다 (application.yml)")
    print("   2. Gmail을 사용하는 경우 앱 비밀번호 설정이 필요합니다")
    print("   3. Firebase Console에서 이메일 템플릿 커스터마이징 가능")
    print("   4. 클라이언트에서 user.sendEmailVerification() 사용도 권장됩니다")
    print()

    print("🔗 관련 API 엔드포인트:")
    print("   - POST /api/auth/send-verification-email?email={email}")
    print("   - POST /api/auth/resend-verification-email?email={email}")
    print()

if __name__ == "__main__":
    main()
