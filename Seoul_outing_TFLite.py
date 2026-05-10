import json
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow.keras.layers import Dense, Input
from tensorflow.keras.models import Sequential


# ==========================================
# 1. CSV 쉼표 충돌을 완벽히 우회하는 강력한 데이터 로더
# ==========================================
def load_and_preprocess_data(csv_path):
    print("📦 CSV 데이터를 파싱하고 Pointwise 분할을 시작합니다...")
    expanded_rows = []

    # pd.read_csv 대신 파일의 텍스트를 직접 읽어 쉼표 오작동을 원천 차단합니다.
    with open(csv_path, "r", encoding="utf-8") as f:
        header = f.readline()  # 첫 줄(헤더) 넘기기

        for line_num, line in enumerate(f, start=1):
            line = line.strip()
            if not line:
                continue

            # 🔥 [핵심 알고리즘] 텍스트에서 직접 '[' 와 ']' 의 위치를 찾아 JSON 구간만 완벽하게 도려냅니다.
            start_idx = line.find("[")
            end_idx = line.rfind("]")

            if start_idx == -1 or end_idx == -1:
                print(
                    f"⚠️ {line_num}번째 줄: JSON 후보군 배열을 찾을 수 없어 스킵합니다."
                )
                continue

            # 앞부분(날씨/취향), JSON 문자열, 뒷부분(선택된 정답)으로 안전하게 3등분
            prefix = line[:start_idx].rstrip(",")
            json_str = line[start_idx : end_idx + 1]
            suffix = line[end_idx + 1 :].lstrip(",")

            # 1. 앞부분 6개 피처 추출 (temp, precipitation, pmIndex, congestion, eventScore, user20sPref)
            prefix_fields = prefix.split(",")
            if len(prefix_fields) < 6:
                print(
                    f"⚠️ {line_num}번째 줄: 날씨 및 취향 데이터가 부족하여 스킵합니다."
                )
                continue

            try:
                user_pref = float(prefix_fields[5])
            except ValueError:
                print(
                    f"⚠️ {line_num}번째 줄: 취향 점수 숫자 변환 오류로 스킵합니다."
                )
                continue

            # 2. 뒷부분 정답 라벨 추출 (selectedPlace)
            suffix_fields = suffix.split(",")
            selected_place = suffix_fields[0].strip() if suffix_fields else ""

            # 텍스트에 큰따옴표가 남아있다면 깔끔하게 제거
            if selected_place.startswith('"') and selected_place.endswith('"'):
                selected_place = selected_place[1:-1]

            # 3. 완벽하게 발췌된 JSON 파싱
            try:
                candidates = json.loads(json_str)
            except Exception as e:
                print(
                    f"⚠️ {line_num}번째 줄 JSON 파싱 실패 (스킵됨) | 사유: {e}"
                )
                continue

            # 1줄의 로그를 10줄의 개별 학습 데이터로 확장 (1 정답, 9 오답)
            for cand in candidates:
                place_info = cand["place"]
                place_name = place_info["placeName"]
                label = 1.0 if place_name == selected_place else 0.0

                expanded_rows.append(
                    [
                        float(user_pref),
                        float(cand["localTemp"]),
                        float(cand["localRain"]),
                        float(cand["localPm"]),
                        float(cand["localCongest"]),
                        float(cand["localEvent"]),
                        float(place_info["indoorTag"]),
                        float(place_info["target20sRatio"]),
                        label,
                    ]
                )

    if len(expanded_rows) == 0:
        raise ValueError(
            "\n🚨 [에러] 성공적으로 변환된 학습 데이터가 0개입니다!\n"
            "파일 내부 텍스트 구조를 다시 확인해주세요."
        )

    # 넘파이 배열 변환
    data_array = np.array(expanded_rows, dtype=np.float32)
    X = data_array[:, :-1]  # 8개 피처
    y = data_array[:, -1]  # 1개 라벨 (0.0 or 1.0)

    print(
        f"✅ 전처리 완료: 원본 {line_num}줄에서 총 {len(X)}개의 훈련 샘플 추출 성공!"
    )
    return X, y


# 🔥 아래 경로를 PC에 저장된 실제 CSV 파일 이름으로 맞춰주세요!
X_train, y_train = load_and_preprocess_data(
    r"C:\Users\nugum\OneDrive\바탕 화면\서울 나들이\seoul_outing_logs_sample.csv"
)


# ==========================================
# 2. 초경량 모바일 딥러닝 모델 설계 (Lightweight DNN)
# ==========================================
print("\n🚀 초경량 딥러닝 모델 학습을 시작합니다...")

model = Sequential(
    [
        Input(shape=(8,)),
        Dense(32, activation="relu"),
        Dense(16, activation="relu"),
        Dense(1, activation="sigmoid"),  # 0.0 ~ 1.0 사이 확률 점수 출력
    ]
)

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.005),
    loss="binary_crossentropy",
    metrics=["accuracy"],
)

model.fit(X_train, y_train, epochs=30, batch_size=16, verbose=1)


# ==========================================
# 3. 순정 TFLite 변환 (초경량 양자화)
# ==========================================
print("\n⚙️ 안드로이드 기본 탑재용 TFLite 변환을 시작합니다...")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # 용량을 4분의 1로 압축
tflite_model = converter.convert()

tflite_filename = "seoul_outing_lightweight.tflite"
with open(tflite_filename, "wb") as f:
    f.write(tflite_model)

print(
    f"🎉 변환 성공! 용량이 극도로 작은 [{tflite_filename}] 모델 생성 완료."
)
