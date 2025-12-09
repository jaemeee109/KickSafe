# train_obb.py
# ------------------------------------------
# Roboflow에서 받은 YOLOv8 Oriented Bounding Boxes 데이터셋으로
# YOLOv8-OBB 모델을 학습시키는 스크립트입니다.
# 학습이 끝나면 best.pt를 KickSafe-Ai 서버에서 사용할 수 있습니다.
# ------------------------------------------

from ultralytics import YOLO


def main():
    # 1) Roboflow에서 받은 OBB 데이터셋의 YAML 파일 경로를 여기에 넣어주세요.
    #    예시: r"C:\datasets\kicksafe_obb\data.yaml"
    data_yaml_path = r"C:\kicksafe-dataset\data.yaml"

    # 2) 사전학습된 YOLOv8-OBB 기본 모델 로드
    #    - "yolov8n-obb.pt" 는 처음 실행 시 자동으로 다운로드됩니다.
    model = YOLO("yolov8n-obb.pt")

    # 3) 학습 실행
    #    - project: 결과 저장 상위 폴더
    #    - name   : 실험 이름
    #    - imgsz  : 입력 이미지 크기
    #    - epochs : 학습 epoch 수 (필요에 따라 조정)
    model.train(
        data=data_yaml_path,
        project="runs/obb",
        name="kicksafe-obb",
        imgsz=640,
        epochs=100,
        batch=16,
    )

    print("===> YOLOv8-OBB 학습이 완료되었습니다.")
    print("     결과 폴더: runs/obb/kicksafe-obb")
    print("     best.pt 경로: runs/obb/kicksafe-obb/weights/best.pt")


if __name__ == "__main__":
    main()
