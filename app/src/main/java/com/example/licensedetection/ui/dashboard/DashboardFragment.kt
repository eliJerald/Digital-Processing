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
import com.chaquo.python.Python
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val py = Python.getInstance()
        val detect = py.getModule("detect")

        // Intialize all the Components in the View
        imageView = binding.imageView
        chooseimageButton = binding.buttonChooseImage

        changeimageButton = binding.buttonChangeImage
        detectlicenseButton = binding.buttonDetectLicense

        // This brings up the box to select an image and then stores that image file path to be used later
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageView!!.setImageURI(it)

                val inputStream = requireContext().contentResolver.openInputStream(it)
                val tempFile = File.createTempFile("input_image", ".jpg", requireContext().cacheDir)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output)}
                }

                currentImageURI = tempFile.absolutePath

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
            filterPlates.forEach { path ->
                runMLKitOCR(path)
            }

            val cannyPlates = jsonArray.getJSONArray(1).let { array ->
                List(array.length()) { i -> array.getString(i) }
            }
            cannyPlates.forEach { path ->
                runMLKitOCR(path)
            }

            val sobelPlates = jsonArray.getJSONArray(2).let { array ->
                List(array.length()) { i -> array.getString(i) }
            }
            sobelPlates.forEach { path ->
                runMLKitOCR(path)
            }
        }

        return root
    }

    // Run an OCR on the given path
    private fun runMLKitOCR(imagePath: String) {
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            Log.e("MLKitOCR", "Image file not found: $imagePath")
            return
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = TextRecognizerOptions.Builder().build()

        val recognizer: TextRecognizer = TextRecognition.getClient(options)

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

                // Optional: update UI with result
                // view?.findViewById<TextView>(R.id.textResult)?.text = resultText
            }
            .addOnFailureListener { e ->
                Log.e("MLKitOCR", "OCR failed for $imagePath", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}