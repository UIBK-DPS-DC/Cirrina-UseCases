import json
import os
import random
import base64
import time
import hashlib

from fastapi import FastAPI, Request, HTTPException, Response
from pydantic import BaseModel
import numpy as np
import cv2
import ContextVariable_pb2

import uvicorn

app = FastAPI()

# Decide whether protobuf should be used (optional environment variable)
proto = "PROTO" not in os.environ or os.environ["PROTO"].lower() in ["true", "t", "1"]

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

hog = cv2.HOGDescriptor()
hog.setSVMDetector(cv2.HOGDescriptor_getDefaultPeopleDetector())


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


def log_data(data: bytes):
    sha256 = hashlib.sha256()
    sha256.update(data)
    hash = sha256.hexdigest()

    timestamp = time.time()
    log_entry = f"{hash},{timestamp}\n"

    with open("/tmp/log_detection.csv", "a") as log_file:
        log_file.write(log_entry)


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

    (regions, _) = hog.detectMultiScale(
        image, winStride=(4, 4), padding=(4, 4), scale=1.05
    )

    # For drawing detections:
    # image_dbg = image.copy()
    # colors = generate_random_colors(len(regions))
    # for color, (x, y, w, h) in zip(colors, regions):
    #    cv2.rectangle(image_dbg, (x, y), (x + w, y + h), color, 2)
    # cv2.imwrite("image_detected.jpg", image_dbg)

    if proto:
        # Create response protobuf message
        response_context_variables = ContextVariable_pb2.ContextVariables()
        detections_context_variable = ContextVariable_pb2.ContextVariable(
            name="personDetection_detected_persons",
            value=ContextVariable_pb2.Value(bool=len(regions) > 0),
        )
        response_context_variables.data.append(detections_context_variable)

        # Serialize the response to protobuf format
        response = response_context_variables.SerializeToString()
        media_type = "application/x-protobuf"
    else:
        response = json.dumps({"personDetection_detected_persons": len(regions) > 0})
        media_type = "application/json"

    log_data(image_bytes)

    # Return the protobuf response
    return Response(content=response, media_type=media_type)


if __name__ == "__main__":
    print(f"Protobuf: {proto}")
    uvicorn.run(app, host="0.0.0.0", port=8000)
