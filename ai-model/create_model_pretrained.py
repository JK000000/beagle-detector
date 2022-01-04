import numpy as np
from tensorflow.keras.layers import Input, Dense, Conv2D, MaxPooling2D, Dropout, Conv2DTranspose, UpSampling2D, Add, LeakyReLU, AveragePooling2D, Flatten, BatchNormalization, GlobalAveragePooling2D
import tensorflow as tf


inp = Input(shape=(480,270,3))
mobilenetv2 = tf.keras.applications.MobileNetV2(
    include_top=False,
    weights='imagenet',
    input_tensor=inp
)

#mobilenetv2.trainable = False

model = tf.keras.Sequential()
model.add(mobilenetv2)
model.add(GlobalAveragePooling2D())
model.add(Dropout(0.1))
model.add(Dense(1, activation='sigmoid'))

model.summary() 

model.save('models/untrained')
