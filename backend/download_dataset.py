"""
Eli Jerald C Rosales
2025-05-01
Downloads the dataset from Kaggle using the kagglehub library.
This script is used to download the dataset from Kaggle using the kagglehub library.

Citation:
BibTeX
@misc{make ml,
title={Car License Plates Dataset},
url={https://makeml.app/datasets/cars-license-plates},
journal={Make ML}}

License
Public domain

Splash banner
Photo by Gunnar Bjarki on Unsplash
"""

# kagglehub: a library that allows you to download datasets from Kaggle.
import kagglehub
# pip install kagglehub[pandas-datasets]

# Download latest version
path = kagglehub.dataset_download("andrewmvd/car-plate-detection")
# windows path example:
# C:\Users\<username>\.cache\kagglehub\datasets\andrewmvd\car-plate-detection\versions\1\images
# C:\Users\<username>eli\.cache\kagglehub\datasets\andrewmvd\car-plate-detection\versions\1\annotations

print("Path to dataset files:", path)