package com.example.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audiorecorder.Adapters.AudioListAdapter
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.models.Audio
import java.io.File

class MainActivity : AppCompatActivity() {
    var i = 1
    private val record_audio_permission_request_code = 1
    private lateinit var binding : ActivityMainBinding
    private var mediaRecorder: MediaRecorder? = null
    private var audioList = mutableListOf<Audio>()
    private lateinit var recyclerView : RecyclerView
    private lateinit var adapter : AudioListAdapter
    private var currentMillis : String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.recyclerView
        adapter = AudioListAdapter(this, audioList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        binding.recorderButton.setOnClickListener {
            checkingMicrophonePermission()
        }
    }

    private fun checkingMicrophonePermission() {
        if(mediaRecorder == null) {
            requestMicrophonePermission()
        } else {
            stopRecording()

        }
    }

    private fun startRecording() {
        val path = getFilePath()
        currentMillis = path
        mediaRecorder = MediaRecorder()
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(path)
            prepare()
            start()
        }
        binding.recorderButton.text = "Stop"
        binding.recorderButton.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.shape_square)
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }

        mediaRecorder = null
        binding.recorderButton.text = "Rec"
        binding.recorderButton.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.shape_circle)


        val audioFilePath = currentMillis

        val createdFile = File(audioFilePath)
        println("File exists: ${createdFile.exists()}")
        if (File(audioFilePath).exists()) {
            val audio = Audio("Audio ${i}", audioFilePath)
            audioList.add(audio)
            i++
            adapter.notifyDataSetChanged()
        } else {
            println("Doesnt found archive: $audioFilePath")
        }

    }

    private fun getFilePath(): String {
        val directory = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        println("Directory path: $directory")

        if (directory != null) {
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val fileName = "audio_${System.currentTimeMillis()}.3gp"
            return File(directory, fileName).absolutePath
        } else {
            throw RuntimeException("External files directory is null. Cannot create audio file.")
        }
    }

    private fun requestMicrophonePermission() {
        val permission = Manifest.permission.RECORD_AUDIO


        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
           AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs access to your microphone\n")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(permission), record_audio_permission_request_code)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), record_audio_permission_request_code)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == record_audio_permission_request_code) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                return
            }
        }
    }
}