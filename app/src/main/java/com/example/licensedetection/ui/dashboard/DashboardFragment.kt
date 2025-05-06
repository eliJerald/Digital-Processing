package com.example.licensedetection.ui.dashboard

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.chaquo.python.Python
import com.example.licensedetection.R
import com.example.licensedetection.databinding.FragmentDashboardBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import java.io.File

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var imageView: ImageView? = null
    private var chooseimageButton: Button? = null

    private var changeimageButton: Button? = null
    private var detectlicenseButton: Button? = null

    private var imagePickerLauncher: ActivityResultLauncher<String>? = null

    private var currentImageURI: String? = null

    // Holds The Results of Each Function
    private val filterResults = mutableListOf<String>()
    private val cannyResults = mutableListOf<String>()
    private val sobelResults = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val py = Python.getInstance()
        val detect = py.getModule("detect")

        // Initialize all the Components in the View
        imageView = binding.imageView
        chooseimageButton = binding.buttonChooseImage

        changeimageButton = binding.buttonChangeImage
        detectlicenseButton = binding.buttonDetectLicense

        // This brings up the box to select an image and then stores that image file path to be used later
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageView!!.setImageURI(it)

                // Create a Temporary File so that the python script has a file path to run
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val tempFile = File.createTempFile("input_image", ".jpg", requireContext().cacheDir)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output)}
                }

                currentImageURI = tempFile.absolutePath

                // Change which buttons are visible to allow user to run the script
                chooseimageButton!!.visibility = View.INVISIBLE

                changeimageButton!!.visibility = View.VISIBLE
                detectlicenseButton!!.visibility = View.VISIBLE

            }
        }

        // Create Listeners for all of the buttons
        chooseimageButton!!.setOnClickListener {
            imagePickerLauncher!!.launch("image/*")
        }

        changeimageButton!!.setOnClickListener {
            imagePickerLauncher!!.launch("image/*")
        }

        detectlicenseButton!!.setOnClickListener {

            // Split the resulting paths from Python Script into different arrays to be ran through ML Kit
            val result = detect.callAttr("main",currentImageURI).toString()
            val jsonArray = JSONArray(result)

            val filterPlates = jsonArray.getJSONArray(0).let { array ->
                List(array.length()) { i -> array.getString(i) }
            }

            val cannyPlates = jsonArray.getJSONArray(1).let { array ->
                List(array.length()) { i -> array.getString(i) }
            }

            val sobelPlates = jsonArray.getJSONArray(2).let { array ->
                List(array.length()) { i -> array.getString(i) }
            }

            var completed = 0
            val total = filterPlates.size + cannyPlates.size + sobelPlates.size

            // Clear previous OCR results
            filterResults.clear()
            cannyResults.clear()
            sobelResults.clear()

            // Function to process each set and then navigate when all are done
            val processSet: (List<String>, String) -> Unit = { paths, key ->
                paths.forEach { path ->
                    runMLKitOCR(path,key) {

                        completed += 1
                        if (completed == total) {
                            // When all sets are processed, navigate

                            // Have a default message if any of the functions don't find a license plate
                            if(filterResults.size == 0)
                                filterResults.add(0,"Detected No License Plate")

                            if(cannyResults.size == 0)
                                cannyResults.add(0,"Detected No License Plate")

                            if(sobelResults.size == 0)
                                sobelResults.add(0,"Detected No License Plate")

                            // Bundle for all plates
                            val bundle = Bundle().apply {
                                putString("EXTRA_RECOGNIZED_TEXT_FILTER", filterResults.distinct().joinToString("\n"))
                                putString("EXTRA_RECOGNIZED_TEXT_CANNY", cannyResults.distinct().joinToString("\n"))
                                putString("EXTRA_RECOGNIZED_TEXT_SOBEL", sobelResults.distinct().joinToString("\n"))
                            }

                            // Navigate to the notification fragment to display results
                            binding.root.findNavController().navigate(
                                R.id.action_dashboardFragment_to_notificationFragment,
                                bundle
                            )

                        }
                    }
                }
            }

            // Process all image paths for each function
            processSet(filterPlates, "filterPlates")
            processSet(cannyPlates, "cannyPlates")
            processSet(sobelPlates, "sobelPlates")
        }


        return root
    }

    // Run an OCR on the given path
    private fun runMLKitOCR(imagePath: String, key: String, onComplete: () -> Unit) {
        // Open the preprocessed image for OCR processing
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            Log.e("MLKitOCR", "Image file not found: $imagePath")
            onComplete()
            return
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = TextRecognizerOptions.Builder().build()

        val recognizer: TextRecognizer = TextRecognition.getClient(options)

        // Run OCR on the image
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // List of State Names to be ignored on license plate detection
                val stateNames = setOf(
                    "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
                    "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho",
                    "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
                    "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota",
                    "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
                    "New Hampshire", "New Jersey", "New Mexico", "New York",
                    "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
                    "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
                    "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington",
                    "West Virginia", "Wisconsin", "Wyoming"
                ).map { it.uppercase().replace("\\s".toRegex(), "") }

                // Will filter text to be greater than 5 characters and does not contain any state names
                val filteredText = visionText.textBlocks
                    .flatMap { it.lines }
                    .map { it.text.replace("\\s+".toRegex(), "").uppercase() }
                    .filter { text ->
                        text.length >= 5 && !stateNames.any{state -> text.contains(state)}
                    }

                Log.d("FilteredOCR", "Filtered results: $filteredText")

                // Save the results to an object dependant on which function was being tested
                if(key == "filterPlates") {
                    filterResults.addAll(filteredText)
                }else if(key == "cannyPlates") {
                    cannyResults.addAll(filteredText)
                }
                else {
                    sobelResults.addAll(filteredText)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MLKitOCR", "OCR failed for $imagePath", e)
            }
            .addOnCompleteListener {
                onComplete()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}