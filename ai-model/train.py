from tensorflow.keras.layers import Input, Dense, Conv2D, MaxPooling2D, Dropout, Conv2DTranspose, UpSampling2D, add, LeakyReLU, AveragePooling2D, Flatten
from tensorflow.keras.models import Model, load_model
from tensorflow.keras import regularizers
from tensorflow import keras
import tensorflow as tf
from PIL import Image 
from numpy import asarray 
import numpy as np
import matplotlib.pyplot as plt
import os
import sys
import random

mseed=136436

dataset = keras.preprocessing.image_dataset_from_directory(
    './data',
    labels="inferred",
    label_mode="binary",
    class_names=['class_n', 'class_b'],
    color_mode="rgb",
    batch_size=8,
    image_size=(480, 270),
    shuffle=True,
    seed=mseed,
    validation_split=0.1,
    subset="training",
    interpolation="bilinear",
    follow_links=False
)

val_dataset = keras.preprocessing.image_dataset_from_directory(
    './data',
    labels="inferred",
    label_mode="binary",
    class_names=['class_n', 'class_b'],
    color_mode="rgb",
    batch_size=1,
    image_size=(480, 270),
    shuffle=True,
    seed=mseed,
    validation_split=0.1,
    subset="validation",
    interpolation="bilinear",
    follow_links=False
)

def process(image,label):
    image = tf.cast(image/255. ,tf.float32)
    return image,label

dataset = dataset.map(process)
val_dataset = val_dataset.map(process)

loaded_model = load_model(
    "./training/untrained",
    custom_objects=None,
    compile=False
)

opt = tf.keras.optimizers.SGD(
    learning_rate=0.0006, momentum=0.5, nesterov=True, name="SGD"
)

opt2 = tf.keras.optimizers.Adagrad(
    learning_rate=0.01,
    initial_accumulator_value=0.1,
    epsilon=1e-07,
    name="Adagrad"
)

opt3 = tf.keras.optimizers.Adadelta(
    learning_rate=0.0003, rho=0.95, epsilon=1e-07, name="Adadelta"
)


loaded_model.compile(optimizer=opt2, loss='binary_crossentropy', metrics=['accuracy'])


for i in range(1,100):
    
    history = loaded_model.fit(dataset,
                    epochs=10, 
                    validation_data=val_dataset)
    loaded_model.save(f"./training/trained{i}")
