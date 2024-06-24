import commentjson
import json

with open('../../csml/smart_factory_develop.csm', 'r') as file:
    data = commentjson.load(file)

json_string = json.dumps(data, ensure_ascii=False)

print(json_string)