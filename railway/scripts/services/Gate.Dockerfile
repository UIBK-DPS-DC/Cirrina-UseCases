FROM python:3.11

COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

WORKDIR /app
COPY gate.py .

CMD ["uvicorn", "gate:app", "--host", "0.0.0.0", "--port", "8000"]