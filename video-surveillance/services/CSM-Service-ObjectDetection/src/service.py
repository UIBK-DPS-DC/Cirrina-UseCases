import json
import os
import random
import base64

from fastapi import FastAPI, Request, HTTPException, Response
from pydantic import BaseModel
import numpy as np
import cv2
import ContextVariable_pb2

import uvicorn

app = FastAPI()

# Decide whether protobuf should be used (optional environment variable)
proto = "PROTO" not in os.environ \
    or os.environ["PROTO"].lower() in ["true", "t", "1"]

# Constants and configurations
INPUT_WIDTH = 640
INPUT_HEIGHT = 640
SCORE_THRESHOLD = 0.2
NMS_THRESHOLD = 0.4
CONFIDENCE_THRESHOLD = 0.4

# Dummy classes
CLASSES = [
    "person",
    "bicycle",
    "car",
    "motorcycle",
    "airplane",
    "bus",
    "train",
    "truck",
    "boat",
    "traffic light",
]


class Detection(BaseModel):
    class_name: str
    confidence: float
    x: int
    y: int
    width: int
    height: int


def generate_random_colors(n: int):
    random.seed(0)
    return [
        (random.randint(128, 255), random.randint(128, 255), random.randint(128, 255))
        for _ in range(n)
    ]


def mock_detect(image: np.ndarray):
    height, width, _ = image.shape
    detections = []

    # Mock detection for demonstration
    for _ in range(random.randint(1, 5)):
        x = random.randint(0, width - 50)
        y = random.randint(0, height - 50)
        w = random.randint(30, 100)
        h = random.randint(30, 100)
        confidence = random.uniform(CONFIDENCE_THRESHOLD, 1.0)
        class_id = random.randint(0, len(CLASSES) - 1)
        detections.append(
            {
                "class_name": CLASSES[class_id],
                "confidence": confidence,
                "x": x,
                "y": y,
                "width": w,
                "height": h,
            }
        )

    return detections


@app.post("/process")
async def process_image(request: Request):
    if proto:
        # Read the raw request body
        body = await request.body()

        # Parse the protobuf message
        context_variables = ContextVariable_pb2.ContextVariables()
        context_variables.ParseFromString(body)

        # Extract the image from the protobuf message
        image_bytes = None
        for context_variable in context_variables.data:
            if context_variable.name == "image":
                image_bytes = context_variable.value.bytes
                break
    else:
        # Parse the request variables
        context_variables = await request.json()

        # Get the image from the request json
        if "image" in context_variables:
            image_bytes = base64.b64decode(context_variables["image"].encode("utf-8"))

    if image_bytes is None:
        raise HTTPException(status_code=400, detail="No image provided in the request")

    np_arr = np.frombuffer(image_bytes, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    if image is None:
        raise HTTPException(status_code=400, detail="Failed to decode the image")

    detections = mock_detect(image)

    # Just return the detections JSON for now
    """    
    if image is not None:
        colors = generate_random_colors(len(CLASSES))
        for detection in detections:
            color = colors[CLASSES.index(detection["class_name"])]
            cv2.rectangle(
                image,
                (detection["x"], detection["y"]),
                (
                    detection["x"] + detection["width"],
                    detection["y"] + detection["height"],
                ),
                color,
                2,
            )
            cv2.putText(
                image,
                detection["class_name"],
                (detection["x"], detection["y"] - 10),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.9,
                color,
                2,
            )

        _, encoded_img = cv2.imencode(".jpg", image)
        return StreamingResponse(
            io.BytesIO(encoded_img.tobytes()), media_type="image/jpeg"
        )
    
    """

    detected_persons = [
        d for d in detections if d["class_name"] == "person" and d["confidence"] > 0.5
    ]

    if proto:
        # Create response protobuf message
        response_context_variables = ContextVariable_pb2.ContextVariables()
        detections_context_variable = ContextVariable_pb2.ContextVariable(
            name="personDetection_detected_persons",
            value=ContextVariable_pb2.Value(bool=len(detected_persons) > 0),
        )
        response_context_variables.data.append(detections_context_variable)

        # Serialize the response to protobuf format
        response = response_context_variables.SerializeToString()
        media_type = "application/x-protobuf"
    else:
        response = json.dumps({
            "personDetection_detected_persons": len(detected_persons) > 0
        })
        media_type = "application/json"

    print(f"Detected {len(detected_persons)} persons out of {len(detections)} objects.")

    # Return the protobuf response
    return Response(content=response, media_type=media_type)


if __name__ == "__main__":
    print(f"Protobuf: {proto}")
    uvicorn.run(app, host="0.0.0.0", port=8000)
