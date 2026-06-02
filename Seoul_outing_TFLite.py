import json
import numpy as np
import tensorflow as tf
from tensorflow.keras.layers import Dense, Input
from tensorflow.keras.models import Sequential
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score

# =====================================================================
# 1. 커스텀 데이터 로더: CSV 쉼표 충돌 완벽 우회 및 정규화 통합
# =====================================================================
def load_and_preprocess_data(csv_path):
    print("📦 데이터를 파싱하고 대조학습용(Pointwise)으로 확장을 시작합니다...")
    expanded_rows = []

    # 원본 텍스트를 직접 읽어 JSON 내부 쉼표로 인한 파싱 오류 원천 차단
    with open(csv_path, "r", encoding="utf-8") as f:
        header = f.readline()  # 첫 줄(헤더) 건너뛰기

        for line_num, line in enumerate(f, start=1):
            line = line.strip()
            if not line:
                continue

            # 텍스트에서 대괄호 '[' 와 ']' 의 위치를 직접 찾아 JSON 영역 발췌
            start_idx = line.find("[")
            end_idx = line.rfind("]")

            if start_idx == -1 or end_idx == -1:
                continue

            # 1단계: 앞부분(기본 환경), JSON 문자열, 뒷부분(정답 장소)으로 분할
            prefix = line[:start_idx].rstrip(",")
            json_str = line[start_idx : end_idx + 1]
            suffix = line[end_idx + 1 :].lstrip(",")

            # 2단계: 유저 취향(user20sPref) 파싱 (index 5)
            prefix_fields = prefix.split(",")
            if len(prefix_fields) < 6:
                continue

            try:
                user_pref = float(prefix_fields[5])
            except ValueError:
                continue

            # 3단계: 최종 선택된 정답 장소(selectedPlace) 파싱
            suffix_fields = suffix.split(",")
            selected_place = suffix_fields[0].strip() if suffix_fields else ""
            if selected_place.startswith('"') and selected_place.endswith('"'):
                selected_place = selected_place[1:-1]

            # 4단계: 분리된 JSON 텍스트 파싱
            try:
                candidates = json.loads(json_str)
            except Exception as e:
                continue

            # 5단계: 1줄의 로그를 10줄의 대조 학습용 샘플(1 정답, 9 오답)로 전개
            for cand in candidates:
                place_info = cand["place"]
                place_name = place_info["placeName"]

                # 🎯 핵심: 정답 장소 1.0, 선택받지 못한 장소 0.0으로 라벨링
                label = 1.0 if place_name == selected_place else 0.0

                # 🔥 정규화 적용 (기온, 강수량을 0.0 ~ 1.0 사이로 압축 및 클리핑)
                raw_temp = float(cand["localTemp"])
                norm_temp = max(0.0, min(1.0, (raw_temp + 20.0) / 60.0))

                raw_rain = float(cand["localRain"])
                norm_rain = max(0.0, min(1.0, raw_rain / 50.0))

                # 주의: 미세먼지(localPm)는 안드로이드에서 이미 정규화되어 오므로 그대로 사용
                norm_pm = float(cand["localPm"])

                # 최종 8개 입력 피처 구성
                expanded_rows.append([
                    float(user_pref),                     # [0] 유저 취향
                    norm_temp,                            # [1] 정규화된 기온
                    norm_rain,                            # [2] 정규화된 강수량
                    norm_pm,                              # [3] 정규화된 미세먼지
                    float(cand["localCongest"]),          # [4] 혼잡도
                    float(cand["localEvent"]),            # [5] 축제 여부 (0.0 or 1.0)
                    float(place_info["indoorTag"]),       # [6] 실내외 태그 (0.0 or 1.0)
                    float(place_info["target20sRatio"]),  # [7] 20대 비율
                    label,                                # [Output] 최종 라벨
                ])

    if len(expanded_rows) == 0:
        raise ValueError("🚨 [에러] 성공적으로 추출된 학습 데이터가 0개입니다!")

    data_array = np.array(expanded_rows, dtype=np.float32)
    X = data_array[:, :-1]  # 8개 피처
    y = data_array[:, -1]   # 1개 라벨

    print(f"✅ 전처리 완료: 총 {len(X)}개의 학습 샘플 추출 성공!")
    return X, y

# 🔥 [필수] 실제 CSV 파일 경로
csv_file_path = r"C:\Users\nugum\OneDrive\바탕 화면\서울 나들이\seoul_outing_logs_sample.csv"
X_all, y_all = load_and_preprocess_data(csv_file_path)


# =====================================================================
# 2. 데이터 분할 및 초경량 딥러닝 모델 학습
# =====================================================================
print("\n🚀 데이터 분할 및 모델 학습을 시작합니다...")

# 전체 데이터를 학습용(80%)과 테스트/검증용(20%)으로 분리
X_train, X_test, y_train, y_test = train_test_split(
    X_all, y_all, test_size=0.2, random_state=42, stratify=y_all
)
print(f"📊 학습 데이터: {len(X_train)}개 | 검증 데이터: {len(X_test)}개")

# 모델 설계 (Fully Connected Network / MLP)
model = Sequential([
    Input(shape=(8,)),
    Dense(32, activation="relu"),
    Dense(16, activation="relu"),
    Dense(1, activation="sigmoid"), # 0.0 ~ 1.0 사이의 추천 점수 출력
])

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
    loss="binary_crossentropy",
    metrics=["accuracy"],
)

# 🔥 [핵심 수정] 0(오답)이 9배 많은 데이터 불균형 참교육 로직 (Class Weight)
# AI가 무조건 0으로 찍는 꼼수를 막기 위해, 1(정답)을 맞추면 9배의 보상을 줌
class_weights = {0: 1.0, 1: 9.0}

# validation_data를 통해 매 에포크마다 검증 데이터로 성능 평가
model.fit(
    X_train, y_train, 
    validation_data=(X_test, y_test), 
    epochs=30, 
    batch_size=16, 
    class_weight=class_weights, # 🔥 가중치 적용!
    verbose=1
)


# =====================================================================
# 3. 모델 성능 상세 검증 (Evaluation & Sanity Check)
# =====================================================================
print("\n🔍 테스트 데이터(20%)에 대한 최종 성능 검증...")
loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"✅ 최종 검증 정확도(Accuracy): {accuracy * 100:.2f}% (가중치로 인해 현실적인 수치로 조정됨)")

# AUC 점수 계산 (추천 시스템 중요 지표)
y_pred_probs = model.predict(X_test, verbose=0)
auc_score = roc_auc_score(y_test, y_pred_probs)
print(f"🎯 AUC 점수 (1.0에 가까울수록 좋음): {auc_score:.4f}")

# 🔥 시나리오 테스트 (Sanity Check)
print("\n🧠 [Sanity Check] 상식 테스트 시뮬레이션")
# 입력: [취향(0.8), 기온(0.6), 비폭우(1.0), 미세먼지최악(1.0), 혼잡도(0.5), 축제무(0.0), 실내외, 20대비율(0.7)]
scenario_A = np.array([[0.8, 0.6, 1.0, 1.0, 0.5, 0.0, 0.0, 0.7]]) # 야외 장소
scenario_B = np.array([[0.8, 0.6, 1.0, 1.0, 0.5, 0.0, 1.0, 0.7]]) # 실내 장소

score_A = model.predict(scenario_A, verbose=0)[0][0] * 100
score_B = model.predict(scenario_B, verbose=0)[0][0] * 100

print(f" - 폭우/미세먼지 심한 날 [야외] 매력도: {score_A:.1f}점")
print(f" - 폭우/미세먼지 심한 날 [실내] 매력도: {score_B:.1f}점")
if score_B > score_A:
    print(" 💡 [통과] AI가 악천후에는 실내를 선호하도록 올바르게 학습되었습니다!")
else:
    print(" ⚠️ [경고] 데이터가 부족하여 AI가 아직 날씨와 실내외 관계를 정확히 파악하지 못했습니다.")


# =====================================================================
# 4. 안드로이드 순정 TFLite 변환 (초경량 최적화)
# =====================================================================
print("\n⚙️ 안드로이드 탑재용 TFLite 변환을 시작합니다...")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT] # 양자화(Quantization) 최적화
tflite_model = converter.convert()

tflite_filename = "seoul_outing_lightweight.tflite"
with open(tflite_filename, "wb") as f:
    f.write(tflite_model)

print(f"🎉 성공! 초경량 모델 [{tflite_filename}] 생성 완료.")
