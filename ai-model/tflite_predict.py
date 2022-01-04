import numpy as np
import tensorflow as tf
from PIL import Image 
from numpy import asarray 
import sys

img=Image.open(sys.argv[1])

res = asarray(img)

res = res / 256

res = res.reshape((1,480,270,3))

# Load the TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_path="models/beagle_model4.tflite")
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Test the model on random input data.
input_shape = input_details[0]['shape']
input_data = np.float32(res)
interpreter.set_tensor(input_details[0]['index'], input_data)

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
output_data = interpreter.get_tensor(output_details[0]['index'])
print(output_data)
