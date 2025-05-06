package com.example.licensedetection.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.licensedetection.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Grab the text that was generated in the detect Fragment
        val filterText = arguments?.getString("EXTRA_RECOGNIZED_TEXT_FILTER") ?: "Found No License Plates"
        val cannyText = arguments?.getString("EXTRA_RECOGNIZED_TEXT_CANNY") ?: "Found No License Plates"
        val sobelText = arguments?.getString("EXTRA_RECOGNIZED_TEXT_SOBEL") ?: "Found No License Plates"

        // Display them in TextViews
        binding.noisePlateText.text = filterText
        binding.cannyPlateText.text = cannyText
        binding.sobelPlateText.text = sobelText

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}