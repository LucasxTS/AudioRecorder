package com.example.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import java.time.format.DateTimeFormatter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audiorecorder.Adapters.AudioListAdapter
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.example.audiorecorder.models.Audio
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalTime

class MainActivity : AppCompatActivity() {
    var i = 1
    private val record_audio_permission_request_code = 1
    private lateinit var binding: ActivityMainBinding
    private var mediaRecorder: MediaRecorder? = null
    private var audioList = mutableListOf<Audio>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AudioListAdapter
    private var currentMillis: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadData()

        recyclerView = binding.recyclerView
        adapter = AudioListAdapter(this, audioList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        binding.recorderButton.setOnClickListener {
            checkingMicrophonePermission()
        }
    }

    private fun loadData() {
        val sharedpreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedListJson = sharedpreferences.getString("Audio_List", null)
        val gson = Gson()
        val listType = object : TypeToken<MutableList<Audio>>() {}.type

        if (savedListJson != null) {
            audioList = gson.fromJson(savedListJson, listType)
        }


    }

    private fun saveData(string: String) {
        val sharedpreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedpreferences.edit()
        editor.apply {
            putString("Audio_List", string)
        }.apply()
    }

    private fun checkingMicrophonePermission() {
        if (mediaRecorder == null) {
            requestMicrophoneAndLocationPermission()
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
        binding.recorderButton.background =
            ContextCompat.getDrawable(this@MainActivity, R.drawable.shape_square)
    }

    private fun stopRecording() {
        val hour = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val formatedHour = if (hour.isBefore(LocalTime.NOON)) {
            hour.format(formatter) + " AM"
        } else {
            hour.format(formatter) + " PM"
        }
        val gson = Gson()

        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }

        mediaRecorder = null
        binding.recorderButton.text = "Rec"
        binding.recorderButton.background =
            ContextCompat.getDrawable(this@MainActivity, R.drawable.shape_circle)
        val audioFilePath = currentMillis

        if (File(audioFilePath).exists()) {
            val audio = Audio("Audio ${i}", audioFilePath, formatedHour)
            audioList.add(audio)
            i++
            adapter.notifyDataSetChanged()
            val json = gson.toJson(audioList)
            saveData(json)

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

    private fun requestMicrophoneAndLocationPermission() {
        val audioPermission = Manifest.permission.RECORD_AUDIO
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

        val hasMicrophonePermission = ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED
        val hasLocationPermission = ContextCompat.checkSelfPermission(this, locationPermission) == PackageManager.PERMISSION_GRANTED

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, audioPermission)) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs access to your microphone\n")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(audioPermission),
                        record_audio_permission_request_code
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(audioPermission),
                record_audio_permission_request_code
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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