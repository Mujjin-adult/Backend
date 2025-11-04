"""
Circuit Breaker 패턴 구현

외부 서비스 호출 실패 시 시스템을 보호하고 복구를 위한 시간을 제공합니다.

상태:
- CLOSED: 정상 작동 (요청 허용)
- OPEN: 차단 상태 (요청 거부, 빠른 실패)
- HALF_OPEN: 복구 시도 (제한적 요청 허용)
"""

import time
import logging
from enum import Enum
from typing import Callable, Any, Optional, Dict
from functools import wraps
from collections import deque
from threading import Lock

logger = logging.getLogger(__name__)


class CircuitState(Enum):
    """Circuit Breaker 상태"""
    CLOSED = "CLOSED"  # 정상 작동
    OPEN = "OPEN"  # 차단 상태
    HALF_OPEN = "HALF_OPEN"  # 복구 시도


class CircuitBreakerError(Exception):
    """Circuit Breaker가 OPEN 상태일 때 발생하는 예외"""
    pass


class CircuitBreaker:
    """
    Circuit Breaker 패턴 구현

    Parameters:
    - failure_threshold: 실패 임계값 (연속 실패 횟수)
    - success_threshold: 성공 임계값 (HALF_OPEN에서 CLOSED로 전환하기 위한 성공 횟수)
    - timeout: OPEN 상태 유지 시간 (초)
    - expected_exception: 감지할 예외 타입 (기본: Exception)
    """

    def __init__(
        self,
        failure_threshold: int = 5,
        success_threshold: int = 2,
        timeout: float = 60.0,
        expected_exception: type = Exception,
        name: Optional[str] = None,
    ):
        self.failure_threshold = failure_threshold
        self.success_threshold = success_threshold
        self.timeout = timeout
        self.expected_exception = expected_exception
        self.name = name or "CircuitBreaker"

        self._state = CircuitState.CLOSED
        self._failure_count = 0
        self._success_count = 0
        self._last_failure_time: Optional[float] = None
        self._lock = Lock()

        # 최근 호출 결과 저장 (모니터링용)
        self._recent_calls = deque(maxlen=100)

        logger.info(
            f"{self.name} initialized - "
            f"failure_threshold={failure_threshold}, "
            f"success_threshold={success_threshold}, "
            f"timeout={timeout}s"
        )

    @property
    def state(self) -> CircuitState:
        """현재 상태 반환"""
        with self._lock:
            # OPEN 상태에서 timeout이 지났으면 HALF_OPEN으로 전환
            if self._state == CircuitState.OPEN:
                if self._last_failure_time and (
                    time.time() - self._last_failure_time >= self.timeout
                ):
                    logger.info(f"{self.name} transitioning from OPEN to HALF_OPEN")
                    self._state = CircuitState.HALF_OPEN
                    self._success_count = 0

            return self._state

    def call(self, func: Callable, *args, **kwargs) -> Any:
        """
        함수 호출을 Circuit Breaker로 보호

        Args:
            func: 호출할 함수
            *args, **kwargs: 함수 인자

        Returns:
            함수 실행 결과

        Raises:
            CircuitBreakerError: Circuit이 OPEN 상태일 때
            Exception: 함수 실행 중 발생한 예외
        """
        current_state = self.state

        if current_state == CircuitState.OPEN:
            logger.warning(f"{self.name} is OPEN, rejecting call to {func.__name__}")
            raise CircuitBreakerError(f"Circuit breaker {self.name} is OPEN")

        try:
            start_time = time.time()
            result = func(*args, **kwargs)
            duration = time.time() - start_time

            self._on_success(duration)
            return result

        except self.expected_exception as e:
            logger.warning(
                f"{self.name} caught exception in {func.__name__}: {type(e).__name__}: {str(e)}"
            )
            self._on_failure()
            raise

    def _on_success(self, duration: float):
        """성공 시 호출"""
        with self._lock:
            self._record_call(True, duration)

            if self._state == CircuitState.HALF_OPEN:
                self._success_count += 1
                logger.info(
                    f"{self.name} success in HALF_OPEN "
                    f"({self._success_count}/{self.success_threshold})"
                )

                if self._success_count >= self.success_threshold:
                    logger.info(f"{self.name} transitioning from HALF_OPEN to CLOSED")
                    self._state = CircuitState.CLOSED
                    self._failure_count = 0
                    self._success_count = 0

            elif self._state == CircuitState.CLOSED:
                # CLOSED 상태에서는 실패 카운트 리셋
                self._failure_count = 0

    def _on_failure(self):
        """실패 시 호출"""
        with self._lock:
            self._record_call(False, 0)
            self._failure_count += 1
            self._last_failure_time = time.time()

            if self._state == CircuitState.HALF_OPEN:
                logger.warning(f"{self.name} failure in HALF_OPEN, reopening circuit")
                self._state = CircuitState.OPEN
                self._success_count = 0

            elif self._state == CircuitState.CLOSED:
                if self._failure_count >= self.failure_threshold:
                    logger.error(
                        f"{self.name} transitioning from CLOSED to OPEN "
                        f"(failures: {self._failure_count}/{self.failure_threshold})"
                    )
                    self._state = CircuitState.OPEN

    def _record_call(self, success: bool, duration: float):
        """호출 결과 기록 (모니터링용)"""
        self._recent_calls.append(
            {
                "timestamp": time.time(),
                "success": success,
                "duration": duration,
                "state": self._state.value,
            }
        )

    def get_stats(self) -> Dict[str, Any]:
        """통계 정보 반환"""
        with self._lock:
            total_calls = len(self._recent_calls)
            if total_calls == 0:
                success_rate = 0
                avg_duration = 0
            else:
                successes = sum(1 for call in self._recent_calls if call["success"])
                success_rate = (successes / total_calls) * 100
                durations = [
                    call["duration"]
                    for call in self._recent_calls
                    if call["success"]
                ]
                avg_duration = sum(durations) / len(durations) if durations else 0

            return {
                "name": self.name,
                "state": self._state.value,
                "failure_count": self._failure_count,
                "success_count": self._success_count,
                "total_calls": total_calls,
                "success_rate": round(success_rate, 2),
                "avg_duration": round(avg_duration, 3),
                "last_failure_time": self._last_failure_time,
            }

    def reset(self):
        """Circuit Breaker 리셋 (CLOSED 상태로 전환)"""
        with self._lock:
            logger.info(f"{self.name} reset to CLOSED state")
            self._state = CircuitState.CLOSED
            self._failure_count = 0
            self._success_count = 0
            self._last_failure_time = None


def circuit_breaker(
    failure_threshold: int = 5,
    success_threshold: int = 2,
    timeout: float = 60.0,
    expected_exception: type = Exception,
    name: Optional[str] = None,
):
    """
    Circuit Breaker 데코레이터

    사용 예시:

    @circuit_breaker(failure_threshold=3, timeout=30, name="external_api")
    def call_external_api():
        response = requests.get("https://api.example.com/data")
        return response.json()
    """

    def decorator(func: Callable) -> Callable:
        breaker_name = name or f"{func.__module__}.{func.__name__}"
        breaker = CircuitBreaker(
            failure_threshold=failure_threshold,
            success_threshold=success_threshold,
            timeout=timeout,
            expected_exception=expected_exception,
            name=breaker_name,
        )

        @wraps(func)
        def wrapper(*args, **kwargs):
            return breaker.call(func, *args, **kwargs)

        # breaker 인스턴스를 함수 속성으로 저장 (테스트/모니터링용)
        wrapper.circuit_breaker = breaker

        return wrapper

    return decorator


# 전역 Circuit Breaker 인스턴스 관리
_circuit_breakers: Dict[str, CircuitBreaker] = {}
_breakers_lock = Lock()


def get_circuit_breaker(
    name: str,
    failure_threshold: int = 5,
    success_threshold: int = 2,
    timeout: float = 60.0,
    expected_exception: type = Exception,
) -> CircuitBreaker:
    """
    이름으로 Circuit Breaker 인스턴스 가져오기

    같은 이름으로 호출하면 동일한 인스턴스 반환 (싱글톤 패턴)
    """
    with _breakers_lock:
        if name not in _circuit_breakers:
            _circuit_breakers[name] = CircuitBreaker(
                failure_threshold=failure_threshold,
                success_threshold=success_threshold,
                timeout=timeout,
                expected_exception=expected_exception,
                name=name,
            )
        return _circuit_breakers[name]


def get_all_circuit_breaker_stats() -> Dict[str, Dict[str, Any]]:
    """모든 Circuit Breaker 통계 반환"""
    with _breakers_lock:
        return {name: breaker.get_stats() for name, breaker in _circuit_breakers.items()}


def reset_all_circuit_breakers():
    """모든 Circuit Breaker 리셋"""
    with _breakers_lock:
        for breaker in _circuit_breakers.values():
            breaker.reset()
        logger.info(f"Reset {len(_circuit_breakers)} circuit breakers")
