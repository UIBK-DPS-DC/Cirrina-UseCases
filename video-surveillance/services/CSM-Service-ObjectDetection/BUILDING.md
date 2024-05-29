# CSM Service Object Detection Development Guide

## Prerequisites
- Docker
- protocol buffer compiler (protoc) version 25.3


## Installation

If you have no generated protobuf files, you can generate them by running the following command:

```bash
protoc -I=proto --python_out=src .\proto\ContextVariable.proto .\proto\Event.proto
```
