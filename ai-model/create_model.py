from tensorflow.keras.layers import Input, Dense, Conv2D, MaxPooling2D, Dropout, Conv2DTranspose, UpSampling2D, Add, LeakyReLU, AveragePooling2D, Flatten, BatchNormalization, GlobalAveragePooling2D
from tensorflow.keras.models import Model
from tensorflow.keras import regularizers
from PIL import Image 
from numpy import asarray 
import numpy as np
import matplotlib.pyplot as plt
import os
import sys


def segment_id(prev, wd):
    l1 = Conv2D(wd, (3,3), padding='same', kernel_regularizer='l2')(prev)
    l1 = BatchNormalization()(l1)
    l1 = LeakyReLU(alpha=0.01)(l1)
    
    l1 = Conv2D(wd, (3,3), padding='same', kernel_regularizer='l2')(l1)
    l1 = BatchNormalization()(l1)
    
    l1 = Add()([l1, prev])
    
    l1 = LeakyReLU(alpha=0.01)(l1)
    return l1

def segment_conv(prev, wd):
    l1 = Conv2D(wd, (3,3), padding='same', kernel_regularizer='l2')(prev)
    l1 = BatchNormalization()(l1)
    l1 = LeakyReLU(alpha=0.01)(l1)
    
    l1 = Conv2D(wd, (3,1), padding='same', kernel_regularizer='l2')(l1)
    l1 = BatchNormalization()(l1)
    l1 = LeakyReLU(alpha=0.01)(l1)
    
    l1 = Conv2D(wd, (1,3), padding='same', kernel_regularizer='l2')(l1)
    l1 = BatchNormalization()(l1)
    
    l2 = Conv2D(wd, (1,1), padding='same', kernel_regularizer='l2')(prev)
    l2 = BatchNormalization()(l2)
    
    l1 = Add()([l1, l2])
    
    l1 = LeakyReLU(alpha=0.01)(l1)
    return l1

inp = Input(shape=(480,270,3))
l1 = Conv2D(32, (7,7), strides=2, padding='same', kernel_regularizer='l2')(inp)
l1 = BatchNormalization()(l1)
l1 = LeakyReLU(alpha=0.01)(l1)
l1 = MaxPooling2D(pool_size=3, strides=2)(l1)

l2 = segment_conv(l1, 32)
l1 = segment_id(l2, 32)
l1 = MaxPooling2D()(l1)

l2 = segment_conv(l1, 48)
l1 = segment_id(l2, 48)
l1 = segment_id(l1, 48)
l1 = MaxPooling2D()(l1)

l2 = segment_conv(l1, 56)
l1 = segment_id(l2, 56)
l1 = segment_id(l1, 56)
l1 = MaxPooling2D()(l1)

l2 = segment_conv(l1, 72)
l1 = segment_id(l2, 72) 
l1 = segment_id(l1, 72)
l1 = MaxPooling2D()(l1)

l2 = segment_conv(l1, 256)
l1 = segment_id(l2, 256)
l1 = segment_id(l1, 256)
l1 = GlobalAveragePooling2D()(l1)

l1 = Flatten()(l1)

out = Dense(1, activation='sigmoid')(l1)

model = Model(inputs=inp, outputs=out)
model.summary() 

model.save('training/untrained')
