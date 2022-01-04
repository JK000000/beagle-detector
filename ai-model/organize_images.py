import os
from PIL import Image 

j=6405

for filename in os.listdir('./rot'):
    img = Image.open(f'./rot/{filename}')
    img = img.resize((270,480))
    img.save(f'./data/class_n/n{j}.jpg')
    print(j)
    j += 1
