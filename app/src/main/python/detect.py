"""
Eli Jerald C Rosales
2025-05-02

This script takes in an input picture and extracts the License Plate 
number.
"""

import cv2
import easyocr
import numpy as np
import matplotlib.pyplot as plt

def detect_license_plate(image_path: str) -> str:
    """
    - Noise reduction
    - no zoom
    - whole picture -> easyocr
    """
    plates = []
    # Load image
    image = cv2.imread(image_path)

    # Convert to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Optional: Apply some preprocessing
    gray = cv2.bilateralFilter(gray, 11, 17, 17)  # Noise reduction

    # Use EasyOCR
    reader = easyocr.Reader(['en'])
    results = reader.readtext(gray)

    # Find the result that looks most like a license plate
    for (bbox, text, prob) in results:
        if len(text) >= 3:  # Adjust these thresholds
            plates.append(text)
    
    # Return plates array
    if len(plates) != 0:
        return plates
    else:
        return "No License Plate Detected"

def canny_edge_zoomed_sharpened_detection ( image_path , low_threshold =50 ,
    high_threshold =130) :
    """
    - Noise reduced
    - zoomed in picture
    - Relevant area only -> easyocr
    """
    plates = []

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
                # Use EasyOCR to detect characters
                reader = easyocr.Reader(['en'])
                results = reader.readtext(sharpened_image)

                # Find the result that looks most like a license plate
                for (bbox, text, prob) in results:
                    if len(text) >= 3:  # Adjust these thresholds
                        plates.append(text)
    if len(plates) != 0:
        return plates
    else:
        return "No License Plate Detected"

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

    # Visualize results for testing purposes
    '''
    plt.figure ( figsize =(8 , 4) )
    plt.subplot (1 , 2 , 1) , plt.imshow ( image , cmap ='gray') , plt.title ('Original')
    plt.subplot (1 , 2 , 2) , plt.imshow ( magnitude , cmap ='gray') , plt.title ('Sobel_Edges')
    plt.tight_layout ()
    plt.show()
    #plt.savefig ('sobel_output.png')
    plt.close ()
    '''

    # Use EasyOCR
    reader = easyocr.Reader(['en'])
    results = reader.readtext(magnitude)

    # Find the result that looks most like a license plate
    for (bbox, text, prob) in results:
        if len(text) >= 5 and prob > 0.4:  # Adjust these thresholds
            plates.append(text)

    # Return plates array or nothing
    if len(plates) != 0:
        return plates
    else:
        return "No License Plate Detected: Sobel_edge"

def main():
    # Example usage
    image_path = "C:\\Users\\ejera\\.cache\\kagglehub\\datasets\\andrewmvd\\car-plate-detection\\versions\\1\\images\\Cars40.png"

    plate_text = detect_license_plate(image_path)
    print("Detected License Plate:", plate_text)

    edges = canny_edge_zoomed_sharpened_detection(image_path)
    print("Detected License Plate - Canny edge + Zoom:", edges)

    mags = sobel_edge_detection(image_path)
    print("Detected License Plate - Sobel edge:", mags)

if __name__ == "__main__":
    main()
