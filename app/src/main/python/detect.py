"""
Eli Jerald C Rosales & Sarel Erasmus
2025-05-06

This script takes in an input picture and preforms some preprocessing actions to then send to an Android OCR for License Plate Detection.
These action include noise reduction, canny edge detection, and sobel edge detection.
"""

import cv2
import numpy as np
import matplotlib.pyplot as plt
import os
import json

def detect_license_plate(image_path: str) -> str:
    """
    - Noise reduction
    - no zoom
    """
    plates = []
    # Load image
    image = cv2.imread(image_path)

    # Convert to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Optional: Apply some preprocessing
    gray = cv2.bilateralFilter(gray, 11, 17, 17)  # Noise reduction

    output_path = "/sdcard/Download/filter_processed_" + os.path.basename(image_path)
    cv2.imwrite(output_path, gray)

    return output_path

def canny_edge_zoomed_sharpened_detection ( image_path , low_threshold =50 ,
    high_threshold =130) :
    """
    - Noise reduced
    - zoomed in picture
    """
    possible_plates_paths = []

    # Read image in grayscale
    image = cv2.imread(image_path)

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Apply Gaussian blur to reduce noise
    blurred = cv2.GaussianBlur(gray, (7,7), 0)

    # Use Canny edge detection
    edges = cv2.Canny(blurred, low_threshold, high_threshold)
    '''
    # SHOW FIGURE FOR TESTING PURPOSES
    plt.figure ( figsize =(8 , 4) )
    plt.subplot (1 , 2 , 1)
    plt.imshow ( edges , cmap ='gray')
    plt.show()
    '''
    # Find contours in the edge-detected image
    contours, _ = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)

    count = 1

    os.makedirs("/sdcard/Download/canny_processed/", exist_ok=True)

    # Loop through contours and filter by aspect ratio
    for contour in contours:
        # Approximate the contour to a polygon
        approx = cv2.approxPolyDP(contour, 0.05 * cv2.arcLength(contour, True), True)
        
        # Check if it has 4 corners  && doesn't have any dips
        if len(approx) == 4 and cv2.isContourConvex(approx):
            # Get bounding box
            x, y, w, h = cv2.boundingRect(approx)
            '''
            # SHOW FIGURE FOR TESTING PURPOSES
            license_plate = image[y:y+h, x:x+w]
            plt.figure ( figsize =(8 , 4) )
            plt.subplot (1 , 2 , 1)
            plt.imshow ( license_plate , cmap ='gray')
            plt.show()
            '''
            if w * h > 200:
                # Draw rectangle for visualization
                license_plate = image[y:y+h, x:x+w]
                # Sharpen the section of the image
                """
                # SHOW BEFORE FIGURE FOR TESTING PURPOSES
                plt.figure ( figsize =(8 , 4) )
                plt.subplot (1 , 2 , 1)
                plt.imshow ( license_plate , cmap ='gray')
                """
                kernel = np.array([[-1, -1, -1],
                                   [-1,  9, -1],
                                   [-1, -1, -1]])
                sharpened_image = cv2.filter2D(license_plate, -1, kernel)
                """
                # SHOW AFTER FIGURE FOR TESTING PURPOSES
                plt.figure ( figsize =(8 , 4) )
                plt.subplot (1 , 2 , 1)
                plt.imshow ( sharpened_image , cmap ='gray')
                plt.show()
                """

                output_path = f"/sdcard/Download/canny_processed/{count}_{os.path.basename(image_path)}"
                cv2.imwrite(output_path, sharpened_image)
                count += 1

                possible_plates_paths.append(output_path)

    return possible_plates_paths

def sobel_edge_detection ( image_path ) :
    """"""
    plates = []
    image = cv2.imread ( image_path , cv2.IMREAD_GRAYSCALE )
    if image is None :
        raise FileNotFoundError (" Image_not_found !")

    # Apply Sobel operator
    sobel_x = cv2.Sobel ( image , cv2.CV_64F , 1 , 0 , ksize =3)
    # Horizontal edges

    sobel_y = cv2.Sobel ( image , cv2.CV_64F , 0 , 1 , ksize =3) 
    # Vertical edges

    magnitude = np.sqrt ( sobel_x **2 + sobel_y **2) 
    # Gradient magnitude

    magnitude = cv2.convertScaleAbs ( magnitude )

    output_path = "/sdcard/Download/sobel_processed_" + os.path.basename(image_path)
    cv2.imwrite(output_path, magnitude)

    return output_path

# Each function returns paths of the preprocessed images which will be passed to ML Kit in DashboardFragment
def main(image_path):

    filter_path = detect_license_plate(image_path)

    canny_plates_paths = canny_edge_zoomed_sharpened_detection(image_path)

    sobel_path = sobel_edge_detection(image_path)

    def to_list(val):
        if isinstance(val, list):
            return val
        elif isinstance(val, str):
            return [val]
        else:
            return [str(val)]

    result = [
        to_list(filter_path),
        to_list(canny_plates_paths),
        to_list(sobel_path)
    ]

    return result

if __name__ == "__main__":
    main()
