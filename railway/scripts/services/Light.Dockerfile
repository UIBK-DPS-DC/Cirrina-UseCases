FROM python:3.11

COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

WORKDIR /app
COPY light.py .

CMD ["uvicorn", "light:app", "--host", "0.0.0.0", "--port", "8000"]